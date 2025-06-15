package com.example.taskflow.models

data class Projeto(
    val id: String = "",
    val nome: String = "",
    val descricao: String = "",
    val dataVencimento: String = "",
    val progresso: Int = 0,
    val totalTarefas: Int = 0,
    val equipeId: String = ""
)