package com.example.taskflow.ui.tarefa.criar

sealed class CriarTarefaState {
    object Idle : CriarTarefaState()
    object Loading : CriarTarefaState()
    object DadosCarregando : CriarTarefaState()
    object DadosCarregados : CriarTarefaState()
    data class MembrosCarregados(val membros: List<Pair<String, String>>) : CriarTarefaState()
    object Success : CriarTarefaState()
    data class Error(val message: String) : CriarTarefaState()
}