package com.example.taskflow.data.repository

import com.example.taskflow.data.model.Projeto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.random.Random

class ProjetoRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun criarProjeto(
        nome: String,
        cor: String,
        callback: (Result<String>) -> Unit
    ) {
        val usuarioAtual = auth.currentUser
        if (usuarioAtual == null) {
            callback(Result.failure(Exception("Usuário não autenticado")))
            return
        }

        criarProjetoComCodigoUnico(nome, cor, usuarioAtual.uid, callback)
    }

    private fun criarProjetoComCodigoUnico(
        nome: String,
        cor: String,
        usuarioId: String,
        callback: (Result<String>) -> Unit
    ) {
        val codigo = gerarCodigoProjeto()

        verificarCodigoUnico(codigo) { isUnico ->
            if (isUnico) {
                // Código é único, criar o projeto
                salvarProjetoNoFirestore(nome, cor, usuarioId, codigo, callback)
            } else {
                // Código já existe, tentar novamente
                criarProjetoComCodigoUnico(nome, cor, usuarioId, callback)
            }
        }
    }

    private fun salvarProjetoNoFirestore(
        nome: String,
        cor: String,
        usuarioId: String,
        codigo: String,
        callback: (Result<String>) -> Unit
    ) {
        val projetoRef = db.collection("projetos").document()
        val projetoId = projetoRef.id

        val projeto = Projeto(
            id = projetoId,
            nome = nome,
            criador = usuarioId,
            membros = listOf(usuarioId),
            cor = cor,
            codigo = codigo
        )

        projetoRef.set(projeto)
            .addOnSuccessListener {
                callback(Result.success("Projeto '$nome' criado!\nCódigo: $codigo"))
            }
            .addOnFailureListener { e ->
                callback(Result.failure(e))
            }
    }

    private fun gerarCodigoProjeto(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..10)
            .map { chars[Random.Default.nextInt(chars.length)] }
            .joinToString("")
    }

    private fun verificarCodigoUnico(codigo: String, callback: (Boolean) -> Unit) {
        db.collection("projetos")
            .whereEqualTo("codigo", codigo)
            .get()
            .addOnSuccessListener { documents ->
                callback(documents.isEmpty)
            }
            .addOnFailureListener {
                callback(false)
            }
    }
}