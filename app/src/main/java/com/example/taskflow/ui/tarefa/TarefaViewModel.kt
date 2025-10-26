package com.example.taskflow.ui.tarefa

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.taskflow.data.model.Comment
import com.example.taskflow.data.repository.TarefaRepository

class TarefaViewModel : ViewModel() {

    private val repository = TarefaRepository()

    private val _state = MutableLiveData<TarefaState>(TarefaState.Idle)
    val state: LiveData<TarefaState> = _state

    private val _titulo = MutableLiveData<String>("")
    val titulo: LiveData<String> = _titulo

    private val _descricao = MutableLiveData<String>("")
    val descricao: LiveData<String> = _descricao

    private val _statusAtual = MutableLiveData<String>("")
    val statusAtual: LiveData<String> = _statusAtual

    private val _prazo = MutableLiveData<String?>()
    val prazo: LiveData<String?> = _prazo

    private val _responsaveis = MutableLiveData<List<String>>(emptyList())
    val responsaveis: LiveData<List<String>> = _responsaveis

    private val _responsaveisNomes = MutableLiveData<List<String>>(emptyList())
    val responsaveisNomes: LiveData<List<String>> = _responsaveisNomes

    private val _prioridade = MutableLiveData<String>("media")
    val prioridade: LiveData<String> = _prioridade

    private val _equipeId = MutableLiveData<String?>()
    val equipeId: LiveData<String?> = _equipeId

    private val _equipeNome = MutableLiveData<String?>()
    val equipeNome: LiveData<String?> = _equipeNome

    // NOVO: LiveData para comentários
    private val _comentarios = MutableLiveData<List<Comment>>(emptyList())
    val comentarios: LiveData<List<Comment>> = _comentarios

    fun inicializarDados(
        tarefaId: String?,
        tarefaTitulo: String?,
        tarefaDescricao: String?,
        tarefaStatus: String?
    ) {
        _titulo.value = tarefaTitulo ?: "Tarefa"
        _descricao.value = if (tarefaDescricao.isNullOrBlank()) {
            "Nenhuma descrição disponível para esta tarefa."
        } else {
            tarefaDescricao
        }
        _statusAtual.value = tarefaStatus ?: "pendente"

        if (tarefaId != null) {
            carregarDadosCompletos(tarefaId)
        }

        _state.value = TarefaState.DadosCarregados(
            titulo = _titulo.value ?: "",
            descricao = _descricao.value ?: "",
            status = _statusAtual.value ?: "",
            mostrarMensagem = false
        )
    }

    private fun carregarDadosCompletos(tarefaId: String) {
        repository.buscarTarefaPorId(tarefaId) { resultado ->
            resultado.onSuccess { tarefa ->
                _titulo.value = tarefa.titulo
                _descricao.value = tarefa.descricao
                _statusAtual.value = tarefa.status
                _prazo.value = tarefa.dataVencimento
                _responsaveis.value = tarefa.responsaveis ?: emptyList()
                _prioridade.value = tarefa.prioridade
                _equipeId.value = tarefa.equipeId

                tarefa.equipeId?.let { eId ->
                    repository.buscarNomeEquipe(eId) { resultEquipe ->
                        resultEquipe.onSuccess { nomeEquipe ->
                            _equipeNome.value = nomeEquipe
                        }
                    }
                }

                if (!tarefa.responsaveis.isNullOrEmpty()) {
                    carregarNomesResponsaveis(tarefa.responsaveis)
                }
            }.onFailure { e ->
                _state.value = TarefaState.Error("Erro ao carregar detalhes: ${e.message}")
            }
        }
    }

    private fun carregarNomesResponsaveis(responsaveisIds: List<String>) {
        repository.buscarNomesUsuarios(responsaveisIds) { resultado ->
            resultado.onSuccess { nomes ->
                _responsaveisNomes.value = nomes
            }.onFailure {
                _responsaveisNomes.value = emptyList()
            }
        }
    }

    fun alterarStatus(tarefaId: String?, novoStatus: String) {
        if (tarefaId == null) {
            _state.value = TarefaState.Error("ID da tarefa não encontrado")
            return
        }

        _statusAtual.value = novoStatus

        repository.alterarStatus(tarefaId, novoStatus) { resultado ->
            resultado.onSuccess {
                _state.value = TarefaState.DadosCarregados(
                    titulo = _titulo.value ?: "",
                    descricao = _descricao.value ?: "",
                    status = novoStatus,
                    mostrarMensagem = true
                )
            }.onFailure { e ->
                _state.value = TarefaState.Error("Erro ao atualizar status no Firebase")
            }
        }
    }

    fun carregarArquivos(tarefaId: String?) {
        if (tarefaId == null) return

        _state.value = TarefaState.Loading

        repository.carregarArquivos(tarefaId) { resultado ->
            resultado.onSuccess { arquivos ->
                _state.value = TarefaState.ArquivosCarregados(arquivos)
            }.onFailure { e ->
                _state.value = TarefaState.Error("Erro ao carregar arquivos")
            }
        }
    }

    fun uploadArquivo(tarefaId: String?, fileUri: android.net.Uri) {
        if (tarefaId == null) {
            _state.value = TarefaState.Error("ID da tarefa não encontrado")
            return
        }

        repository.uploadArquivo(tarefaId, fileUri) { resultado ->
            resultado.onSuccess {
                carregarArquivos(tarefaId)
            }.onFailure { e ->
                _state.value = TarefaState.Error("Erro ao enviar arquivo")
            }
        }
    }

    fun obterDownloadUrl(
        fileRef: com.google.firebase.storage.StorageReference,
        callback: (Result<android.net.Uri>) -> Unit
    ) {
        repository.obterDownloadUrlArquivo(fileRef, callback)
    }

    // ==================== COMENTÁRIOS (NOVO) ====================

    fun carregarComentarios(tarefaId: String?) {
        if (tarefaId == null) return

        repository.carregarComentarios(tarefaId) { resultado ->
            resultado.onSuccess { comentarios ->
                _comentarios.value = comentarios
            }.onFailure { e ->
                _state.value = TarefaState.Error("Erro ao carregar comentários: ${e.message}")
            }
        }
    }

    fun adicionarComentario(tarefaId: String?, mensagem: String) {
        if (tarefaId == null) {
            _state.value = TarefaState.Error("ID da tarefa não encontrado")
            return
        }

        if (mensagem.isBlank()) {
            _state.value = TarefaState.Error("Mensagem não pode estar vazia")
            return
        }

        repository.adicionarComentario(tarefaId, mensagem) { resultado ->
            resultado.onSuccess {
                // Comentários serão atualizados automaticamente pelo listener
            }.onFailure { e ->
                _state.value = TarefaState.Error("Erro ao adicionar comentário: ${e.message}")
            }
        }
    }

    fun deletarComentario(tarefaId: String?, commentId: String) {
        if (tarefaId == null) return

        repository.deletarComentario(tarefaId, commentId) { resultado ->
            resultado.onFailure { e ->
                _state.value = TarefaState.Error("Erro ao deletar comentário: ${e.message}")
            }
        }
    }

    fun traduzirStatus(status: String?): String {
        return when (status) {
            "pendente" -> "Pendente"
            "em_andamento" -> "Em andamento"
            "concluida" -> "Concluída"
            else -> "Status desconhecido"
        }
    }

    fun limparEstado() {
        _state.value = TarefaState.Idle
    }
}