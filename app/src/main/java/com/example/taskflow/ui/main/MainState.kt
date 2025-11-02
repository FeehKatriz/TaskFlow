package com.example.taskflow.ui.main

sealed class MainState {
    object Loading : MainState()
    data class Success(
        val nomeUsuario: String,
        val fotoPerfilUrl: String?,
        val notificacoesNaoLidas: Int
    ) : MainState()
    data class Error(val message: String) : MainState()
}

sealed class MainEvent {
    data class ShowMessage(val message: String) : MainEvent()
    data class NavigateToTarefa(val tarefaId: String) : MainEvent()
    data class NavigateToEquipe(val equipeId: String) : MainEvent()
}