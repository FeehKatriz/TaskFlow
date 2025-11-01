package com.example.taskflow.ui.cadastrar

sealed class CadastrarState {
    object Idle : CadastrarState()
    object Loading : CadastrarState()
    object Success : CadastrarState()
    data class EmailVerificationSent(val email: String) : CadastrarState()
    data class EmailResent(val message: String) : CadastrarState()
    data class Error(val message: String) : CadastrarState()
}