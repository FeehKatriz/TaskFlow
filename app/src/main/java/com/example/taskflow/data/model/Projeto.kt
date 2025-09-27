package com.example.taskflow.data.model

data class Projeto(
    var id: String = "",
    val nome: String = "",
    val descricao: String = "",
    val dataVencimento: String = "",
    val progresso: Int = 0,
    val totalTarefas: Int = 0,
    val criador: String ="",
    val cor: String = "",
    val equipeId: String = "",
    val membros: List<String> = emptyList()
)