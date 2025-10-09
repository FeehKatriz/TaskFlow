package com.example.taskflow.ui.tarefa.criar

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.taskflow.data.repository.TarefaRepository

class CriarTarefaViewModel : ViewModel() {

    private val repository = TarefaRepository()

    private val _state = MutableLiveData<CriarTarefaState>(CriarTarefaState.Idle)
    val state: LiveData<CriarTarefaState> = _state

    private val _projetoId = MutableLiveData<String?>("")
    val projetoId: LiveData<String?> = _projetoId

    private val _equipeId = MutableLiveData<String?>("")
    val equipeId: LiveData<String?> = _equipeId

    private val _tituloErro = MutableLiveData<String?>()
    val tituloErro: LiveData<String?> = _tituloErro

    private val _descricaoErro = MutableLiveData<String?>()
    val descricaoErro: LiveData<String?> = _descricaoErro

    fun inicializarDados(projetoIdRecebido: String?, equipeIdRecebida: String?) {
        if (!projetoIdRecebido.isNullOrEmpty() && !equipeIdRecebida.isNullOrEmpty()) {
            _projetoId.value = projetoIdRecebido
            _equipeId.value = equipeIdRecebida
            _state.value = CriarTarefaState.DadosCarregados
        } else if (projetoIdRecebido.isNullOrEmpty()) {
            carregarProjetosDoUsuario()
        } else {
            buscarEquipeDoProjeto(projetoIdRecebido)
        }
    }

    private fun carregarProjetosDoUsuario() {
        _state.value = CriarTarefaState.DadosCarregando

        repository.carregarProjetosDoUsuario { resultado ->
            resultado.onSuccess { (projetoId, equipeId) ->
                _projetoId.value = projetoId
                _equipeId.value = equipeId
                _state.value = CriarTarefaState.DadosCarregados
            }.onFailure { e ->
                _state.value = CriarTarefaState.Error(e.message ?: "Erro ao carregar dados")
            }
        }
    }

    private fun buscarEquipeDoProjeto(projetoId: String) {
        _state.value = CriarTarefaState.DadosCarregando

        repository.buscarEquipeDoProjeto(projetoId) { resultado ->
            resultado.onSuccess { equipeId ->
                _projetoId.value = projetoId
                _equipeId.value = equipeId
                _state.value = CriarTarefaState.DadosCarregados
            }.onFailure { e ->
                _state.value = CriarTarefaState.Error(e.message ?: "Erro ao carregar equipe")
            }
        }
    }

    fun validarCampos(titulo: String, descricao: String): Boolean {
        var valido = true

        if (titulo.isBlank() || titulo == "Titulo da Tarefa") {
            _tituloErro.value = "Digite o título da tarefa"
            valido = false
        } else {
            _tituloErro.value = null
        }

        if (descricao.isBlank() || descricao == "Descrição") {
            _descricaoErro.value = "Digite a descrição da tarefa"
            valido = false
        } else {
            _descricaoErro.value = null
        }

        return valido
    }

    fun criarTarefa(titulo: String, descricao: String) {
        if (!validarCampos(titulo, descricao)) {
            return
        }

        if (_state.value != CriarTarefaState.DadosCarregados) {
            _state.value = CriarTarefaState.Error("Aguarde o carregamento dos dados...")
            return
        }

        val pId = _projetoId.value ?: ""
        val eId = _equipeId.value ?: ""

        if (pId.isEmpty() || eId.isEmpty()) {
            _state.value = CriarTarefaState.Error("Erro: Projeto ou equipe não definidos")
            return
        }

        _state.value = CriarTarefaState.Loading

        repository.criarTarefa(titulo, descricao, pId, eId) { resultado ->
            resultado.onSuccess {
                _state.value = CriarTarefaState.Success
            }.onFailure { e ->
                _state.value = CriarTarefaState.Error(e.message ?: "Erro ao criar tarefa")
            }
        }
    }

    fun limparEstado() {
        _state.value = CriarTarefaState.Idle
    }
}