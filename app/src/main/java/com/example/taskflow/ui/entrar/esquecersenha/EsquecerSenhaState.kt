package com.example.taskflow.ui.entrar.esquecersenha

sealed class EsquecerSenhaState {
    object Idle : EsquecerSenhaState()
    object Loading : EsquecerSenhaState()
    object Success : EsquecerSenhaState()
    data class Error(val message: String) : EsquecerSenhaState()
}