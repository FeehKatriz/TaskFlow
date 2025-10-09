package com.example.taskflow.ui.projeto

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.taskflow.data.model.Projeto
import com.example.taskflow.data.repository.ProjetoRepository

class ProjetosViewModel : ViewModel() {

    private val repository = ProjetoRepository()

    private val _state = MutableLiveData<ProjetosState>(ProjetosState.Idle)
    val state: LiveData<ProjetosState> = _state

    private val _entrarProjetoState = MutableLiveData<ProjetosState>(ProjetosState.Idle)
    val entrarProjetoState: LiveData<ProjetosState> = _entrarProjetoState

    private val _codigoErro = MutableLiveData<String?>()
    val codigoErro: LiveData<String?> = _codigoErro

    fun carregarProjetos() {
        _state.value = ProjetosState.Loading

        repository.carregarProjetosDoUsuario { resultado ->
            resultado.onSuccess { projetos ->
                _state.value = ProjetosState.Success(projetos)
            }.onFailure { e ->
                _state.value = ProjetosState.Error(e.message ?: "Erro ao carregar projetos")
            }
        }
    }

    fun validarCodigo(codigo: String): Boolean {
        return if (codigo.isBlank()) {
            _codigoErro.value = "Digite o código do projeto"
            false
        } else {
            _codigoErro.value = null
            true
        }
    }

    fun entrarNoProjeto(codigo: String) {
        if (!validarCodigo(codigo)) {
            return
        }

        _entrarProjetoState.value = ProjetosState.Loading

        repository.entrarNoProjeto(codigo.trim().uppercase()) { resultado ->
            resultado.onSuccess { mensagem ->
                _entrarProjetoState.value = ProjetosState.Success(emptyList())
                // Recarregar projetos
                carregarProjetos()
            }.onFailure { e ->
                _entrarProjetoState.value = ProjetosState.Error(e.message ?: "Erro ao entrar no projeto")
            }
        }
    }

    fun limparEstado() {
        _state.value = ProjetosState.Idle
    }

    fun limparEstadoEntrarProjeto() {
        _entrarProjetoState.value = ProjetosState.Idle
    }
}