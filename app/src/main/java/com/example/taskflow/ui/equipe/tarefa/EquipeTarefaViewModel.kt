package com.example.taskflow.ui.equipe.tarefa

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.taskflow.data.repository.TarefaRepository
import com.example.taskflow.data.repository.EquipeRepository
import com.example.taskflow.data.repository.ProjetoRepository
import com.google.firebase.auth.FirebaseAuth

class EquipeTarefaViewModel : ViewModel() {

    private val tarefaRepository = TarefaRepository()
    private val equipeRepository = EquipeRepository()
    private val projetoRepository = ProjetoRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _state = MutableLiveData<EquipeTarefaState>(EquipeTarefaState.Idle)
    val state: LiveData<EquipeTarefaState> = _state

    private val _projetoId = MutableLiveData<String?>("")
    val projetoId: LiveData<String?> = _projetoId

    private val _nomeEquipe = MutableLiveData<String>("")
    val nomeEquipe: LiveData<String> = _nomeEquipe

    // ✅ NOVO: Controle de permissões de admin
    private val _isAdmin = MutableLiveData<Boolean>(false)
    val isAdmin: LiveData<Boolean> = _isAdmin

    private val _isCreator = MutableLiveData<Boolean>(false)
    val isCreator: LiveData<Boolean> = _isCreator

    private val _podeEditar = MutableLiveData<Boolean>(false)
    val podeEditar: LiveData<Boolean> = _podeEditar

    fun inicializarDados(equipeId: String, projetoIdRecebido: String?) {
        // Buscar nome da equipe
        buscarNomeEquipe(equipeId)

        // Se já tem projeto ID, não precisa buscar
        if (!projetoIdRecebido.isNullOrEmpty()) {
            _projetoId.value = projetoIdRecebido
            verificarPermissoesDoUsuario(projetoIdRecebido)
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
                verificarPermissoesDoUsuario(projetoId)
            }.onFailure { e ->
                _state.value = EquipeTarefaState.Error("Erro ao buscar projeto: ${e.message}")
            }
        }
    }

    // ==================== VERIFICAÇÃO DE PERMISSÕES ====================

    /**
     * ✅ Verifica se o usuário é criador ou admin do projeto
     */
    private fun verificarPermissoesDoUsuario(projetoId: String) {
        projetoRepository.verificarPermissoes(projetoId) { resultado ->
            resultado.onSuccess { (estaNoProjeto, isCreator, isAdmin) ->
                _isCreator.value = isCreator
                _isAdmin.value = isAdmin
                _podeEditar.value = isCreator || isAdmin
            }.onFailure { e ->
                _isCreator.value = false
                _isAdmin.value = false
                _podeEditar.value = false
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

    // ==================== EDIÇÃO DO NOME (COM VERIFICAÇÃO) ====================

    fun atualizarNomeEquipe(equipeId: String, novoNome: String) {
        // ✅ VERIFICAR PERMISSÃO ANTES DE EDITAR
        if (_podeEditar.value != true) {
            _state.value = EquipeTarefaState.Error("Apenas criadores e administradores podem editar equipes")
            return
        }

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

    // ==================== EXCLUSÃO EM CASCATA (COM VERIFICAÇÃO) ====================

    fun excluirEquipe(equipeId: String) {
        // ✅ VERIFICAR PERMISSÃO ANTES DE EXCLUIR
        if (_podeEditar.value != true) {
            _state.value = EquipeTarefaState.Error("Apenas criadores e administradores podem excluir equipes")
            return
        }

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

    // ==================== HELPER PARA UI ====================

    /**
     * ✅ Método auxiliar para verificar se pode realizar ações de edição
     */
    fun verificarSeUsuarioPodeEditar(): Boolean {
        return _podeEditar.value == true
    }
}