package com.example.taskflow.ui.notificacoes

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskflow.ui.tarefa.TarefaFragment
import com.example.taskflow.ui.equipe.EquipeFragment
import com.example.taskflow.ui.projeto.ProjetoFragment
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NotificationHostViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<NotificationHostState>(NotificationHostState.Loading)
    val uiState: StateFlow<NotificationHostState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<NotificationHostEvent>()
    val events: SharedFlow<NotificationHostEvent> = _events.asSharedFlow()

    /**
     * Processa o Intent e determina qual Fragment carregar
     */
    fun processarIntent(intent: Intent?) {
        viewModelScope.launch {
            if (intent == null) {
                _uiState.value = NotificationHostState.Error("Intent inválida")
                _events.emit(NotificationHostEvent.ShowMessage("Erro ao abrir notificação"))
                _events.emit(NotificationHostEvent.NavigateBack)
                return@launch
            }

            val destinationType = intent.getStringExtra(NotificationHostExtras.DESTINATION_TYPE)

            when (destinationType) {
                NotificationHostExtras.TYPE_TAREFA -> processarTarefa(intent)
                NotificationHostExtras.TYPE_EQUIPE -> processarEquipe(intent)
                NotificationHostExtras.TYPE_PROJETO -> processarProjeto(intent)
                else -> {
                    _uiState.value = NotificationHostState.Error("Tipo de destino desconhecido: $destinationType")
                    _events.emit(NotificationHostEvent.ShowMessage("Tipo de notificação não suportado"))
                    _events.emit(NotificationHostEvent.NavigateBack)
                }
            }
        }
    }

    private fun processarTarefa(intent: Intent) {
        val tarefaId = intent.getStringExtra(NotificationHostExtras.TAREFA_ID)

        if (tarefaId.isNullOrBlank()) {
            _uiState.value = NotificationHostState.Error("ID da tarefa não fornecido")
            viewModelScope.launch {
                _events.emit(NotificationHostEvent.ShowMessage("Erro: Tarefa não encontrada"))
                _events.emit(NotificationHostEvent.NavigateBack)
            }
            return
        }

        val comentarioId = intent.getStringExtra(NotificationHostExtras.COMENTARIO_ID)
        val scrollToComentario = intent.getBooleanExtra(NotificationHostExtras.SCROLL_TO_COMENTARIO, false)

        // Criar destino
        val destination = NotificationDestination.Tarefa(
            tarefaId = tarefaId,
            comentarioId = comentarioId,
            scrollToComentario = scrollToComentario
        )

        // Criar Fragment com argumentos
        val fragment = TarefaFragment().apply {
            arguments = Bundle().apply {
                putString("tarefaId", tarefaId)
                comentarioId?.let { putString("comentarioId", it) }
                if (scrollToComentario) {
                    putBoolean("scrollToComentario", true)
                }
            }
        }

        _uiState.value = NotificationHostState.Success(
            destination = destination,
            fragment = fragment
        )
    }

    private fun processarEquipe(intent: Intent) {
        val equipeId = intent.getStringExtra(NotificationHostExtras.EQUIPE_ID)

        if (equipeId.isNullOrBlank()) {
            _uiState.value = NotificationHostState.Error("ID da equipe não fornecido")
            viewModelScope.launch {
                _events.emit(NotificationHostEvent.ShowMessage("Erro: Equipe não encontrada"))
                _events.emit(NotificationHostEvent.NavigateBack)
            }
            return
        }

        val destination = NotificationDestination.Equipe(equipeId = equipeId)

        val fragment = EquipeFragment().apply {
            arguments = Bundle().apply {
                putString("equipeId", equipeId)
            }
        }

        _uiState.value = NotificationHostState.Success(
            destination = destination,
            fragment = fragment
        )
    }

    private fun processarProjeto(intent: Intent) {
        val projetoId = intent.getStringExtra(NotificationHostExtras.PROJETO_ID)

        if (projetoId.isNullOrBlank()) {
            _uiState.value = NotificationHostState.Error("ID do projeto não fornecido")
            viewModelScope.launch {
                _events.emit(NotificationHostEvent.ShowMessage("Erro: Projeto não encontrado"))
                _events.emit(NotificationHostEvent.NavigateBack)
            }
            return
        }

        val destination = NotificationDestination.Projeto(projetoId = projetoId)

        val fragment = ProjetoFragment().apply {
            arguments = Bundle().apply {
                putString("projetoId", projetoId)
            }
        }

        _uiState.value = NotificationHostState.Success(
            destination = destination,
            fragment = fragment
        )
    }

    /**
     * Helper para criar Intent de Tarefa
     */
    companion object {
        fun createTarefaIntent(
            context: android.content.Context,
            tarefaId: String,
            comentarioId: String? = null,
            scrollToComentario: Boolean = false
        ): Intent {
            return Intent(context, NotificationHostActivity::class.java).apply {
                putExtra(NotificationHostExtras.DESTINATION_TYPE, NotificationHostExtras.TYPE_TAREFA)
                putExtra(NotificationHostExtras.TAREFA_ID, tarefaId)
                comentarioId?.let { putExtra(NotificationHostExtras.COMENTARIO_ID, it) }
                if (scrollToComentario) {
                    putExtra(NotificationHostExtras.SCROLL_TO_COMENTARIO, true)
                }
            }
        }

        fun createEquipeIntent(
            context: android.content.Context,
            equipeId: String
        ): Intent {
            return Intent(context, NotificationHostActivity::class.java).apply {
                putExtra(NotificationHostExtras.DESTINATION_TYPE, NotificationHostExtras.TYPE_EQUIPE)
                putExtra(NotificationHostExtras.EQUIPE_ID, equipeId)
            }
        }

        fun createProjetoIntent(
            context: android.content.Context,
            projetoId: String
        ): Intent {
            return Intent(context, NotificationHostActivity::class.java).apply {
                putExtra(NotificationHostExtras.DESTINATION_TYPE, NotificationHostExtras.TYPE_PROJETO)
                putExtra(NotificationHostExtras.PROJETO_ID, projetoId)
            }
        }
    }
}