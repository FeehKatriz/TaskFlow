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


sealed class NotificacoesEvent {
    data class ShowMessage(val message: String) : NotificacoesEvent()

    data class NavigateToTarefa(val tarefaId: String) : NotificacoesEvent()

    data class NavigateToTarefaWithComentario(
        val tarefaId: String,
        val comentarioId: String?,
        val scrollToComentario: Boolean = true
    ) : NotificacoesEvent()

    data class NavigateToEquipe(val equipeId: String) : NotificacoesEvent()

    data class NavigateToProjeto(val projetoId: String) : NotificacoesEvent()
}