package com.example.taskflow.ui.equipe.criar

sealed class CriarEquipeState {
    object Idle : CriarEquipeState()
    object Loading : CriarEquipeState()
    object Success : CriarEquipeState()
    data class Error(val message: String) : CriarEquipeState()
}