package com.example.taskflow.ui.projeto.criar

sealed class CriarProjetoState {
    object Idle : CriarProjetoState()
    object Loading : CriarProjetoState()
    object Success : CriarProjetoState()
    data class Error(val message: String) : CriarProjetoState()
}