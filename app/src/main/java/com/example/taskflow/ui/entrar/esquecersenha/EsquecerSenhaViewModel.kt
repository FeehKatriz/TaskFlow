package com.example.taskflow.ui.entrar.esquecersenha

import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.taskflow.data.repository.UsuarioRepository

class EsquecerSenhaViewModel : ViewModel() {

    private val repository = UsuarioRepository()

    private val _state = MutableLiveData<EsquecerSenhaState>(EsquecerSenhaState.Idle)
    val state: LiveData<EsquecerSenhaState> = _state

    private val _emailErro = MutableLiveData<String?>()
    val emailErro: LiveData<String?> = _emailErro

    fun validarEmail(email: String): Boolean {
        return when {
            email.isBlank() -> {
                _emailErro.value = "Preencha o e-mail"
                false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                _emailErro.value = "Formato de email inválido"
                false
            }
            else -> {
                _emailErro.value = null
                true
            }
        }
    }

    fun enviarEmailRecuperacao(email: String) {
        if (!validarEmail(email)) {
            return
        }

        _state.value = EsquecerSenhaState.Loading

        repository.enviarEmailRecuperacaoSenha(email.trim()) { resultado ->
            resultado.onSuccess {
                _state.value = EsquecerSenhaState.Success
            }.onFailure { erro ->
                val mensagem = erro.message ?: "Erro desconhecido"
                _state.value = EsquecerSenhaState.Error(mensagem)
            }
        }
    }

    fun limparEstado() {
        _state.value = EsquecerSenhaState.Idle
    }
}