package com.example.taskflow.data.repository

import android.util.Log
import com.example.taskflow.data.model.Equipe
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EquipeRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Buscar o primeiro projeto do usuário
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

    // ==================== NOVO: CARREGAR USUÁRIOS DO PROJETO ====================
    /**
     * Carrega os usuários disponíveis no projeto para adicionar à equipe
     */
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

                    // Buscar informações dos membros
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

    // ==================== NOVO: CRIAR EQUIPE COM MEMBROS ====================
    /**
     * Cria uma equipe e adiciona os membros selecionados
     */
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

        // Criar mapa de dados da equipe
        val equipeRef = firestore.collection("equipes").document()
        val equipeId = equipeRef.id

        val equipeData = hashMapOf(
            "id" to equipeId,
            "nome" to equipe.nome,
            "descricao" to equipe.descricao,
            "dataVencimento" to equipe.dataVencimento,
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

    // ==================== MÉTODO ORIGINAL (mantido para compatibilidade) ====================
    // Criar uma nova equipe
    fun criarEquipe(equipe: Equipe, callback: (Result<Unit>) -> Unit) {
        firestore.collection("equipes")
            .add(equipe)
            .addOnSuccessListener { documentReference ->
                documentReference.update("id", documentReference.id)
                    .addOnSuccessListener {
                        callback(Result.success(Unit))
                    }
                    .addOnFailureListener { e ->
                        // Equipe criada mesmo com erro ao atualizar ID
                        callback(Result.success(Unit))
                    }
            }
            .addOnFailureListener { e ->
                callback(Result.failure(e))
            }
    }
}