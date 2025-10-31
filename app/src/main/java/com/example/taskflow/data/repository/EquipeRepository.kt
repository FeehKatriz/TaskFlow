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

    // ==================== NOVAS FUNÇÕES PARA EDIÇÃO E EXCLUSÃO ====================

    /**
     * Atualiza o nome de uma equipe
     */
    fun atualizarNomeEquipe(
        equipeId: String,
        novoNome: String,
        callback: (Result<Unit>) -> Unit
    ) {
        firestore.collection("equipes")
            .document(equipeId)
            .update("nome", novoNome)
            .addOnSuccessListener {
                Log.d("EquipeRepository", "Nome da equipe atualizado com sucesso")
                callback(Result.success(Unit))
            }
            .addOnFailureListener { e ->
                Log.e("EquipeRepository", "Erro ao atualizar nome da equipe", e)
                callback(Result.failure(e))
            }
    }

    /**
     * Exclui uma equipe e todas as suas tarefas em cascata
     */
    fun excluirEquipeComTarefas(
        equipeId: String,
        callback: (Result<Unit>) -> Unit
    ) {
        // Primeiro, buscar todas as tarefas da equipe
        firestore.collection("tarefas")
            .whereEqualTo("equipeId", equipeId)
            .get()
            .addOnSuccessListener { tarefasSnapshot ->
                // Se não houver tarefas, apenas excluir a equipe
                if (tarefasSnapshot.isEmpty) {
                    excluirEquipe(equipeId, callback)
                    return@addOnSuccessListener
                }

                // Usar batch para excluir todas as tarefas de uma vez
                val batch = firestore.batch()

                // Adicionar exclusão de todas as tarefas ao batch
                tarefasSnapshot.documents.forEach { tarefaDoc ->
                    batch.delete(tarefaDoc.reference)
                }

                // Adicionar exclusão da equipe ao batch
                val equipeRef = firestore.collection("equipes").document(equipeId)
                batch.delete(equipeRef)

                // Executar todas as exclusões de uma vez
                batch.commit()
                    .addOnSuccessListener {
                        Log.d("EquipeRepository", "Equipe e ${tarefasSnapshot.size()} tarefas excluídas com sucesso")
                        callback(Result.success(Unit))
                    }
                    .addOnFailureListener { e ->
                        Log.e("EquipeRepository", "Erro ao excluir equipe e tarefas", e)
                        callback(Result.failure(Exception("Erro ao excluir equipe e tarefas: ${e.message}")))
                    }
            }
            .addOnFailureListener { e ->
                Log.e("EquipeRepository", "Erro ao buscar tarefas da equipe", e)
                callback(Result.failure(Exception("Erro ao buscar tarefas da equipe: ${e.message}")))
            }
    }

    /**
     * Exclui apenas a equipe (usado quando não há tarefas)
     */
    private fun excluirEquipe(
        equipeId: String,
        callback: (Result<Unit>) -> Unit
    ) {
        firestore.collection("equipes")
            .document(equipeId)
            .delete()
            .addOnSuccessListener {
                Log.d("EquipeRepository", "Equipe excluída com sucesso")
                callback(Result.success(Unit))
            }
            .addOnFailureListener { e ->
                Log.e("EquipeRepository", "Erro ao excluir equipe", e)
                callback(Result.failure(e))
            }
    }
}