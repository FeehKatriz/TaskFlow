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