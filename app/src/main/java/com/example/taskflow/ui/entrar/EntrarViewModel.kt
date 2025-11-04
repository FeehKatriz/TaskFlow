package com.example.taskflow.ui.entrar

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.taskflow.data.repository.UsuarioRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

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
                val user = firebaseAuth.currentUser
                if (user?.isEmailVerified == true) {
                    _state.value = EntrarState.Success
                } else {
                    _state.value = EntrarState.EmailNotVerified(email.trim())
                }
            }.onFailure { erro ->
                _state.value = EntrarState.Error(erro.message ?: "Erro desconhecido")
            }
        }
    }

    // Novo método para login com Google
    fun entrarComGoogle(idToken: String) {
        _state.value = EntrarState.Loading

        val credential = GoogleAuthProvider.getCredential(idToken, null)

        firebaseAuth.signInWithCredential(credential)
            .addOnSuccessListener { authResult ->
                // Obter informações do usuário
                val user = authResult.user
                if (user != null) {
                    // Verificar se é o primeiro login (usuário novo)
                    val isNewUser = authResult.additionalUserInfo?.isNewUser ?: false

                    if (isNewUser) {
                        // Salvar dados do novo usuário no Firestore
                        salvarDadosUsuarioGoogle(user)
                    } else {
                        // Usuário já existe, só fazer login
                        _state.value = EntrarState.Success
                    }
                } else {
                    _state.value = EntrarState.Error("Erro ao obter dados do usuário")
                }
            }
            .addOnFailureListener { exception ->
                _state.value = EntrarState.Error(
                    exception.message ?: "Erro ao fazer login com Google"
                )
            }
    }

    private fun salvarDadosUsuarioGoogle(user: com.google.firebase.auth.FirebaseUser) {
        val nome = user.displayName ?: ""
        val email = user.email ?: ""
        val fotoUrl = user.photoUrl?.toString()

        repository.salvarDadosUsuarioGoogle(user.uid, nome, email, fotoUrl) { resultado ->
            resultado.onSuccess {
                // Também atualizar o profile do FirebaseUser para facilitar acesso
                val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                    displayName = nome
                    photoUri = user.photoUrl
                }
                user.updateProfile(profileUpdates)

                _state.value = EntrarState.Success
            }.onFailure { erro ->
                _state.value = EntrarState.Error(
                    "Erro ao salvar dados: ${erro.message}"
                )
            }
        }
    }

    fun reenviarEmailVerificacao() {
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
        val user = firebaseAuth.currentUser
        if (user != null && !user.isEmailVerified) {
            repository.deslogar()
        }
        _state.value = EntrarState.Idle
    }
}