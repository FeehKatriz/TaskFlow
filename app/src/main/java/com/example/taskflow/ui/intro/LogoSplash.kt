package com.example.taskflow.ui.intro

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.taskflow.R
import com.example.taskflow.ui.entrar.EntrarViewModel
import com.example.taskflow.ui.main.MainActivity

class LogoSplash : AppCompatActivity() {

    private val viewModel: EntrarViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logo_splash)

        // Aguarda 2 segundos e verifica autenticação
        Handler(Looper.getMainLooper()).postDelayed({
            verificarAutenticacao()
        }, 2000) // 2000 milissegundos = 2 segundos
    }

    private fun verificarAutenticacao() {
        if (viewModel.verificarUsuarioLogado()) {
            // Usuário já está logado, vai direto para MainActivity
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            // Usuário não está logado, vai para tela de introdução
            startActivity(Intent(this, IntroActivity::class.java))
        }
        finish()
    }
}