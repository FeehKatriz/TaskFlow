package com.example.taskflow.ui.equipe.tarefa

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.taskflow.data.repository.TarefaRepository

class EquipeTarefaViewModel : ViewModel() {

    private val repository = TarefaRepository()

    private val _state = MutableLiveData<EquipeTarefaState>(EquipeTarefaState.Idle)
    val state: LiveData<EquipeTarefaState> = _state

    private val _projetoId = MutableLiveData<String?>("")
    val projetoId: LiveData<String?> = _projetoId

    private val _nomeEquipe = MutableLiveData<String>("")
    val nomeEquipe: LiveData<String> = _nomeEquipe

    fun inicializarDados(equipeId: String, projetoIdRecebido: String?) {
        // Buscar nome da equipe
        buscarNomeEquipe(equipeId)

        // Se já tem projeto ID, não precisa buscar
        if (!projetoIdRecebido.isNullOrEmpty()) {
            _projetoId.value = projetoIdRecebido
        } else {
            buscarProjetoDaEquipe(equipeId)
        }

        carregarTarefas(equipeId)
    }

    private fun buscarNomeEquipe(equipeId: String) {
        repository.buscarNomeEquipe(equipeId) { resultado ->
            resultado.onSuccess { nome ->
                _nomeEquipe.value = nome
            }.onFailure { e ->
                _nomeEquipe.value = "Equipe"
            }
        }
    }

    private fun buscarProjetoDaEquipe(equipeId: String) {
        repository.buscarProjetoDaEquipe(equipeId) { resultado ->
            resultado.onSuccess { projetoId ->
                _projetoId.value = projetoId
            }.onFailure { e ->
                // Log do erro, mas não impede o carregamento de tarefas
                _state.value = EquipeTarefaState.Error("Erro ao buscar projeto: ${e.message}")
            }
        }
    }

    fun carregarTarefas(equipeId: String) {
        _state.value = EquipeTarefaState.Loading

        repository.carregarTarefasComListener(equipeId) { resultado ->
            resultado.onSuccess { tarefas ->
                _state.value = EquipeTarefaState.Success(tarefas)
            }.onFailure { e ->
                _state.value = EquipeTarefaState.Error(e.message ?: "Erro ao carregar tarefas")
            }
        }
    }

    fun limparEstado() {
        _state.value = EquipeTarefaState.Idle
    }
}