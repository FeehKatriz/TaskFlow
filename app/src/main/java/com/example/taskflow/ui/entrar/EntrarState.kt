package com.example.taskflow.ui.entrar

sealed class  EntrarState {
    object Idle : EntrarState()
    object Loading : EntrarState()
    object Success : EntrarState()
    data class Error(val message: String) : EntrarState()
}