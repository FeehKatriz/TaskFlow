package com.example.taskflow.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.taskflow.R
import com.example.taskflow.ui.main.MainActivity
import com.example.taskflow.ui.notificacoes.NotificacoesActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        const val TAG = "FCM_Service"
        const val CHANNEL_ID = "taskflow_notifications"
        const val CHANNEL_NAME = "TaskFlow Notificações"
        private const val PREFS_NAME = "fcm_prefs"
        private const val KEY_PENDING_TOKEN = "pending_token"

        /**
         * Método público para ser chamado após login
         * ✅ Verifica se há token pendente e salva
         */
        fun verificarESalvarTokenPendente(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val tokenPendente = prefs.getString(KEY_PENDING_TOKEN, null)

            if (tokenPendente != null) {
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId != null) {
                    Log.d(TAG, "🔄 Salvando token pendente após login")
                    FirebaseFirestore.getInstance()
                        .collection("usuarios")
                        .document(userId)
                        .set(
                            mapOf("fcmTokens" to FieldValue.arrayUnion(tokenPendente)),
                            com.google.firebase.firestore.SetOptions.merge()
                        )
                        .addOnSuccessListener {
                            Log.d(TAG, "✅ Token pendente salvo com sucesso!")
                            prefs.edit().remove(KEY_PENDING_TOKEN).apply()
                        }
                }
            }
        }
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
     * ✅ ATUALIZADO: Salva em array e lida com usuário não logado
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "🔑 Novo token FCM: $token")

        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            // Usuário está logado, salvar imediatamente
            salvarTokenNoFirestore(token, userId)
        } else {
            // Usuário não está logado, salvar token pendente
            Log.w(TAG, "⚠️ Usuário não logado, salvando token pendente")
            salvarTokenPendente(token)
        }
    }

    /**
     * Salva token pendente no SharedPreferences
     * (será salvo no Firestore quando o usuário logar)
     */
    private fun salvarTokenPendente(token: String) {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PENDING_TOKEN, token)
            .apply()
        Log.d(TAG, "💾 Token pendente salvo localmente")
    }

    /**
     * Salva token no Firestore usando ARRAY (sem duplicatas)
     * ✅ ATUALIZADO: Usa arrayUnion para evitar duplicação
     */
    private fun salvarTokenNoFirestore(token: String, userId: String) {
        FirebaseFirestore.getInstance()
            .collection("usuarios")
            .document(userId)
            .set(
                mapOf("fcmTokens" to FieldValue.arrayUnion(token)),
                com.google.firebase.firestore.SetOptions.merge()
            )
            .addOnSuccessListener {
                Log.d(TAG, "✅ Token adicionado ao array no Firestore!")
                // Limpar token pendente se houver
                limparTokenPendente()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Erro ao salvar token: ${e.message}", e)
            }
    }

    /**
     * Limpa token pendente do SharedPreferences
     */
    private fun limparTokenPendente() {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_PENDING_TOKEN)
            .apply()
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

        // Exibir notificação visual
        exibirNotificacao(titulo, mensagem, tipo, tarefaId, equipeId)
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
        val intent = Intent(this, NotificacoesActivity::class.java).apply {
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