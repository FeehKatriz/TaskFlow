package com.example.taskflow.ui.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskflow.data.repository.NotificacaoRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val notificacaoRepository: NotificacaoRepository = NotificacaoRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<MainState>(MainState.Loading)
    val uiState: StateFlow<MainState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<MainEvent>()
    val events: SharedFlow<MainEvent> = _events.asSharedFlow()

    private val _notificacoesNaoLidas = MutableStateFlow(0)
    val notificacoesNaoLidas: StateFlow<Int> = _notificacoesNaoLidas.asStateFlow()

    init {
        carregarDadosUsuario()
        observarNotificacoes()
    }

    /**
     * Carrega dados do usuário (nome e foto)
     */
    private fun carregarDadosUsuario() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _uiState.value = MainState.Error("Usuário não autenticado")
            return
        }

        viewModelScope.launch {
            firestore.collection("usuarios")
                .document(userId)
                .addSnapshotListener { document, error ->
                    if (error != null) {
                        Log.e("MainViewModel", "Erro ao carregar usuário", error)
                        _uiState.value = MainState.Success(
                            nomeUsuario = "Usuário",
                            fotoPerfilUrl = null,
                            notificacoesNaoLidas = _notificacoesNaoLidas.value
                        )
                        return@addSnapshotListener
                    }

                    val nome = document?.getString("nome") ?: "Usuário"

                    // Buscar URL da foto
                    buscarFotoPerfilUrl(userId, nome)
                }
        }
    }

    /**
     * Busca URL da foto de perfil
     */
    private fun buscarFotoPerfilUrl(userId: String, nome: String) {
        viewModelScope.launch {
            try {
                val ref = storage.getReference("usuarios/$userId/fotoPerfil.jpg")
                val url = ref.downloadUrl.await()

                _uiState.value = MainState.Success(
                    nomeUsuario = nome,
                    fotoPerfilUrl = url.toString(),
                    notificacoesNaoLidas = _notificacoesNaoLidas.value
                )
            } catch (e: Exception) {
                _uiState.value = MainState.Success(
                    nomeUsuario = nome,
                    fotoPerfilUrl = null,
                    notificacoesNaoLidas = _notificacoesNaoLidas.value
                )
            }
        }
    }

    /**
     * Observa notificações não lidas
     */
    private fun observarNotificacoes() {
        viewModelScope.launch {
            notificacaoRepository.observarNotificacoes()
                .catch { e ->
                    Log.e("MainViewModel", "Erro ao observar notificações", e)
                }
                .collect { notificacoes ->
                    val count = notificacoes.count { !it.lida }
                    _notificacoesNaoLidas.value = count

                    // Atualizar também o state
                    val currentState = _uiState.value
                    if (currentState is MainState.Success) {
                        _uiState.value = currentState.copy(notificacoesNaoLidas = count)
                    }
                }
        }
    }

    /**
     * Salva token FCM no Firestore
     */
    fun salvarTokenFCM(token: String) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w("MainViewModel", "⚠️ Usuário não autenticado")
            return
        }

        viewModelScope.launch {
            try {
                firestore.collection("usuarios")
                    .document(userId)
                    .set(
                        mapOf("fcmTokens" to FieldValue.arrayUnion(token)),
                        com.google.firebase.firestore.SetOptions.merge()
                    )
                    .await()

                Log.d("MainViewModel", "✅ Token FCM salvo com sucesso!")
            } catch (e: Exception) {
                Log.e("MainViewModel", "❌ Erro ao salvar token FCM", e)
            }
        }
    }

    /**
     * Remove token FCM ao deslogar
     */
    suspend fun removerTokenFCM(): Boolean {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e("MainViewModel", "❌ Usuário não autenticado")
            return false
        }

        return try {
            val token = FirebaseMessaging.getInstance().token.await()

            firestore.collection("usuarios")
                .document(userId)
                .update("fcmTokens", FieldValue.arrayRemove(token))
                .await()

            Log.d("MainViewModel", "✅ Token removido com sucesso!")
            true
        } catch (e: Exception) {
            Log.e("MainViewModel", "❌ Erro ao remover token", e)
            false
        }
    }

    /**
     * Processa notificação recebida
     */
    fun processarNotificacao(tipo: String?, tarefaId: String?, equipeId: String?) {
        viewModelScope.launch {
            if (tipo == null) return@launch

            Log.d("MainViewModel", "🔔 Processando notificação: $tipo")

            when (tipo) {
                "TAREFA_ATRIBUIDA", "TAREFA_ATUALIZADA", "NOVO_COMENTARIO" -> {
                    tarefaId?.let {
                        _events.emit(MainEvent.NavigateToTarefa(it))
                    }
                }
                "CONVITE_EQUIPE" -> {
                    equipeId?.let {
                        _events.emit(MainEvent.NavigateToEquipe(it))
                    }
                }
            }
        }
    }

    /**
     * Recarrega foto do perfil (chamado após edição)
     */
    fun recarregarFotoPerfil() {
        val userId = auth.currentUser?.uid ?: return
        val currentState = _uiState.value

        if (currentState is MainState.Success) {
            buscarFotoPerfilUrl(userId, currentState.nomeUsuario)
        }
    }
}