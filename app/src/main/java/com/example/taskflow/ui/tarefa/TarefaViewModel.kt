package com.example.taskflow.ui.tarefa

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
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

        // Carrega os dados iniciais SEM mostrar mensagem
        _state.value = TarefaState.DadosCarregados(
            titulo = _titulo.value ?: "",
            descricao = _descricao.value ?: "",
            status = _statusAtual.value ?: "",
            mostrarMensagem = false  // NÃO mostra Toast na inicialização
        )
    }

    fun alterarStatus(tarefaId: String?, novoStatus: String) {
        if (tarefaId == null) {
            _state.value = TarefaState.Error("ID da tarefa não encontrado")
            return
        }

        _statusAtual.value = novoStatus

        repository.alterarStatus(tarefaId, novoStatus) { resultado ->
            resultado.onSuccess {
                // Emite estado COM flag para mostrar mensagem
                _state.value = TarefaState.DadosCarregados(
                    titulo = _titulo.value ?: "",
                    descricao = _descricao.value ?: "",
                    status = novoStatus,
                    mostrarMensagem = true  // MOSTRA Toast quando altera
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