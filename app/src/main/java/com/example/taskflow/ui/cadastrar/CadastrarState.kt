package com.example.taskflow.ui.cadastrar

sealed class CadastrarState{
    object Idle : CadastrarState()
    object Loading : CadastrarState()
    object Success : CadastrarState()
    data class Error(val message: String) : CadastrarState()

}