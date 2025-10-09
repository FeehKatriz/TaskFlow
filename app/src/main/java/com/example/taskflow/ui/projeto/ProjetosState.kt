package com.example.taskflow.ui.projeto

import com.example.taskflow.data.model.Projeto

sealed class ProjetosState {
    object Idle : ProjetosState()
    object Loading : ProjetosState()
    data class Success(val projetos: List<Projeto>) : ProjetosState()
    data class Error(val message: String) : ProjetosState()
}