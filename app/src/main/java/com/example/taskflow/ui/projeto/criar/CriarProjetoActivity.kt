package com.example.taskflow.ui.projeto.criar

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.taskflow.databinding.ActivityCriarProjetoBinding

class CriarProjetoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCriarProjetoBinding
    private val viewModel: CriarProjetoViewModel by viewModels()

    private val cores = arrayOf("#3F51B5", "#FF5722", "#4CAF50", "#FFC107", "#E91E63")
    private val nomesCores = arrayOf("Azul", "Laranja", "Verde", "Amarelo", "Rosa")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityCriarProjetoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.teste) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        configurarListeners()
        observarEstado()
        observarErros()
        observarCor()
    }

    private fun configurarListeners() {
        // Botão Voltar
        binding.button4.setOnClickListener {
            finish()
        }

        // Botão Escolher Cor
        binding.button11.setOnClickListener {
            abrirSeletorCor()
        }

        // Botão Criar Projeto
        binding.button7.setOnClickListener {
            val nome = binding.editTextText3.text.toString()
            viewModel.criarProjeto(nome)
        }
    }

    private fun observarEstado() {
        viewModel.state.observe(this) { state ->
            when (state) {
                is CriarProjetoState.Idle -> {
                    habilitarBotao()
                }
                is CriarProjetoState.Loading -> {
                    desabilitarBotao()
                }
                is CriarProjetoState.Success -> {
                    habilitarBotao()
                    Toast.makeText(
                        this,
                        "Projeto criado com sucesso!",
                        Toast.LENGTH_LONG
                    ).show()
                    binding.editTextText3.setText("")
                    setResult(RESULT_OK)
                    finish()
                }
                is CriarProjetoState.Error -> {
                    habilitarBotao()
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                    viewModel.limparEstado()
                }
            }
        }
    }

    private fun observarErros() {
        viewModel.nomeErro.observe(this) { erro ->
            if (erro != null) {
                binding.editTextText3.requestFocus()
            }
        }
    }

    private fun observarCor() {
        viewModel.corSelecionada.observe(this) { cor ->
            binding.button11.setBackgroundColor(Color.parseColor(cor))
        }
    }

    private fun abrirSeletorCor() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Escolha a cor do projeto")
        builder.setItems(nomesCores) { _, index ->
            val corSelecionada = cores[index]
            viewModel.setSelecionadaCor(corSelecionada)
            Toast.makeText(this, "Cor selecionada: ${nomesCores[index]}", Toast.LENGTH_SHORT).show()
        }
        builder.show()
    }

    private fun desabilitarBotao() {
        binding.button7.isEnabled = false
        binding.button7.text = "Criando..."
    }

    private fun habilitarBotao() {
        binding.button7.isEnabled = true
        binding.button7.text = "CRIAR PROJETO"
    }
}