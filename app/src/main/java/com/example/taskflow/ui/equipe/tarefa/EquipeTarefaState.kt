package com.example.taskflow.ui.equipe.tarefa

import com.example.taskflow.data.model.Tarefa

sealed class EquipeTarefaState {
    object Idle : EquipeTarefaState()
    object Loading : EquipeTarefaState()
    data class Success(val tarefas: EquipeTarefasOrganizadas) : EquipeTarefaState()
    object NomeAtualizado : EquipeTarefaState()
    object EquipeExcluida : EquipeTarefaState()
    data class Error(val message: String) : EquipeTarefaState()
}

data class EquipeTarefasOrganizadas(
    val tarefasPendentes: List<Tarefa> = emptyList(),
    val tarefasAndamento: List<Tarefa> = emptyList(),
    val tarefasConcluidas: List<Tarefa> = emptyList()
)