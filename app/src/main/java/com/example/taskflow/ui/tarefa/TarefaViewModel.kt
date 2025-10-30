package com.example.taskflow.ui.tarefa

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.taskflow.data.model.Comment
import com.example.taskflow.data.repository.TarefaRepository
import com.example.taskflow.data.repository.ComentarioRepository
import com.example.taskflow.data.repository.ArquivoRepository

class TarefaViewModel : ViewModel() {

    private val tarefaRepository = TarefaRepository()
    private val comentarioRepository = ComentarioRepository()
    private val arquivoRepository = ArquivoRepository()

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
        tarefaRepository.buscarTarefaPorId(tarefaId) { resultado ->
            resultado.onSuccess { tarefa ->
                _titulo.value = tarefa.titulo
                _descricao.value = tarefa.descricao
                _statusAtual.value = tarefa.status
                _prazo.value = tarefa.dataVencimento
                _responsaveis.value = tarefa.responsaveis ?: emptyList()
                _prioridade.value = tarefa.prioridade
                _equipeId.value = tarefa.equipeId

                tarefa.equipeId?.let { eId ->
                    tarefaRepository.buscarNomeEquipe(eId) { resultEquipe ->
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
        tarefaRepository.buscarNomesUsuarios(responsaveisIds) { resultado ->
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

        val equipeIdAtual = _equipeId.value
        if (equipeIdAtual == null) {
            _state.value = TarefaState.Error("Equipe não identificada")
            return
        }

        tarefaRepository.verificarSeUsuarioEstaEquipe(equipeIdAtual) { resultado ->
            resultado.onSuccess { isMembro ->
                if (isMembro) {
                    _statusAtual.value = novoStatus

                    tarefaRepository.alterarStatus(tarefaId, novoStatus) { resultAlteracao ->
                        resultAlteracao.onSuccess {
                            _state.value = TarefaState.DadosCarregados(
                                titulo = _titulo.value ?: "",
                                descricao = _descricao.value ?: "",
                                status = novoStatus,
                                mostrarMensagem = true
                            )
                        }.onFailure { e ->
                            _state.value = TarefaState.Error("Erro ao atualizar status: ${e.message}")
                        }
                    }
                } else {
                    _state.value = TarefaState.Error("Você não tem permissão para alterar esta tarefa. Apenas membros da equipe podem fazer alterações.")
                }
            }.onFailure { e ->
                _state.value = TarefaState.Error("Erro ao verificar permissões: ${e.message}")
            }
        }
    }

    // ==================== ARQUIVOS ====================

    fun carregarArquivos(tarefaId: String?) {
        if (tarefaId == null) return

        _state.value = TarefaState.Loading

        arquivoRepository.carregarArquivos(tarefaId) { resultado ->
            resultado.onSuccess { arquivos ->
                _state.value = TarefaState.ArquivosCarregados(arquivos)
            }.onFailure { e ->
                _state.value = TarefaState.Error("Erro ao carregar arquivos: ${e.message}")
            }
        }
    }

    fun uploadArquivo(tarefaId: String?, fileUri: android.net.Uri) {
        if (tarefaId == null) {
            _state.value = TarefaState.Error("ID da tarefa não encontrado")
            return
        }

        _state.value = TarefaState.Loading

        arquivoRepository.uploadArquivo(tarefaId, fileUri) { resultado ->
            resultado.onSuccess { nomeArquivo ->
                _state.value = TarefaState.ArquivoEnviado(nomeArquivo)
                carregarArquivos(tarefaId)
            }.onFailure { e ->
                _state.value = TarefaState.Error("Erro ao enviar arquivo: ${e.message}")
            }
        }
    }

    fun obterDownloadUrl(
        fileRef: com.google.firebase.storage.StorageReference,
        callback: (Result<android.net.Uri>) -> Unit
    ) {
        arquivoRepository.obterDownloadUrl(fileRef, callback)
    }

    fun deletarArquivo(tarefaId: String?, nomeArquivo: String) {
        if (tarefaId == null) return

        _state.value = TarefaState.Loading

        arquivoRepository.deletarArquivo(tarefaId, nomeArquivo) { resultado ->
            resultado.onSuccess {
                carregarArquivos(tarefaId)
            }.onFailure { e ->
                _state.value = TarefaState.Error("Erro ao deletar arquivo: ${e.message}")
            }
        }
    }

    // ==================== COMENTÁRIOS ====================

    fun carregarComentarios(tarefaId: String?) {
        if (tarefaId == null) return

        comentarioRepository.carregarComentarios(tarefaId) { resultado ->
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

        comentarioRepository.adicionarComentario(tarefaId, mensagem) { resultado ->
            resultado.onSuccess {
                // Comentários serão atualizados automaticamente pelo listener
            }.onFailure { e ->
                _state.value = TarefaState.Error("Erro ao adicionar comentário: ${e.message}")
            }
        }
    }

    fun deletarComentario(tarefaId: String?, commentId: String) {
        if (tarefaId == null) return

        comentarioRepository.deletarComentario(tarefaId, commentId) { resultado ->
            resultado.onFailure { e ->
                _state.value = TarefaState.Error("Erro ao deletar comentário: ${e.message}")
            }
        }
    }

    fun editarComentario(tarefaId: String?, commentId: String, novaMensagem: String) {
        if (tarefaId == null) return

        if (novaMensagem.isBlank()) {
            _state.value = TarefaState.Error("Mensagem não pode estar vazia")
            return
        }

        comentarioRepository.editarComentario(tarefaId, commentId, novaMensagem) { resultado ->
            resultado.onFailure { e ->
                _state.value = TarefaState.Error("Erro ao editar comentário: ${e.message}")
            }
        }
    }

    // ==================== EDIÇÃO DE CAMPOS (NOVO) ====================

    fun atualizarTitulo(tarefaId: String?, novoTitulo: String) {
        if (tarefaId == null) {
            _state.value = TarefaState.Error("ID da tarefa não encontrado")
            return
        }

        if (novoTitulo.isBlank()) {
            _state.value = TarefaState.Error("Título não pode estar vazio")
            return
        }

        _state.value = TarefaState.Loading

        tarefaRepository.atualizarTitulo(tarefaId, novoTitulo) { resultado ->
            resultado.onSuccess {
                _titulo.value = novoTitulo
                _state.value = TarefaState.CampoAtualizado("Título atualizado com sucesso")
            }.onFailure { e ->
                _state.value = TarefaState.Error("Erro ao atualizar título: ${e.message}")
            }
        }
    }

    fun atualizarDescricao(tarefaId: String?, novaDescricao: String) {
        if (tarefaId == null) {
            _state.value = TarefaState.Error("ID da tarefa não encontrado")
            return
        }

        _state.value = TarefaState.Loading

        tarefaRepository.atualizarDescricao(tarefaId, novaDescricao) { resultado ->
            resultado.onSuccess {
                _descricao.value = novaDescricao
                _state.value = TarefaState.CampoAtualizado("Descrição atualizada com sucesso")
            }.onFailure { e ->
                _state.value = TarefaState.Error("Erro ao atualizar descrição: ${e.message}")
            }
        }
    }

    fun atualizarPrazo(tarefaId: String?, novoPrazo: String?) {
        if (tarefaId == null) {
            _state.value = TarefaState.Error("ID da tarefa não encontrado")
            return
        }

        _state.value = TarefaState.Loading

        tarefaRepository.atualizarPrazo(tarefaId, novoPrazo) { resultado ->
            resultado.onSuccess {
                _prazo.value = novoPrazo
                _state.value = TarefaState.CampoAtualizado("Prazo atualizado com sucesso")
            }.onFailure { e ->
                _state.value = TarefaState.Error("Erro ao atualizar prazo: ${e.message}")
            }
        }
    }

    fun atualizarPrioridade(tarefaId: String?, novaPrioridade: String) {
        if (tarefaId == null) {
            _state.value = TarefaState.Error("ID da tarefa não encontrado")
            return
        }

        _state.value = TarefaState.Loading

        tarefaRepository.atualizarPrioridade(tarefaId, novaPrioridade) { resultado ->
            resultado.onSuccess {
                _prioridade.value = novaPrioridade
                _state.value = TarefaState.CampoAtualizado("Prioridade atualizada com sucesso")
            }.onFailure { e ->
                _state.value = TarefaState.Error("Erro ao atualizar prioridade: ${e.message}")
            }
        }
    }

    fun atualizarResponsaveis(tarefaId: String?, novosResponsaveis: List<String>) {
        if (tarefaId == null) {
            _state.value = TarefaState.Error("ID da tarefa não encontrado")
            return
        }

        _state.value = TarefaState.Loading

        tarefaRepository.atualizarResponsaveis(tarefaId, novosResponsaveis) { resultado ->
            resultado.onSuccess {
                _responsaveis.value = novosResponsaveis

                // Atualizar nomes dos responsáveis
                if (novosResponsaveis.isNotEmpty()) {
                    carregarNomesResponsaveis(novosResponsaveis)
                } else {
                    _responsaveisNomes.value = emptyList()
                }

                _state.value = TarefaState.CampoAtualizado("Responsáveis atualizados com sucesso")
            }.onFailure { e ->
                _state.value = TarefaState.Error("Erro ao atualizar responsáveis: ${e.message}")
            }
        }
    }

    fun carregarMembrosEquipe() {
        val equipeIdAtual = _equipeId.value
        if (equipeIdAtual == null) {
            _state.value = TarefaState.Error("Equipe não identificada")
            return
        }

        _state.value = TarefaState.Loading

        tarefaRepository.carregarMembrosEquipe(equipeIdAtual) { resultado ->
            resultado.onSuccess { membros ->
                _state.value = TarefaState.MembrosEquipeCarregados(membros)
            }.onFailure { e ->
                _state.value = TarefaState.Error("Erro ao carregar membros: ${e.message}")
            }
        }
    }

    // ==================== EXCLUSÃO (NOVO) ====================

    fun excluirTarefa(tarefaId: String?) {
        if (tarefaId == null) {
            _state.value = TarefaState.Error("ID da tarefa não encontrado")
            return
        }

        val equipeIdAtual = _equipeId.value
        if (equipeIdAtual == null) {
            _state.value = TarefaState.Error("Equipe não identificada")
            return
        }

        _state.value = TarefaState.Loading

        tarefaRepository.excluirTarefa(tarefaId, equipeIdAtual) { resultado ->
            resultado.onSuccess {
                _state.value = TarefaState.TarefaExcluida
            }.onFailure { e ->
                _state.value = TarefaState.Error("Erro ao excluir tarefa: ${e.message}")
            }
        }
    }

    // ==================== UTILITÁRIOS ====================

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