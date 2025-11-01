package com.example.taskflow.ui.entrar

sealed class EntrarState {
    object Idle : EntrarState()
    object Loading : EntrarState()
    object Success : EntrarState()
    data class EmailNotVerified(val email: String) : EntrarState()
    data class EmailResent(val message: String) : EntrarState()
    data class Error(val message: String) : EntrarState()
}