package com.example.taskflow.data.model

data class Projeto(
    var id: String = "",
    val nome: String = "",
    val criador: String = "",
    val membros: List<String> = listOf(),
    val cor: String = "#3F51B5",
    val dataCriacao: Long = System.currentTimeMillis(),
    val codigo: String = ""
)