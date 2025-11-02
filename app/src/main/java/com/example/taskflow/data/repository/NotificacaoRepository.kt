package com.example.taskflow.data.repository

import com.example.taskflow.data.model.Notificacao
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class NotificacaoRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val notificacoesCollection = firestore.collection("notificacoes")

    fun observarNotificacoes(): Flow<List<Notificacao>> = callbackFlow {
        val userId = auth.currentUser?.uid ?: run {
            close()
            return@callbackFlow
        }

        val listener = notificacoesCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val notificacoes = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Notificacao::class.java)
                }?.sortedByDescending { it.timestamp.toDate() } ?: emptyList()

                trySend(notificacoes)
            }

        awaitClose { listener.remove() }
    }

    suspend fun marcarComoLida(notificacaoId: String): Result<Unit> {
        return try {
            notificacoesCollection.document(notificacaoId)
                .update(
                    mapOf(
                        "lida" to true,
                        "visualizada" to true
                    )
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun marcarTodasComoLidas(): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("Usuário não autenticado")

            val notificacoesNaoLidas = notificacoesCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("lida", false)
                .get()
                .await()

            val batch = firestore.batch()
            notificacoesNaoLidas.documents.forEach { doc ->
                batch.update(doc.reference, "lida", true)
                batch.update(doc.reference, "visualizada", true)
            }
            batch.commit().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletarNotificacao(notificacaoId: String): Result<Unit> {
        return try {
            notificacoesCollection.document(notificacaoId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletarTodasNotificacoes(): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("Usuário não autenticado")

            val todasNotificacoes = notificacoesCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val batch = firestore.batch()
            todasNotificacoes.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.commit().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun contarNaoLidas(): Int {
        return try {
            val userId = auth.currentUser?.uid ?: return 0

            val snapshot = notificacoesCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("lida", false)
                .get()
                .await()

            snapshot.size()
        } catch (e: Exception) {
            0
        }
    }
}