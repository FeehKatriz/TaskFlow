package com.example.taskflow.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class UsuarioRepository {
    private val firebaseAuth = FirebaseAuth.getInstance()

    fun entrar(email: String, senha: String, callback: (Result<Unit>) -> Unit) {
        firebaseAuth.signInWithEmailAndPassword(email, senha)
            .addOnSuccessListener {
                callback(Result.success(Unit))
            }
            .addOnFailureListener { erro ->
                val mensagemErro = when (erro) {
                    is FirebaseAuthInvalidUserException -> "E-mail não cadastrado"
                    is FirebaseAuthInvalidCredentialsException -> "E-mail ou senha estão incorretos"
                    else -> erro.message ?: "Erro desconhecido"
                }
                callback(Result.failure(Exception(mensagemErro)))
            }
    }

    fun verificarUsuarioLogado(): Boolean {
        return firebaseAuth.currentUser != null
    }

    fun deslogar() {
        firebaseAuth.signOut()
    }
}