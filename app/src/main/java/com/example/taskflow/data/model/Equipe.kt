package com.example.taskflow.data.model

data class Equipe(
    val id: String = "",
    val nome: String = "",
    val descricao: String = "",
    val dataVencimento: String = "",
    val progresso: Int = 0,
    val totalTarefas: Int = 0,
    val projetoId: String = "",
    val membros: List<String> = emptyList()
)