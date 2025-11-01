package com.example.taskflow.ui.entrar

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.taskflow.data.repository.UsuarioRepository
import com.google.firebase.auth.FirebaseAuth

class EntrarViewModel : ViewModel() {

    private val repository = UsuarioRepository()
    private val firebaseAuth = FirebaseAuth.getInstance()

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
                // Verificar se o email foi verificado
                val user = firebaseAuth.currentUser
                if (user?.isEmailVerified == true) {
                    _state.value = EntrarState.Success
                } else {
                    // Email não verificado - usuário permanece logado
                    // para poder reenviar o email de verificação
                    _state.value = EntrarState.EmailNotVerified(email.trim())
                }
            }.onFailure { erro ->
                _state.value = EntrarState.Error(erro.message ?: "Erro desconhecido")
            }
        }
    }

    fun reenviarEmailVerificacao() {
        // O usuário JÁ está logado, então currentUser não será null
        val user = firebaseAuth.currentUser
        if (user != null) {
            user.sendEmailVerification()
                .addOnSuccessListener {
                    _state.value = EntrarState.EmailResent("Email reenviado com sucesso!")
                }
                .addOnFailureListener { exception ->
                    _state.value = EntrarState.Error("Erro ao reenviar email: ${exception.message}")
                }
        } else {
            _state.value = EntrarState.Error("Usuário não encontrado")
        }
    }

    fun verificarUsuarioLogado(): Boolean {
        return repository.verificarUsuarioLogado()
    }

    fun deslogar() {
        repository.deslogar()
    }

    fun limparEstado() {
        // Fazer logout apenas se o email ainda não foi verificado
        val user = firebaseAuth.currentUser
        if (user != null && !user.isEmailVerified) {
            repository.deslogar()
        }
        _state.value = EntrarState.Idle
    }
}