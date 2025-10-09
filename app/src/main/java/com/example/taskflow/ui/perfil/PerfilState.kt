package com.example.taskflow.ui.perfil

sealed class PerfilState {
    object Idle : PerfilState()
    object Loading : PerfilState()
    data class DadosCarregados(val usuario: com.example.taskflow.data.model.Usuario) : PerfilState()
    object Salvando : PerfilState()
    object Success : PerfilState()
    data class Error(val message: String) : PerfilState()
}