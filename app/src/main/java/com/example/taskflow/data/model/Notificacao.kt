package com.example.taskflow.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Notificacao(
    @DocumentId
    val id: String = "",
    val tipo: String = "",
    val titulo: String = "",
    val mensagem: String = "",
    val userId: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val lida: Boolean = false,
    val visualizada: Boolean = false,

    // Dados relacionados
    val tarefaId: String? = null,
    val equipeId: String? = null,
    val comentarioId: String? = null,
    val remetenteId: String? = null,
    val remetenteNome: String? = null,
    val novoStatus: String? = null,
    val enviadoPush: Boolean = false
) {
    companion object {
        // Tipos vindos das Cloud Functions
        const val TIPO_TAREFA_ATUALIZADA = "TAREFA_ATUALIZADA"
        const val TIPO_TAREFA_CRIADA = "TAREFA_CRIADA"
        const val TIPO_NOVO_COMENTARIO = "NOVO_COMENTARIO"
    }
}