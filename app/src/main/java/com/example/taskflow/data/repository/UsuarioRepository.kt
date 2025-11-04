package com.example.taskflow.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.EmailAuthProvider

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

    fun enviarEmailRecuperacaoSenha(email: String, callback: (Result<Unit>) -> Unit) {
        firebaseAuth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                callback(Result.success(Unit))
            }
            .addOnFailureListener { erro ->
                val mensagem = erro.message ?: "Erro ao enviar email de recuperação"
                callback(Result.failure(Exception(mensagem)))
            }
    }

    fun carregarDadosUsuario(uid: String, callback: (Result<com.example.taskflow.data.model.Usuario>) -> Unit) {
        val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()

        firestore.collection("usuarios")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val usuario = document.toObject(com.example.taskflow.data.model.Usuario::class.java)
                    if (usuario != null) {
                        callback(Result.success(usuario))
                    } else {
                        callback(Result.failure(Exception("Erro ao converter dados do usuário")))
                    }
                } else {
                    callback(Result.failure(Exception("Documento do usuário não encontrado")))
                }
            }
            .addOnFailureListener { e ->
                callback(Result.failure(e))
            }
    }

    fun uploadFotoPerfil(uid: String, imageUri: android.net.Uri, callback: (Result<String>) -> Unit) {
        val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference
        val fotoRef = storageRef.child("usuarios/$uid/fotoPerfil.jpg")

        fotoRef.putFile(imageUri)
            .addOnSuccessListener {
                fotoRef.downloadUrl.addOnSuccessListener { uri ->
                    callback(Result.success(uri.toString()))
                }
            }
            .addOnFailureListener { e ->
                callback(Result.failure(e))
            }
    }

    fun salvarAlteracoesPerfil(
        uid: String,
        nome: String?,
        fotoUrl: String?,
        callback: (Result<Unit>) -> Unit
    ) {
        val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val dadosAtualizados = mutableMapOf<String, Any>()

        if (nome != null) dadosAtualizados["nome"] = nome
        if (fotoUrl != null) dadosAtualizados["fotoUrl"] = fotoUrl

        if (dadosAtualizados.isEmpty()) {
            callback(Result.success(Unit))
            return
        }

        firestore.collection("usuarios")
            .document(uid)
            .update(dadosAtualizados)
            .addOnSuccessListener {
                callback(Result.success(Unit))
            }
            .addOnFailureListener { e ->
                callback(Result.failure(e))
            }
    }

    fun trocarSenha(senhaAtual: String, novaSenha: String, callback: (Result<Unit>) -> Unit) {
        val user = firebaseAuth.currentUser
        val email = user?.email

        if (user == null || email == null) {
            callback(Result.failure(Exception("Usuário não autenticado")))
            return
        }

        // Reautenticar com senha atual
        val credential = EmailAuthProvider.getCredential(email, senhaAtual)

        user.reauthenticate(credential)
            .addOnSuccessListener {
                // Senha atual correta, agora atualizar para nova senha
                user.updatePassword(novaSenha)
                    .addOnSuccessListener {
                        callback(Result.success(Unit))
                    }
                    .addOnFailureListener { e ->
                        callback(Result.failure(Exception("Erro ao atualizar senha: ${e.message}")))
                    }
            }
            .addOnFailureListener {
                callback(Result.failure(Exception("Senha atual incorreta")))
            }
    }

    fun salvarDadosUsuarioGoogle(
        uid: String,
        nome: String,
        email: String,
        fotoUrl: String?,
        callback: (Result<Unit>) -> Unit
    ) {
        val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()

        val userData = hashMapOf(
            "nome" to nome,
            "email" to email,
            "fotoUrl" to (fotoUrl ?: ""),
            "dataCriacao" to com.google.firebase.Timestamp.now(),
            "provider" to "google"
        )

        firestore.collection("usuarios")
            .document(uid)
            .set(userData)
            .addOnSuccessListener {
                callback(Result.success(Unit))
            }
            .addOnFailureListener { e ->
                callback(Result.failure(e))
            }
    }
}