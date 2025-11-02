package com.example.taskflow.ui.projeto

import com.example.taskflow.data.model.Equipe

sealed class ProjetoState {
    object Idle : ProjetoState()
    object Loading : ProjetoState()

    data class InfoCarregada(
        val nomeProjeto: String,
        val codigoProjeto: String,
        val isCreator: Boolean,
        val isAdmin: Boolean // ✅ NOVO: se é administrador
    ) : ProjetoState()

    data class EquipesCarregadas(val equipes: List<Equipe>) : ProjetoState()

    data class MembrosCarregados(
        val membros: List<Map<String, String>>,
        val criadorId: String, // ✅ NOVO: para identificar o criador
        val adminsIds: List<String> // ✅ NOVO: lista de IDs dos admins
    ) : ProjetoState()

    data class CampoAtualizado(val mensagem: String) : ProjetoState()
    data class Error(val message: String) : ProjetoState()
}