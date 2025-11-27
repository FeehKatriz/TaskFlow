package com.example.taskflow.data.repository

import android.util.Log
import com.example.taskflow.data.model.Equipe
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EquipeRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val comentarioRepository = ComentarioRepository()
    private val arquivoRepository = ArquivoRepository()

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
        val currentUserId = auth.currentUser?.uid

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
                                    // Se for o usuário atual, mostrar "Você"
                                    val nome = if (memberId == currentUserId) {
                                        "Você"
                                    } else {
                                        userDoc.getString("nome") ?: "Sem nome"
                                    }
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

        // Validar que há pelo menos um membro selecionado
        if (membros.isEmpty()) {
            callback(Result.failure(Exception("É necessário selecionar pelo menos um membro")))
            return
        }

        // Usar os membros selecionados
        val equipeRef = firestore.collection("equipes").document()
        val equipeId = equipeRef.id

        val equipeData = hashMapOf(
            "id" to equipeId,
            "nome" to equipe.nome,
            "progresso" to equipe.progresso,
            "totalTarefas" to equipe.totalTarefas,
            "projetoId" to equipe.projetoId,
            "membros" to membros, // Apenas os membros selecionados
            "criadoPor" to userId,
            "criadoEm" to com.google.firebase.Timestamp.now()
        )

        equipeRef.set(equipeData)
            .addOnSuccessListener {
                Log.d("EquipeRepository", "Equipe criada com sucesso: $equipeId com ${membros.size} membros")
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

    // ==================== FUNÇÕES PARA EDIÇÃO E EXCLUSÃO ====================

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
     * Também deleta comentários e arquivos de cada tarefa
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

                val tarefaIds = tarefasSnapshot.documents.map { it.id }
                var processadas = 0
                val totalTarefas = tarefaIds.size

                // Para cada tarefa, deletar comentários e arquivos primeiro
                tarefaIds.forEach { tarefaId ->
                    // Deletar comentários da tarefa
                    comentarioRepository.deletarTodosComentarios(tarefaId) { resultComentarios ->
                        resultComentarios.onFailure { e ->
                            Log.w("EquipeRepository", "Aviso: Erro ao deletar comentários da tarefa $tarefaId", e)
                        }

                        // Deletar arquivos da tarefa
                        arquivoRepository.deletarTodosArquivosDaTarefa(tarefaId) { resultArquivos ->
                            resultArquivos.onFailure { e ->
                                Log.w("EquipeRepository", "Aviso: Erro ao deletar arquivos da tarefa $tarefaId", e)
                            }

                            processadas++

                            // Quando todas as tarefas tiverem seus dados limpos, deletar tudo
                            if (processadas == totalTarefas) {
                                // Usar batch para excluir todas as tarefas + equipe
                                val batch = firestore.batch()

                                tarefasSnapshot.documents.forEach { tarefaDoc ->
                                    batch.delete(tarefaDoc.reference)
                                }

                                val equipeRef = firestore.collection("equipes").document(equipeId)
                                batch.delete(equipeRef)

                                batch.commit()
                                    .addOnSuccessListener {
                                        Log.d("EquipeRepository", "✅ Equipe e ${tarefasSnapshot.size()} tarefas excluídas com sucesso (incluindo comentários e arquivos)")
                                        callback(Result.success(Unit))
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("EquipeRepository", "❌ Erro ao excluir equipe e tarefas", e)
                                        callback(Result.failure(Exception("Erro ao excluir equipe e tarefas: ${e.message}")))
                                    }
                            }
                        }
                    }
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