package com.example.taskflow.ui.tarefa

import com.example.taskflow.data.model.Comment

sealed class TarefaState {
    object Idle : TarefaState()
    object Loading : TarefaState()

    data class DadosCarregados(
        val titulo: String,
        val descricao: String,
        val status: String,
        val mostrarMensagem: Boolean = false
    ) : TarefaState()

    data class ArquivosCarregados(val arquivos: List<Arquivo>) : TarefaState()

    data class ArquivoEnviado(val nomeArquivo: String) : TarefaState()

    data class ComentariosCarregados(val comentarios: List<Comment>) : TarefaState()

    data class Error(val message: String) : TarefaState()
}

data class Arquivo(
    val nome: String,
    val mimeType: String,
    val ref: com.google.firebase.storage.StorageReference
)