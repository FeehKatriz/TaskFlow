package com.example.taskflow.ui.cadastrar

import androidx.lifecycle.ViewModel

class CadastrarViewModel : ViewModel() {

    fun validarCampos(
        nome: String,
        email: String,
        nickname: String,
        senha: String,
        confirmaSenha: String
    ): String? {
        return when {
            nome.isBlank() || email.isBlank() || nickname.isBlank() || senha.isBlank() || confirmaSenha.isBlank() ->
                "Preencha todos os campos"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                "Email inválido"
            nickname.length < 3 ->
                "Nickname muito curto"
            senha.length < 6 ->
                "Senha muito curta"
            senha != confirmaSenha ->
                "Senhas não coincidem"
            else -> null // tudo certo
        }
    }



}