package com.example.taskflow.ui.projeto

import com.example.taskflow.data.model.Equipe

sealed class ProjetoState {
    object Idle : ProjetoState()
    object Loading : ProjetoState()
    data class InfoCarregada(
        val nomeProjeto: String,
        val codigoProjeto: String,
        val isCreator: Boolean
    ) : ProjetoState()
    data class EquipesCarregadas(val equipes: List<Equipe>) : ProjetoState()
    data class MembrosCarregados(val membros: List<Map<String, String>>) : ProjetoState()
    data class Error(val message: String) : ProjetoState()
}