package com.example.taskflow.models

data class Equipe(
    val nome: String = "",
    val criador: String = "",
    val membros: List<String> = listOf(),
    val cor: String = "#3F51B5",
    val dataCriacao: Long = System.currentTimeMillis()
)