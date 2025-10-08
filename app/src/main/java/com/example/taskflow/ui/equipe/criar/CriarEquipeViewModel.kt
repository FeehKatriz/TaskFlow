package com.example.taskflow.ui.equipe.criar

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.taskflow.data.model.Equipe
import com.example.taskflow.data.repository.EquipeRepository

class CriarEquipeViewModel : ViewModel() {

    private val repository = EquipeRepository()

    private val _state = MutableLiveData<CriarEquipeState>(CriarEquipeState.Idle)
    val state: LiveData<CriarEquipeState> = _state

    private val _projetoId = MutableLiveData<String?>()
    val projetoId: LiveData<String?> = _projetoId

    private val _dataFormatada = MutableLiveData<String>("")
    val dataFormatada: LiveData<String> = _dataFormatada

    // Inicializar com o projeto recebido ou buscar o primeiro
    fun inicializarProjeto(projetoIdRecebido: String?) {
        if (!projetoIdRecebido.isNullOrEmpty()) {
            _projetoId.value = projetoIdRecebido
        } else {
            carregarPrimeiroProjeto()
        }
    }

    fun setData(dia: Int, mes: Int, ano: Int) {
        val dataFormatada = String.format("%02d/%02d/%d", dia, mes + 1, ano)
        _dataFormatada.value = dataFormatada
    }

    private fun carregarPrimeiroProjeto() {
        repository.carregarPrimeiroProjeto { resultado ->
            resultado.onSuccess { projetoId ->
                _projetoId.value = projetoId
            }.onFailure { e ->
                _state.value = CriarEquipeState.Error(e.message ?: "Erro ao carregar projetos")
            }
        }
    }

    fun validarCampos(nomeEquipe: String, prazo: String): String? {
        return when {
            nomeEquipe.isBlank() || nomeEquipe == "Nome da Equipe" ->
                "Digite o nome da equipe"
            prazo.isBlank() || prazo == "Prazo" ->
                "Selecione o prazo da equipe"
            _projetoId.value.isNullOrEmpty() ->
                "Erro: Projeto não selecionado"
            else -> null
        }
    }

    fun criarEquipe(nomeEquipe: String, prazo: String) {
        val erro = validarCampos(nomeEquipe, prazo)
        if (erro != null) {
            _state.value = CriarEquipeState.Error(erro)
            return
        }

        _state.value = CriarEquipeState.Loading

        val equipe = Equipe(
            nome = nomeEquipe,
            descricao = "",
            dataVencimento = prazo,
            progresso = 0,
            totalTarefas = 0,
            projetoId = _projetoId.value ?: ""
        )

        repository.criarEquipe(equipe) { resultado ->
            resultado.onSuccess {
                _state.value = CriarEquipeState.Success
            }.onFailure { e ->
                _state.value = CriarEquipeState.Error("Erro ao criar equipe: ${e.message}")
            }
        }
    }

    fun limparEstado() {
        _state.value = CriarEquipeState.Idle
    }
}