package com.example.taskflow.ui.notificacoes

import androidx.fragment.app.Fragment

/**
 * Define os destinos possíveis ao abrir uma notificação
 */
sealed class NotificationDestination {
    data class Tarefa(
        val tarefaId: String,
        val comentarioId: String? = null,
        val scrollToComentario: Boolean = false
    ) : NotificationDestination()

    data class Equipe(
        val equipeId: String
    ) : NotificationDestination()

    data class Projeto(
        val projetoId: String
    ) : NotificationDestination()

}

/**
 * Estados da NotificationHostActivity
 */
sealed class NotificationHostState {
    object Loading : NotificationHostState()

    data class Success(
        val destination: NotificationDestination,
        val fragment: Fragment
    ) : NotificationHostState()

    data class Error(val message: String) : NotificationHostState()
}

/**
 * Eventos one-time da NotificationHostActivity
 */
sealed class NotificationHostEvent {
    data class ShowMessage(val message: String) : NotificationHostEvent()
    object NavigateBack : NotificationHostEvent()
}

/**
 * Companion object com constantes para Intent extras
 */
object NotificationHostExtras {
    const val DESTINATION_TYPE = "destination_type"
    const val TAREFA_ID = "tarefa_id"
    const val COMENTARIO_ID = "comentario_id"
    const val SCROLL_TO_COMENTARIO = "scroll_to_comentario"
    const val EQUIPE_ID = "equipe_id"
    const val PROJETO_ID = "projeto_id"

    // Tipos de destino
    const val TYPE_TAREFA = "TAREFA"
    const val TYPE_EQUIPE = "EQUIPE"
    const val TYPE_PROJETO = "PROJETO"
}