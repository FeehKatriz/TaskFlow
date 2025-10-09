package com.example.taskflow.ui.tarefa

sealed class TarefaState {
    object Idle : TarefaState()
    object Loading : TarefaState()

    data class DadosCarregados(
        val titulo: String,
        val descricao: String,
        val status: String,
        val mostrarMensagem: Boolean = false  // Flag para controlar exibição do Toast
    ) : TarefaState()

    data class ArquivosCarregados(val arquivos: List<Arquivo>) : TarefaState()
    data class Error(val message: String) : TarefaState()
}

data class Arquivo(
    val nome: String,
    val mimeType: String,
    val ref: com.google.firebase.storage.StorageReference
)