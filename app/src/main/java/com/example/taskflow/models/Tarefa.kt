package com.example.taskflow.models

data class Tarefa(
    var id: String = "",
    val titulo: String = "",
    val descricao: String = "",
    val projetoId: String = "",
    val equipeId: String = "",
    val criadoPor: String = "", // ID do usuário que criou
    val status: String = "pendente", // pendente, em_andamento, concluida
    val prioridade: String = "media", // baixa, media, alta
    val dataVencimento: String = "",
    val dataCriacao: Long = System.currentTimeMillis(),
    val anexos: List<String> = emptyList() // URLs dos anexos
)