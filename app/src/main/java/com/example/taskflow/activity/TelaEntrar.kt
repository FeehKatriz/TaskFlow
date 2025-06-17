package com.example.taskflow.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.taskflow.R
import com.example.taskflow.databinding.ActivityTelaEntrarBinding

class TelaEntrar : AppCompatActivity() {

    private val binding by lazy {
        ActivityTelaEntrarBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(binding.root)

        binding.btnEntrarLogin.setOnClickListener {
            startActivity(Intent(this, TelaInicial::class.java))
        }
        binding.btnSemConta.setOnClickListener {
            startActivity(Intent(this, TelaCadastro::class.java))
        }
        binding.btnEsqueceuSenha.setOnClickListener {
            startActivity(Intent(this, tela_esqueci_minha_senha::class.java))
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.teste)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}