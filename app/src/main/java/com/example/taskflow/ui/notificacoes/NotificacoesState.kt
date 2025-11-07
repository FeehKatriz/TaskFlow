package com.example.taskflow.ui.notificacoes

import com.example.taskflow.data.model.Notificacao

sealed class NotificacoesState {
    object Loading : NotificacoesState()
    data class Success(
        val notificacoesNaoLidas: List<Notificacao>,
        val notificacoesLidas: List<Notificacao>,
        val totalNaoLidas: Int
    ) : NotificacoesState()
    object Empty : NotificacoesState()
    data class Error(val message: String) : NotificacoesState()
}

/**
 * ATUALIZADO: Novos eventos para navegação via NotificationHostActivity
 */
sealed class NotificacoesEvent {
    data class ShowMessage(val message: String) : NotificacoesEvent()

    // Navegação simples para tarefa
    data class NavigateToTarefa(val tarefaId: String) : NotificacoesEvent()

    // Navegação para tarefa com foco em comentário
    data class NavigateToTarefaWithComentario(
        val tarefaId: String,
        val comentarioId: String?,
        val scrollToComentario: Boolean = true
    ) : NotificacoesEvent()

    // Navegação para equipe
    data class NavigateToEquipe(val equipeId: String) : NotificacoesEvent()

    // Navegação para projeto
    data class NavigateToProjeto(val projetoId: String) : NotificacoesEvent()
}