package com.example.taskflow.ui.projeto

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.taskflow.data.model.Equipe
import com.example.taskflow.data.repository.ProjetoRepository
import com.google.firebase.firestore.ListenerRegistration
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

    private val _isAdmin = MutableLiveData<Boolean>(false)
    val isAdmin: LiveData<Boolean> = _isAdmin

    // ✅ NOVO: LiveData para verificar se usuário ainda está no projeto
    private val _estaNoProjeto = MutableLiveData<Boolean>(true)
    val estaNoProjeto: LiveData<Boolean> = _estaNoProjeto

    private val _corProjeto = MutableLiveData<String>("#4285F4")
    val corProjeto: LiveData<String> = _corProjeto

    // ✅ NOVO: Listener para monitoramento em tempo real
    private var permissoesListener: ListenerRegistration? = null

    // ==================== MONITORAMENTO EM TEMPO REAL ====================

    /**
     * ✅ NOVO: Inicia monitoramento em tempo real das permissões do usuário
     * Detecta quando:
     * - Usuário é removido do projeto
     * - Usuário perde permissões de admin
     * - Usuário ganha permissões de admin
     */
    fun iniciarMonitoramentoPermissoes(projetoId: String) {
        permissoesListener = repository.monitorarPermissoes(projetoId) { resultado ->
            resultado.onSuccess { (estaNoProjeto, isCreator, isAdmin) ->
                val estaNoProjetoAntes = _estaNoProjeto.value ?: true
                val eraAdmin = _isAdmin.value ?: false

                // Atualizar valores
                _estaNoProjeto.value = estaNoProjeto
                _isCreator.value = isCreator
                _isAdmin.value = isAdmin

                // ✅ NOTIFICAR: Usuário foi removido do projeto
                if (estaNoProjetoAntes && !estaNoProjeto) {
                    _state.value = ProjetoState.UsuarioRemovidoDoProjeto
                }

                // ✅ NOTIFICAR: Usuário perdeu permissões de admin
                if (estaNoProjetoAntes && estaNoProjeto && eraAdmin && !isAdmin && !isCreator) {
                    _state.value = ProjetoState.PermissoesRevogadas
                }
            }.onFailure { e ->
                _state.value = ProjetoState.Error("Erro ao monitorar permissões: ${e.message}")
            }
        }
    }

    /**
     * ✅ NOVO: Para o monitoramento quando o Fragment é destruído
     */
    fun pararMonitoramentoPermissoes(projetoId: String) {
        permissoesListener?.remove()
        permissoesListener = null
    }

    /**
     * ✅ NOVO: Recarrega permissões manualmente (usado no onResume)
     */
    fun recarregarPermissoes(projetoId: String) {
        repository.verificarPermissoes(projetoId) { resultado ->
            resultado.onSuccess { (estaNoProjeto, isCreator, isAdmin) ->
                val estaNoProjetoAntes = _estaNoProjeto.value ?: true

                _estaNoProjeto.value = estaNoProjeto
                _isCreator.value = isCreator
                _isAdmin.value = isAdmin

                // Se foi removido, notificar
                if (estaNoProjetoAntes && !estaNoProjeto) {
                    _state.value = ProjetoState.UsuarioRemovidoDoProjeto
                }
            }.onFailure { e ->
                _state.value = ProjetoState.Error("Erro ao verificar permissões: ${e.message}")
            }
        }
    }

    // ==================== CARREGAMENTO DE DADOS ====================

    fun carregarInfoProjeto(projetoId: String) {
        repository.carregarInfoProjeto(projetoId) { resultado ->
            resultado.onSuccess { (nome, codigo, isCreator, isAdmin) ->
                _nomeProjeto.value = nome
                _codigoProjeto.value = codigo
                _isCreator.value = isCreator
                _isAdmin.value = isAdmin
                _state.value = ProjetoState.InfoCarregada(nome, codigo, isCreator, isAdmin)
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
            resultado.onSuccess { (membros, criadorId, adminsIds) ->
                _state.value = ProjetoState.MembrosCarregados(membros, criadorId, adminsIds)
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

    // ==================== ATUALIZAÇÃO DE CÓDIGO ====================

    fun atualizarCodigo(projetoId: String) {
        // ✅ NOVO: Verificar permissões antes de executar
        if (_isCreator.value != true && _isAdmin.value != true) {
            _state.value = ProjetoState.Error("Você não tem permissão para gerar novo código")
            return
        }

        gerarCodigoUnico { novoCodigo ->
            if (novoCodigo != null) {
                repository.atualizarCodigoProjeto(projetoId, novoCodigo) { resultado ->
                    resultado.onSuccess {
                        _codigoProjeto.value = novoCodigo
                        _state.value = ProjetoState.InfoCarregada(
                            _nomeProjeto.value ?: "",
                            novoCodigo,
                            _isCreator.value ?: false,
                            _isAdmin.value ?: false
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

        // ✅ NOVO: Verificar permissões antes de executar
        if (_isCreator.value != true && _isAdmin.value != true) {
            _state.value = ProjetoState.Error("Você não tem permissão para editar o nome")
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
        // ✅ NOVO: Verificar permissões antes de executar
        if (_isCreator.value != true && _isAdmin.value != true) {
            _state.value = ProjetoState.Error("Você não tem permissão para editar a cor")
            return
        }

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

    // ==================== GERENCIAMENTO DE ADMINS ====================

    fun promoverParaAdmin(projetoId: String, userId: String) {
        if (_isCreator.value != true) {
            _state.value = ProjetoState.Error("Apenas o criador pode promover administradores")
            return
        }

        _state.value = ProjetoState.Loading

        repository.promoverParaAdmin(projetoId, userId) { resultado ->
            resultado.onSuccess {
                _state.value = ProjetoState.CampoAtualizado("Membro promovido a administrador")
                carregarMembros(projetoId)
            }.onFailure { e ->
                _state.value = ProjetoState.Error("Erro ao promover: ${e.message}")
            }
        }
    }

    fun removerAdmin(projetoId: String, userId: String) {
        if (_isCreator.value != true) {
            _state.value = ProjetoState.Error("Apenas o criador pode remover administradores")
            return
        }

        _state.value = ProjetoState.Loading

        repository.removerAdmin(projetoId, userId) { resultado ->
            resultado.onSuccess {
                _state.value = ProjetoState.CampoAtualizado("Administrador rebaixado a membro")
                carregarMembros(projetoId)
            }.onFailure { e ->
                _state.value = ProjetoState.Error("Erro ao remover admin: ${e.message}")
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

    // ✅ NOVO: Limpar listener ao destruir ViewModel
    override fun onCleared() {
        super.onCleared()
        permissoesListener?.remove()
    }
}