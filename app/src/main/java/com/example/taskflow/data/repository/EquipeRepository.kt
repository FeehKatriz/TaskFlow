package com.example.taskflow.data.repository

import android.util.Log
import com.example.taskflow.data.model.Equipe
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EquipeRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun carregarPrimeiroProjeto(callback: (Result<String>) -> Unit) {
        val userId = auth.currentUser?.uid ?: run {
            callback(Result.failure(Exception("Usuário não autenticado")))
            return
        }

        firestore.collection("projetos")
            .whereArrayContains("membros", userId)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    val projetoId = snapshot.documents[0].id
                    callback(Result.success(projetoId))
                } else {
                    callback(Result.failure(Exception("Você precisa fazer parte de um projeto primeiro")))
                }
            }
            .addOnFailureListener { e ->
                callback(Result.failure(e))
            }
    }

    fun carregarUsuariosDisponiveis(
        projetoId: String,
        callback: (Result<List<Pair<String, String>>>) -> Unit
    ) {
        firestore.collection("projetos")
            .document(projetoId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val membrosIds = document.get("membros") as? List<String> ?: emptyList()

                    if (membrosIds.isEmpty()) {
                        callback(Result.success(emptyList()))
                        return@addOnSuccessListener
                    }

                    val usuarios = mutableListOf<Pair<String, String>>()
                    var processados = 0

                    membrosIds.forEach { memberId ->
                        firestore.collection("usuarios")
                            .document(memberId)
                            .get()
                            .addOnSuccessListener { userDoc ->
                                if (userDoc.exists()) {
                                    val nome = userDoc.getString("nome") ?: "Sem nome"
                                    usuarios.add(Pair(memberId, nome))
                                }
                                processados++

                                if (processados == membrosIds.size) {
                                    callback(Result.success(usuarios))
                                }
                            }
                            .addOnFailureListener { e ->
                                processados++
                                if (processados == membrosIds.size) {
                                    callback(Result.success(usuarios))
                                }
                            }
                    }
                } else {
                    callback(Result.failure(Exception("Projeto não encontrado")))
                }
            }
            .addOnFailureListener { e ->
                Log.e("EquipeRepository", "Erro ao carregar usuários do projeto", e)
                callback(Result.failure(e))
            }
    }

    fun criarEquipeComMembros(
        equipe: Equipe,
        membros: List<String>,
        callback: (Result<Unit>) -> Unit
    ) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            callback(Result.failure(Exception("Usuário não autenticado")))
            return
        }

        // Adicionar o criador aos membros se não estiver na lista
        val todosMembros = if (!membros.contains(userId)) {
            membros + userId
        } else {
            membros
        }

        val equipeRef = firestore.collection("equipes").document()
        val equipeId = equipeRef.id

        // ✅ ATUALIZADO: Removidos campos descricao e dataVencimento
        val equipeData = hashMapOf(
            "id" to equipeId,
            "nome" to equipe.nome,
            "progresso" to equipe.progresso,
            "totalTarefas" to equipe.totalTarefas,
            "projetoId" to equipe.projetoId,
            "membros" to todosMembros,
            "criadoPor" to userId,
            "criadoEm" to com.google.firebase.Timestamp.now()
        )

        equipeRef.set(equipeData)
            .addOnSuccessListener {
                Log.d("EquipeRepository", "Equipe criada com sucesso: $equipeId com ${todosMembros.size} membros")
                callback(Result.success(Unit))
            }
            .addOnFailureListener { e ->
                Log.e("EquipeRepository", "Erro ao criar equipe", e)
                callback(Result.failure(e))
            }
    }

    // Método original mantido para compatibilidade
    fun criarEquipe(equipe: Equipe, callback: (Result<Unit>) -> Unit) {
        firestore.collection("equipes")
            .add(equipe)
            .addOnSuccessListener { documentReference ->
                documentReference.update("id", documentReference.id)
                    .addOnSuccessListener {
                        callback(Result.success(Unit))
                    }
                    .addOnFailureListener { e ->
                        callback(Result.success(Unit))
                    }
            }
            .addOnFailureListener { e ->
                callback(Result.failure(e))
            }
    }
}