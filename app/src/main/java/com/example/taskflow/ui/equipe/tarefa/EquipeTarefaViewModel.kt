package com.example.taskflow.ui.equipe.tarefa

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.taskflow.data.repository.TarefaRepository
import com.example.taskflow.data.repository.EquipeRepository

class EquipeTarefaViewModel : ViewModel() {

    private val tarefaRepository = TarefaRepository()
    private val equipeRepository = EquipeRepository()

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
        tarefaRepository.buscarNomeEquipe(equipeId) { resultado ->
            resultado.onSuccess { nome ->
                _nomeEquipe.value = nome
            }.onFailure { e ->
                _nomeEquipe.value = "Equipe"
            }
        }
    }

    private fun buscarProjetoDaEquipe(equipeId: String) {
        tarefaRepository.buscarProjetoDaEquipe(equipeId) { resultado ->
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

        tarefaRepository.carregarTarefasComListener(equipeId) { resultado ->
            resultado.onSuccess { tarefas ->
                _state.value = EquipeTarefaState.Success(tarefas)
            }.onFailure { e ->
                _state.value = EquipeTarefaState.Error(e.message ?: "Erro ao carregar tarefas")
            }
        }
    }

    // ==================== EDIÇÃO DO NOME (NOVO) ====================

    fun atualizarNomeEquipe(equipeId: String, novoNome: String) {
        if (novoNome.isBlank()) {
            _state.value = EquipeTarefaState.Error("Nome não pode estar vazio")
            return
        }

        _state.value = EquipeTarefaState.Loading

        equipeRepository.atualizarNomeEquipe(equipeId, novoNome) { resultado ->
            resultado.onSuccess {
                _nomeEquipe.value = novoNome
                _state.value = EquipeTarefaState.NomeAtualizado
            }.onFailure { e ->
                _state.value = EquipeTarefaState.Error("Erro ao atualizar nome: ${e.message}")
            }
        }
    }

    // ==================== EXCLUSÃO EM CASCATA (NOVO) ====================

    fun excluirEquipe(equipeId: String) {
        _state.value = EquipeTarefaState.Loading

        equipeRepository.excluirEquipeComTarefas(equipeId) { resultado ->
            resultado.onSuccess {
                _state.value = EquipeTarefaState.EquipeExcluida
            }.onFailure { e ->
                _state.value = EquipeTarefaState.Error("Erro ao excluir equipe: ${e.message}")
            }
        }
    }

    fun limparEstado() {
        _state.value = EquipeTarefaState.Idle
    }
}