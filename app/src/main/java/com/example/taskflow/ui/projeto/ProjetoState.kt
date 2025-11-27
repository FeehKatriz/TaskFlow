package com.example.taskflow.ui.projeto

import com.example.taskflow.data.model.Equipe

sealed class ProjetoState {
    object Idle : ProjetoState()
    object Loading : ProjetoState()

    data class InfoCarregada(
        val nomeProjeto: String,
        val codigoProjeto: String,
        val isCreator: Boolean,
        val isAdmin: Boolean
    ) : ProjetoState()

    data class EquipesCarregadas(val equipes: List<Equipe>) : ProjetoState()

    data class MembrosCarregados(
        val membros: List<Map<String, String>>,
        val criadorId: String,
        val adminsIds: List<String>
    ) : ProjetoState()

    data class CampoAtualizado(val mensagem: String) : ProjetoState()

    // Estado quando usuário é removido do projeto
    object UsuarioRemovidoDoProjeto : ProjetoState()

    // Estado quando permissões de admin são revogadas
    object PermissoesRevogadas : ProjetoState()

    // Estado quando projeto é excluído com sucesso
    object ProjetoExcluido : ProjetoState()

    data class Error(val message: String) : ProjetoState()
}