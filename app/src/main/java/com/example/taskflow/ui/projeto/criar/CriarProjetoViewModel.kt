package com.example.taskflow.ui.projeto.criar

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.taskflow.data.repository.ProjetoRepository

class CriarProjetoViewModel : ViewModel() {

    private val repository = ProjetoRepository()

    private val _state = MutableLiveData<CriarProjetoState>(CriarProjetoState.Idle)
    val state: LiveData<CriarProjetoState> = _state

    private val _corSelecionada = MutableLiveData<String>("#3F51B5")
    val corSelecionada: LiveData<String> = _corSelecionada

    private val _nomeErro = MutableLiveData<String?>()
    val nomeErro: LiveData<String?> = _nomeErro

    fun setSelecionadaCor(cor: String) {
        _corSelecionada.value = cor
    }

    fun validarCampos(nome: String): Boolean {
        if (nome.isBlank()) {
            _nomeErro.value = "Por favor, insira o nome do projeto"
            return false
        }
        _nomeErro.value = null
        return true
    }

    fun criarProjeto(nome: String) {
        if (!validarCampos(nome)) {
            return
        }

        _state.value = CriarProjetoState.Loading

        val cor = _corSelecionada.value ?: "#3F51B5"

        repository.criarProjeto(nome, cor) { resultado ->
            resultado.onSuccess { mensagem ->
                _state.value = CriarProjetoState.Success
            }.onFailure { e ->
                val mensagem = e.message ?: "Erro desconhecido ao criar projeto"
                _state.value = CriarProjetoState.Error(mensagem)
            }
        }
    }

    fun limparEstado() {
        _state.value = CriarProjetoState.Idle
    }
}