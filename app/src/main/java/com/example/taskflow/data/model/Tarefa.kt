package com.example.taskflow.data.model

import com.google.firebase.Timestamp

data class Tarefa(
    var id: String = "",
    val titulo: String = "",
    val descricao: String = "",
    val projetoId: String = "",
    val equipeId: String = "",
    val equipeNome: String = "", // Nome da equipe (para exibição)
    val criadoPor: String = "", // ID do usuário que criou
    val atualizadoPor: String = "", // ID do último usuário que atualizou
    val status: String = "pendente", // pendente, em_andamento, concluida
    val prioridade: String = "media", // baixa, media, alta
    val dataVencimento: String = "",
    val dataCriacao: Long = System.currentTimeMillis(),
    val atualizadoEm: Timestamp? = null,
    val anexos: List<String> = emptyList(), // URLs dos anexos
    val responsaveis: List<String> = emptyList() // IDs dos usuários responsáveis
)