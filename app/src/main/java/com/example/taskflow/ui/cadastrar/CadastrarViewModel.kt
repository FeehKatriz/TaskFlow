package com.example.taskflow.ui.cadastrar

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class CadastrarViewModel : ViewModel() {

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val fireStore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val _state = MutableLiveData<CadastrarState>(CadastrarState.Idle)
    val state: LiveData<CadastrarState> = _state

    fun validarCampos(
        nome: String,
        email: String,
        senha: String,
        confirmaSenha: String
    ): String? {
        return when {
            nome.isBlank() || email.isBlank() || senha.isBlank() || confirmaSenha.isBlank() ->
                "Preencha todos os campos"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                "Email inválido"
            senha.length < 6 ->
                "Senha muito curta (mínimo 6 caracteres)"
            !validarSenhaForte(senha) ->
                "A senha deve conter letra maiúscula, minúscula, número e caractere especial"
            senha != confirmaSenha ->
                "Senhas não coincidem"
            else -> null
        }
    }

    private fun validarSenhaForte(senha: String): Boolean {
        val temMaiuscula = senha.any { it.isUpperCase() }
        val temMinuscula = senha.any { it.isLowerCase() }
        val temNumero = senha.any { it.isDigit() }
        val temEspecial = senha.any { !it.isLetterOrDigit() }
        return temMaiuscula && temMinuscula && temNumero && temEspecial
    }

    fun cadastrarUsuario(
        nome: String,
        email: String,
        senha: String,
        confirmaSenha: String,
        imageUri: Uri?
    ) {
        val erro = validarCampos(nome, email, senha, confirmaSenha)
        if (erro != null) {
            _state.value = CadastrarState.Error(erro)
            return
        }

        _state.value = CadastrarState.Loading

        firebaseAuth.createUserWithEmailAndPassword(email, senha)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    user?.let {
                        if (imageUri != null) {
                            val fotoRef = storage.reference.child("usuarios/${user.uid}/fotoPerfil.jpg")
                            fotoRef.putFile(imageUri)
                                .addOnSuccessListener {
                                    fotoRef.downloadUrl.addOnSuccessListener { uri ->
                                        salvarDadosFirestore(user.uid, nome, email, uri.toString())
                                    }
                                }
                                .addOnFailureListener { e ->
                                    _state.value = CadastrarState.Error("Erro ao enviar foto: ${e.message}")
                                }
                        } else {
                            salvarDadosFirestore(user.uid, nome, email, null)
                        }
                    }
                } else {
                    val errorMessage = task.exception?.message ?: "Erro desconhecido"
                    _state.value = CadastrarState.Error("Erro no cadastro: $errorMessage")
                }
            }
    }

    private fun salvarDadosFirestore(
        uid: String,
        nome: String,
        email: String,
        fotoUrl: String?
    ) {
        val userData = hashMapOf(
            "nome" to nome,
            "email" to email,
            "fotoUrl" to (fotoUrl ?: ""),
            "dataCriacao" to Timestamp.now()
        )

        fireStore.collection("usuarios")
            .document(uid)
            .set(userData)
            .addOnSuccessListener {
                enviarEmailVerificacao(email)
            }
            .addOnFailureListener { exception ->
                _state.value = CadastrarState.Error("Erro ao salvar dados: ${exception.message}")
                firebaseAuth.currentUser?.delete()
            }
    }

    private fun enviarEmailVerificacao(email: String) {
        val user = firebaseAuth.currentUser
        user?.sendEmailVerification()
            ?.addOnSuccessListener {
                _state.value = CadastrarState.EmailVerificationSent(email)
            }
            ?.addOnFailureListener { exception ->
                _state.value = CadastrarState.Error("Erro ao enviar email de verificação: ${exception.message}")
            }
    }

    fun reenviarEmailVerificacao() {
        val user = firebaseAuth.currentUser
        if (user != null) {
            user.sendEmailVerification()
                .addOnSuccessListener {
                    _state.value = CadastrarState.EmailResent("Email reenviado com sucesso!")
                }
                .addOnFailureListener { exception ->
                    _state.value = CadastrarState.Error("Erro ao reenviar email: ${exception.message}")
                }
        } else {
            _state.value = CadastrarState.Error("Usuário não encontrado")
        }
    }

    fun voltarParaIdle() {
        _state.value = CadastrarState.Idle
    }
}