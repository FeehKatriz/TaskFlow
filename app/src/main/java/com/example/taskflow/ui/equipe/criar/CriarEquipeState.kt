package com.example.taskflow.ui.equipe.criar

sealed class CriarEquipeState {
    object Idle : CriarEquipeState()
    object Loading : CriarEquipeState()
    object Success : CriarEquipeState()
    data class UsuariosCarregados(val usuarios: List<Pair<String, String>>) : CriarEquipeState()
    data class Error(val message: String) : CriarEquipeState()
}