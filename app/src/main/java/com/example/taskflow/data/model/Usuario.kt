package com.example.taskflow.data.model

import com.google.firebase.firestore.PropertyName
import java.util.Date

data class Usuario(
    @get:PropertyName("nome") @set:PropertyName("nome")
    var nome: String = "",

    @get:PropertyName("email") @set:PropertyName("email")
    var email: String = "",

    @get:PropertyName("nickname") @set:PropertyName("nickname")
    var nickname: String = "",

    @get:PropertyName("fotoUrl") @set:PropertyName("fotoUrl")
    var fotoUrl: String = "",

    @get:PropertyName("dataCriacao") @set:PropertyName("dataCriacao")
    var dataCriacao: Date? = null
) {
    constructor() : this("", "", "", "", null)
}