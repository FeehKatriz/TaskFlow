package com.example.taskflow.data.repository

import android.util.Log
import com.example.taskflow.data.model.Comment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ComentarioRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /**
     * Carrega comentários de uma tarefa em tempo real
     */
    fun carregarComentarios(
        tarefaId: String,
        callback: (Result<List<Comment>>) -> Unit
    ) {
        firestore.collection("tarefas")
            .document(tarefaId)
            .collection("comentarios")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ComentarioRepository", "Erro ao carregar comentários", error)
                    callback(Result.failure(error))
                    return@addSnapshotListener
                }

                val comentarios = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val comment = doc.toObject(Comment::class.java)
                        comment?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e("ComentarioRepository", "Erro ao converter comentário", e)
                        null
                    }
                } ?: emptyList()

                Log.d("ComentarioRepository", "Comentários carregados: ${comentarios.size}")
                callback(Result.success(comentarios))
            }
    }

    /**
     * Adiciona um novo comentário à tarefa
     * ✅ Salva apenas userId e mensagem - dados do usuário são buscados dinamicamente
     */
    fun adicionarComentario(
        tarefaId: String,
        mensagem: String,
        callback: (Result<Unit>) -> Unit
    ) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            callback(Result.failure(Exception("Usuário não autenticado")))
            return
        }

        val comment = hashMapOf(
            "userId" to userId,
            "message" to mensagem.trim(),
            "timestamp" to System.currentTimeMillis()
        )

        firestore.collection("tarefas")
            .document(tarefaId)
            .collection("comentarios")
            .add(comment)
            .addOnSuccessListener { documentReference ->
                Log.d("ComentarioRepository", "✅ Comentário adicionado - ID: ${documentReference.id}, Tarefa: $tarefaId, Autor: $userId")
                callback(Result.success(Unit))
            }
            .addOnFailureListener { e ->
                Log.e("ComentarioRepository", "❌ Erro ao adicionar comentário", e)
                callback(Result.failure(e))
            }
    }

    /**
     * Deleta um comentário (apenas o autor pode deletar)
     */
    fun deletarComentario(
        tarefaId: String,
        commentId: String,
        callback: (Result<Unit>) -> Unit
    ) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            callback(Result.failure(Exception("Usuário não autenticado")))
            return
        }

        // Verificar se o usuário é o autor do comentário
        firestore.collection("tarefas")
            .document(tarefaId)
            .collection("comentarios")
            .document(commentId)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    callback(Result.failure(Exception("Comentário não encontrado")))
                    return@addOnSuccessListener
                }

                val autorId = document.getString("userId")
                if (autorId != userId) {
                    callback(Result.failure(Exception("Você não tem permissão para deletar este comentário")))
                    return@addOnSuccessListener
                }

                // Deletar o comentário
                document.reference.delete()
                    .addOnSuccessListener {
                        Log.d("ComentarioRepository", "✅ Comentário deletado - ID: $commentId, Tarefa: $tarefaId")
                        callback(Result.success(Unit))
                    }
                    .addOnFailureListener { e ->
                        Log.e("ComentarioRepository", "❌ Erro ao deletar comentário", e)
                        callback(Result.failure(e))
                    }
            }
            .addOnFailureListener { e ->
                Log.e("ComentarioRepository", "❌ Erro ao verificar autor do comentário", e)
                callback(Result.failure(e))
            }
    }

    /**
     * Edita um comentário existente (apenas o autor pode editar)
     */
    fun editarComentario(
        tarefaId: String,
        commentId: String,
        novaMensagem: String,
        callback: (Result<Unit>) -> Unit
    ) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            callback(Result.failure(Exception("Usuário não autenticado")))
            return
        }

        if (novaMensagem.trim().isEmpty()) {
            callback(Result.failure(Exception("Mensagem não pode estar vazia")))
            return
        }

        // Verificar se o usuário é o autor do comentário
        firestore.collection("tarefas")
            .document(tarefaId)
            .collection("comentarios")
            .document(commentId)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    callback(Result.failure(Exception("Comentário não encontrado")))
                    return@addOnSuccessListener
                }

                val autorId = document.getString("userId")
                if (autorId != userId) {
                    callback(Result.failure(Exception("Você não tem permissão para editar este comentário")))
                    return@addOnSuccessListener
                }

                // Atualizar o comentário
                val updates = hashMapOf<String, Any>(
                    "message" to novaMensagem.trim(),
                    "editadoEm" to System.currentTimeMillis()
                )

                document.reference.update(updates)
                    .addOnSuccessListener {
                        Log.d("ComentarioRepository", "✅ Comentário editado - ID: $commentId, Tarefa: $tarefaId")
                        callback(Result.success(Unit))
                    }
                    .addOnFailureListener { e ->
                        Log.e("ComentarioRepository", "❌ Erro ao editar comentário", e)
                        callback(Result.failure(e))
                    }
            }
            .addOnFailureListener { e ->
                Log.e("ComentarioRepository", "❌ Erro ao verificar autor do comentário", e)
                callback(Result.failure(e))
            }
    }

    /**
     * Conta o total de comentários de uma tarefa
     */
    fun contarComentarios(
        tarefaId: String,
        callback: (Result<Int>) -> Unit
    ) {
        firestore.collection("tarefas")
            .document(tarefaId)
            .collection("comentarios")
            .get()
            .addOnSuccessListener { snapshot ->
                val total = snapshot.size()
                Log.d("ComentarioRepository", "Total de comentários da tarefa $tarefaId: $total")
                callback(Result.success(total))
            }
            .addOnFailureListener { e ->
                Log.e("ComentarioRepository", "❌ Erro ao contar comentários", e)
                callback(Result.failure(e))
            }
    }
}