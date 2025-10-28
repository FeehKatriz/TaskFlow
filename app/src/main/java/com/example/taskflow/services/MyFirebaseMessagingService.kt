package com.example.taskflow.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.taskflow.R
import com.example.taskflow.ui.main.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        const val TAG = "FCM_Service"
        const val CHANNEL_ID = "taskflow_notifications"
        const val CHANNEL_NAME = "TaskFlow Notificações"
    }

    /**
     * Chamado quando uma mensagem é recebida
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d(TAG, "📩 Mensagem recebida de: ${message.from}")
        Log.d(TAG, "📦 Dados: ${message.data}")

        // Processar data message
        if (message.data.isNotEmpty()) {
            handleDataMessage(message.data)
        }

        // Processar notification message (opcional)
        message.notification?.let {
            Log.d(TAG, "📌 Título: ${it.title}")
            Log.d(TAG, "📝 Corpo: ${it.body}")
        }
    }

    /**
     * Chamado quando um novo token é gerado
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "🔑 Novo token FCM: $token")

        // Salvar no Firestore
        salvarTokenNoFirestore(token)
    }

    /**
     * Processa mensagem de dados e exibe notificação
     */
    private fun handleDataMessage(data: Map<String, String>) {
        val tipo = data["tipo"] ?: ""
        val titulo = data["titulo"] ?: "TaskFlow"
        val mensagem = data["mensagem"] ?: ""
        val tarefaId = data["tarefaId"]
        val equipeId = data["equipeId"]

        Log.d(TAG, "🔄 Processando notificação: $tipo")

        // Salvar no histórico
        salvarNotificacaoNoHistorico(data)

        // Exibir notificação visual
        exibirNotificacao(titulo, mensagem, tipo, tarefaId, equipeId)
    }

    /**
     * Salva token no Firestore
     */
    private fun salvarTokenNoFirestore(token: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.w(TAG, "⚠️ Usuário não autenticado, token não salvo")
            return
        }

        FirebaseFirestore.getInstance()
            .collection("usuarios")
            .document(userId)
            .update("fcmToken", token)
            .addOnSuccessListener {
                Log.d(TAG, "✅ Token FCM salvo com sucesso!")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Erro ao salvar token: ${e.message}", e)

                // Tentar criar campo se não existir
                FirebaseFirestore.getInstance()
                    .collection("usuarios")
                    .document(userId)
                    .set(
                        mapOf("fcmToken" to token),
                        com.google.firebase.firestore.SetOptions.merge()
                    )
            }
    }

    /**
     * Salva notificação no Firestore para histórico
     */
    private fun salvarNotificacaoNoHistorico(data: Map<String, String>) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val notificacao = hashMapOf(
            "userId" to userId,
            "tipo" to (data["tipo"] ?: ""),
            "titulo" to (data["titulo"] ?: ""),
            "mensagem" to (data["mensagem"] ?: ""),
            "lida" to false,
            "timestamp" to com.google.firebase.Timestamp.now(),
            "tarefaId" to (data["tarefaId"] ?: ""),
            "equipeId" to (data["equipeId"] ?: ""),
            "projetoId" to (data["projetoId"] ?: "")
        )

        FirebaseFirestore.getInstance()
            .collection("notificacoes")
            .add(notificacao)
            .addOnSuccessListener {
                Log.d(TAG, "💾 Notificação salva no histórico")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Erro ao salvar notificação: ${e.message}", e)
            }
    }

    /**
     * Exibe notificação na barra de status
     */
    private fun exibirNotificacao(
        titulo: String,
        mensagem: String,
        tipo: String,
        tarefaId: String?,
        equipeId: String?
    ) {
        // Criar canal
        criarCanalNotificacao()

        // Intent para abrir o app
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("NOTIFICATION_TYPE", tipo)
            putExtra("TAREFA_ID", tarefaId)
            putExtra("EQUIPE_ID", equipeId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Construir notificação
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(titulo)
            .setContentText(mensagem)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mensagem))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 200, 500))

        // Exibir
        val notificationManager = getSystemService(NotificationManager::class.java)
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())

        Log.d(TAG, "🔔 Notificação exibida: $titulo")
    }

    /**
     * Cria canal de notificação
     */
    private fun criarCanalNotificacao() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações de tarefas, equipes e projetos"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                enableLights(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)

            Log.d(TAG, "📢 Canal de notificação criado")
        }
    }
}