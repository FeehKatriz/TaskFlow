package com.example.taskflow.ui.projeto

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.taskflow.data.model.Equipe
import com.example.taskflow.data.repository.ProjetoRepository
import kotlin.random.Random

class ProjetoViewModel : ViewModel() {

    private val repository = ProjetoRepository()

    private val _state = MutableLiveData<ProjetoState>(ProjetoState.Idle)
    val state: LiveData<ProjetoState> = _state

    private val _nomeProjeto = MutableLiveData<String>("")
    val nomeProjeto: LiveData<String> = _nomeProjeto

    private val _codigoProjeto = MutableLiveData<String?>("")
    val codigoProjeto: LiveData<String?> = _codigoProjeto

    private val _isCreator = MutableLiveData<Boolean>(false)
    val isCreator: LiveData<Boolean> = _isCreator

    private val _corProjeto = MutableLiveData<String>("#4285F4")
    val corProjeto: LiveData<String> = _corProjeto

    fun carregarInfoProjeto(projetoId: String) {
        repository.carregarInfoProjeto(projetoId) { resultado ->
            resultado.onSuccess { (nome, codigo, isCreator) ->
                _nomeProjeto.value = nome
                _codigoProjeto.value = codigo
                _isCreator.value = isCreator
                _state.value = ProjetoState.InfoCarregada(nome, codigo, isCreator)
            }.onFailure { e ->
                _state.value = ProjetoState.Error(e.message ?: "Erro ao carregar informações")
            }
        }
    }

    fun carregarEquipes(projetoId: String) {
        _state.value = ProjetoState.Loading

        repository.carregarEquipes(projetoId) { resultado ->
            resultado.onSuccess { equipes ->
                _state.value = ProjetoState.EquipesCarregadas(equipes)
            }.onFailure { e ->
                _state.value = ProjetoState.Error(e.message ?: "Erro ao carregar equipes")
            }
        }
    }

    fun carregarMembros(projetoId: String) {
        _state.value = ProjetoState.Loading

        repository.carregarMembros(projetoId) { resultado ->
            resultado.onSuccess { membros ->
                _state.value = ProjetoState.MembrosCarregados(membros)
            }.onFailure { e ->
                _state.value = ProjetoState.Error(e.message ?: "Erro ao carregar membros")
            }
        }
    }

    fun verificarSeUsuarioEstaNoProjeto(projetoId: String, callback: (Boolean) -> Unit) {
        repository.verificarSeUsuarioEstaNoProjeto(projetoId) { resultado ->
            resultado.onSuccess { estaNoProjeto ->
                callback(estaNoProjeto)
            }.onFailure {
                callback(false)
            }
        }
    }

    fun atualizarCodigo(projetoId: String) {
        gerarCodigoUnico { novoCodigo ->
            if (novoCodigo != null) {
                repository.atualizarCodigoProjeto(projetoId, novoCodigo) { resultado ->
                    resultado.onSuccess {
                        _codigoProjeto.value = novoCodigo
                        _state.value = ProjetoState.InfoCarregada(
                            _nomeProjeto.value ?: "",
                            novoCodigo,
                            _isCreator.value ?: false
                        )
                    }.onFailure { e ->
                        _state.value = ProjetoState.Error(e.message ?: "Erro ao atualizar código")
                    }
                }
            } else {
                _state.value = ProjetoState.Error("Erro ao gerar novo código")
            }
        }
    }

    // ==================== EDIÇÃO DE CAMPOS ====================

    fun atualizarNomeProjeto(projetoId: String, novoNome: String) {
        if (novoNome.isBlank()) {
            _state.value = ProjetoState.Error("Nome não pode estar vazio")
            return
        }

        _state.value = ProjetoState.Loading

        repository.atualizarNomeProjeto(projetoId, novoNome) { resultado ->
            resultado.onSuccess {
                _nomeProjeto.value = novoNome
                _state.value = ProjetoState.CampoAtualizado("Nome atualizado com sucesso")
            }.onFailure { e ->
                _state.value = ProjetoState.Error("Erro ao atualizar nome: ${e.message}")
            }
        }
    }

    fun atualizarCorProjeto(projetoId: String, novaCor: String) {
        _state.value = ProjetoState.Loading

        repository.atualizarCorProjeto(projetoId, novaCor) { resultado ->
            resultado.onSuccess {
                _corProjeto.value = novaCor
                _state.value = ProjetoState.CampoAtualizado("Cor atualizada com sucesso")
            }.onFailure { e ->
                _state.value = ProjetoState.Error("Erro ao atualizar cor: ${e.message}")
            }
        }
    }

    // ==================== UTILITÁRIOS ====================

    private fun gerarCodigoProjeto(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..10)
            .map { chars[Random.Default.nextInt(chars.length)] }
            .joinToString("")
    }

    private fun verificarCodigoUnico(codigo: String, callback: (Boolean) -> Unit) {
        repository.verificarCodigoUnico(codigo) { isUnico ->
            callback(isUnico)
        }
    }

    private fun gerarCodigoUnico(callback: (String?) -> Unit) {
        val codigo = gerarCodigoProjeto()

        verificarCodigoUnico(codigo) { isUnico ->
            if (isUnico) {
                callback(codigo)
            } else {
                gerarCodigoUnico(callback)
            }
        }
    }

    fun limparEstado() {
        _state.value = ProjetoState.Idle
    }
}