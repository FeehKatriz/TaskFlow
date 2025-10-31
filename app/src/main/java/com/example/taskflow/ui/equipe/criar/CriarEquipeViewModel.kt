package com.example.taskflow.ui.equipe.criar

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.taskflow.data.model.Equipe
import com.example.taskflow.data.repository.EquipeRepository

class CriarEquipeViewModel : ViewModel() {

    private val repository = EquipeRepository()

    private val _state = MutableLiveData<CriarEquipeState>(CriarEquipeState.Idle)
    val state: LiveData<CriarEquipeState> = _state

    private val _projetoId = MutableLiveData<String?>()
    val projetoId: LiveData<String?> = _projetoId

    fun inicializarProjeto(projetoIdRecebido: String?) {
        if (!projetoIdRecebido.isNullOrEmpty()) {
            _projetoId.value = projetoIdRecebido
        } else {
            carregarPrimeiroProjeto()
        }
    }

    private fun carregarPrimeiroProjeto() {
        repository.carregarPrimeiroProjeto { resultado ->
            resultado.onSuccess { projetoId ->
                _projetoId.value = projetoId
            }.onFailure { e ->
                _state.value = CriarEquipeState.Error(e.message ?: "Erro ao carregar projetos")
            }
        }
    }

    fun carregarUsuariosDisponiveis() {
        val pId = _projetoId.value
        if (pId.isNullOrEmpty()) {
            _state.value = CriarEquipeState.Error("Projeto não identificado")
            return
        }

        repository.carregarUsuariosDisponiveis(pId) { resultado ->
            resultado.onSuccess { usuarios ->
                _state.value = CriarEquipeState.UsuariosCarregados(usuarios)
            }.onFailure { e ->
                _state.value = CriarEquipeState.Error(e.message ?: "Erro ao carregar usuários")
            }
        }
    }

    fun validarCampos(nomeEquipe: String): String? {
        return when {
            nomeEquipe.isBlank() || nomeEquipe == "Nome da Equipe" ->
                "Digite o nome da equipe"
            _projetoId.value.isNullOrEmpty() ->
                "Erro: Projeto não selecionado"
            else -> null
        }
    }

    fun criarEquipe(nomeEquipe: String, membros: List<String>) {
        val erro = validarCampos(nomeEquipe)
        if (erro != null) {
            _state.value = CriarEquipeState.Error(erro)
            return
        }

        _state.value = CriarEquipeState.Loading

        val equipe = Equipe(
            nome = nomeEquipe,
            progresso = 0,
            totalTarefas = 0,
            projetoId = _projetoId.value ?: ""
        )

        repository.criarEquipeComMembros(equipe, membros) { resultado ->
            resultado.onSuccess {
                _state.value = CriarEquipeState.Success
            }.onFailure { e ->
                _state.value = CriarEquipeState.Error("Erro ao criar equipe: ${e.message}")
            }
        }
    }

    fun limparEstado() {
        if (_state.value is CriarEquipeState.Error) {
            _state.value = CriarEquipeState.Idle
        }
    }
}