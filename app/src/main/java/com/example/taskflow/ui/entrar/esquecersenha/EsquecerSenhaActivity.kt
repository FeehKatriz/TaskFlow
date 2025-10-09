package com.example.taskflow.ui.entrar.esquecersenha

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.taskflow.R
import com.example.taskflow.databinding.ActivityEsquecerSenhaBinding
import com.example.taskflow.ui.entrar.EntrarActivity

class EsquecerSenhaActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityEsquecerSenhaBinding.inflate(layoutInflater)
    }

    private val viewModel: EsquecerSenhaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        configurarListeners()
        observarEstado()
        observarErros()
    }

    private fun configurarListeners() {
        binding.btnvoltarLogin.setOnClickListener {
            startActivity(Intent(this, EntrarActivity::class.java))
            finish()
        }

        binding.btnRecupearSenha.setOnClickListener {
            val email = binding.emailRecuperacao.text.toString()
            viewModel.enviarEmailRecuperacao(email)
        }
    }

    private fun observarEstado() {
        viewModel.state.observe(this) { state ->
            when (state) {
                is EsquecerSenhaState.Idle -> {
                    habilitarBotao()
                }
                is EsquecerSenhaState.Loading -> {
                    desabilitarBotao()
                }
                is EsquecerSenhaState.Success -> {
                    habilitarBotao()
                    Toast.makeText(
                        this,
                        "Email de recuperação enviado!",
                        Toast.LENGTH_LONG
                    ).show()
                    startActivity(Intent(this, EntrarActivity::class.java))
                    finish()
                }
                is EsquecerSenhaState.Error -> {
                    habilitarBotao()
                    Toast.makeText(
                        this,
                        state.message,
                        Toast.LENGTH_LONG
                    ).show()
                    viewModel.limparEstado()
                }
            }
        }
    }

    private fun observarErros() {
        viewModel.emailErro.observe(this) { erro ->
            binding.textInputLayoutEmail.error = erro
        }
    }

    private fun desabilitarBotao() {
        binding.btnRecupearSenha.isEnabled = false
        binding.btnRecupearSenha.text = "Enviando..."
    }

    private fun habilitarBotao() {
        binding.btnRecupearSenha.isEnabled = true
        binding.btnRecupearSenha.text = "RECUPERAR SENHA"
    }
}