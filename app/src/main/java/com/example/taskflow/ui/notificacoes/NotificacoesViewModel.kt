package com.example.taskflow.ui.notificacoes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskflow.data.model.Notificacao
import com.example.taskflow.data.repository.NotificacaoRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NotificacoesViewModel(
    private val repository: NotificacaoRepository = NotificacaoRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<NotificacoesState>(NotificacoesState.Loading)
    val uiState: StateFlow<NotificacoesState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<NotificacoesEvent>()
    val events: SharedFlow<NotificacoesEvent> = _events.asSharedFlow()

    init {
        observarNotificacoes()
    }

    private fun observarNotificacoes() {
        viewModelScope.launch {
            repository.observarNotificacoes()
                .catch { e ->
                    _uiState.value = NotificacoesState.Error(
                        e.message ?: "Erro ao carregar notificações"
                    )
                }
                .collect { notificacoes ->
                    if (notificacoes.isEmpty()) {
                        _uiState.value = NotificacoesState.Empty
                    } else {
                        val naoLidas = notificacoes.filter { !it.lida }
                        val lidas = notificacoes.filter { it.lida }

                        _uiState.value = NotificacoesState.Success(
                            notificacoesNaoLidas = naoLidas,
                            notificacoesLidas = lidas,
                            totalNaoLidas = naoLidas.size
                        )
                    }
                }
        }
    }

    fun marcarComoLida(notificacao: Notificacao) {
        if (notificacao.lida) return

        viewModelScope.launch {
            repository.marcarComoLida(notificacao.id)
                .onFailure { e ->
                    _events.emit(NotificacoesEvent.ShowMessage(
                        "Erro ao marcar como lida: ${e.message}"
                    ))
                }
        }
    }

    fun marcarTodasComoLidas() {
        viewModelScope.launch {
            repository.marcarTodasComoLidas()
                .onSuccess {
                    _events.emit(NotificacoesEvent.ShowMessage(
                        "Todas as notificações foram marcadas como lidas"
                    ))
                }
                .onFailure { e ->
                    _events.emit(NotificacoesEvent.ShowMessage(
                        "Erro: ${e.message}"
                    ))
                }
        }
    }

    fun deletarNotificacao(notificacao: Notificacao) {
        viewModelScope.launch {
            repository.deletarNotificacao(notificacao.id)
                .onSuccess {
                    _events.emit(NotificacoesEvent.ShowMessage("Notificação removida"))
                }
                .onFailure { e ->
                    _events.emit(NotificacoesEvent.ShowMessage(
                        "Erro ao deletar: ${e.message}"
                    ))
                }
        }
    }

    fun deletarTodasNotificacoes() {
        viewModelScope.launch {
            repository.deletarTodasNotificacoes()
                .onSuccess {
                    _events.emit(NotificacoesEvent.ShowMessage("Todas as notificações foram removidas"))
                }
                .onFailure { e ->
                    _events.emit(NotificacoesEvent.ShowMessage(
                        "Erro ao deletar todas: ${e.message}"
                    ))
                }
        }
    }


    fun onNotificacaoClick(notificacao: Notificacao) {
        viewModelScope.launch {
            marcarComoLida(notificacao)

            when (notificacao.tipo) {
                "NOVO_COMENTARIO" -> {
                    notificacao.tarefaId?.let { tarefaId ->
                        _events.emit(NotificacoesEvent.NavigateToTarefaWithComentario(
                            tarefaId = tarefaId,
                            comentarioId = notificacao.comentarioId,
                            scrollToComentario = true
                        ))
                    }
                }

                "TAREFA_CRIADA",
                "TAREFA_ATUALIZADA",
                "PRAZO_PROXIMO",
                "PRAZO_ATRASADO" -> {
                    notificacao.tarefaId?.let { tarefaId ->
                        _events.emit(NotificacoesEvent.NavigateToTarefa(tarefaId))
                    }
                }

                "EQUIPE_ATUALIZADA" -> {
                    notificacao.equipeId?.let { equipeId ->
                        _events.emit(NotificacoesEvent.NavigateToEquipe(equipeId))
                    }
                }

                else -> {
                    _events.emit(NotificacoesEvent.ShowMessage(
                        "Tipo de notificação não suportado: ${notificacao.tipo}"
                    ))
                }
            }
        }
    }
}