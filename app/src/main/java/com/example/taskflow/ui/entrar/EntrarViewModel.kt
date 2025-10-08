package com.example.taskflow.ui.entrar

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.taskflow.data.repository.UsuarioRepository

class EntrarViewModel : ViewModel() {

    private val repository = UsuarioRepository()

    private val _state = MutableLiveData<EntrarState>(EntrarState.Idle)
    val state: LiveData<EntrarState> = _state

    private val _emailErro = MutableLiveData<String?>()
    val emailErro: LiveData<String?> = _emailErro

    private val _senhaErro = MutableLiveData<String?>()
    val senhaErro: LiveData<String?> = _senhaErro

    fun validarCampos(email: String, senha: String): Boolean {
        var valido = true

        if (email.isBlank()) {
            _emailErro.value = "Preencha o e-mail"
            valido = false
        } else {
            _emailErro.value = null
        }

        if (senha.isBlank()) {
            _senhaErro.value = "Preencha sua senha"
            valido = false
        } else {
            _senhaErro.value = null
        }

        return valido
    }

    fun entrar(email: String, senha: String) {
        if (!validarCampos(email, senha)) {
            return
        }

        _state.value = EntrarState.Loading

        repository.entrar(email.trim(), senha.trim()) { resultado ->
            resultado.onSuccess {
                _state.value = EntrarState.Success
            }.onFailure { erro ->
                _state.value = EntrarState.Error(erro.message ?: "Erro desconhecido")
            }
        }
    }

    fun verificarUsuarioLogado(): Boolean {
        return repository.verificarUsuarioLogado()
    }

    fun deslogar() {
        repository.deslogar()
    }

    fun limparEstado() {
        _state.value = EntrarState.Idle
    }
}