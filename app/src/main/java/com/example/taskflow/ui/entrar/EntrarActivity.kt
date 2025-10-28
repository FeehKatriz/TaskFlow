package com.example.taskflow.ui.entrar

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.taskflow.R
import com.example.taskflow.databinding.ActivityLoginBinding
import com.example.taskflow.ui.cadastrar.CadastrarActivity
import com.example.taskflow.ui.entrar.esquecersenha.EsquecerSenhaActivity
import com.example.taskflow.ui.main.MainActivity
import com.example.taskflow.utils.exibirMensagem

class EntrarActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityLoginBinding.inflate(layoutInflater)
    }

    private val viewModel: EntrarViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.teste)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        configurarListeners()
        observarEstado()
        observarErros()
    }

    /*override fun onStart() {
        super.onStart()
        // Deslogar sempre ao voltar para a tela de login
       // viewModel.deslogar()

        // Se usuário está logado, ir para MainActivity
        if (viewModel.verificarUsuarioLogado()) {
            navegarParaHome()
        }
    }*/

    private fun configurarListeners() {
        binding.btnSemConta.setOnClickListener {
            startActivity(Intent(this, CadastrarActivity::class.java))
        }

        binding.btnLogar.setOnClickListener {
            val email = binding.editLoginEmail.text.toString()
            val senha = binding.editLoginSenha.text.toString()
            viewModel.entrar(email, senha)
        }

        binding.btnEsqueceuSenha.setOnClickListener {
            startActivity(Intent(this, EsquecerSenhaActivity::class.java))
        }
    }

    private fun observarEstado() {
        viewModel.state.observe(this) { state ->
            when (state) {
                is EntrarState.Idle -> {
                    habilitarBotao()
                }
                is EntrarState.Loading -> {
                    desabilitarBotao()
                }
                is EntrarState.Success -> {
                    habilitarBotao()
                    exibirMensagem("Logado com sucesso!")
                    navegarParaHome()
                }
                is EntrarState.Error -> {
                    habilitarBotao()
                    exibirMensagem(state.message)
                    viewModel.limparEstado()
                }
            }
        }
    }

    private fun observarErros() {
        viewModel.emailErro.observe(this) { erro ->
            binding.TextInputLayoutLoginEmail.error = erro
        }

        viewModel.senhaErro.observe(this) { erro ->
            binding.TextInputLayoutLoginSenha.error = erro
        }
    }

    private fun desabilitarBotao() {
        binding.btnLogar.isEnabled = false
    }

    private fun habilitarBotao() {
        binding.btnLogar.isEnabled = true
    }

    private fun navegarParaHome() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}