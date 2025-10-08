package com.example.taskflow.data.repository

import com.example.taskflow.data.model.Equipe
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EquipeRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Buscar o primeiro projeto do usuário
    fun carregarPrimeiroProjeto(callback: (Result<String>) -> Unit) {
        val userId = auth.currentUser?.uid ?: run {
            callback(Result.failure(Exception("Usuário não autenticado")))
            return
        }

        firestore.collection("projetos")
            .whereArrayContains("membros", userId)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    val projetoId = snapshot.documents[0].id
                    callback(Result.success(projetoId))
                } else {
                    callback(Result.failure(Exception("Você precisa fazer parte de um projeto primeiro")))
                }
            }
            .addOnFailureListener { e ->
                callback(Result.failure(e))
            }
    }

    // Criar uma nova equipe
    fun criarEquipe(equipe: Equipe, callback: (Result<Unit>) -> Unit) {
        firestore.collection("equipes")
            .add(equipe)
            .addOnSuccessListener { documentReference ->
                documentReference.update("id", documentReference.id)
                    .addOnSuccessListener {
                        callback(Result.success(Unit))
                    }
                    .addOnFailureListener { e ->
                        // Equipe criada mesmo com erro ao atualizar ID
                        callback(Result.success(Unit))
                    }
            }
            .addOnFailureListener { e ->
                callback(Result.failure(e))
            }
    }
}