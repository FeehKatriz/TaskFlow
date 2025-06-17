package com.example.taskflow.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.taskflow.R

class LogoSplash : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logo_splash)

        // Aguarda 2 segundos e vai para a tela de boas-vindas
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this,  IntroActivity::class.java)
            startActivity(intent)
            finish()
        }, 2000) // 2000 milissegundos = 2 segundos
    }
}