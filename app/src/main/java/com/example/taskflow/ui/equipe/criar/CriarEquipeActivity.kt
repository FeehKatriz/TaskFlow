package com.example.taskflow.ui.equipe.criar

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.taskflow.R
import com.example.taskflow.databinding.ActivityCriarEquipeBinding
import java.util.Calendar

class CriarEquipeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCriarEquipeBinding
    private val viewModel: CriarEquipeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityCriarEquipeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.teste)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicializar projeto
        val projetoIdRecebido = intent.getStringExtra("projetoId")
        viewModel.inicializarProjeto(projetoIdRecebido)

        configurarListeners()
        observarEstado()
    }

    private fun configurarListeners() {
        // Botão voltar
        binding.button4.setOnClickListener {
            finish()
        }

        // Seletor de data
        binding.imageView22.setOnClickListener {
            abrirSeletorData()
        }

        binding.editTextText4.setOnClickListener {
            abrirSeletorData()
        }

        // Botão criar equipe
        binding.button7.setOnClickListener {
            val nomeEquipe = binding.editTextText3.text.toString().trim()
            val prazo = binding.editTextText4.text.toString().trim()
            viewModel.criarEquipe(nomeEquipe, prazo)
        }

        // Limpar placeholder ao focar
        binding.editTextText3.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && binding.editTextText3.text.toString() == "Nome da Equipe") {
                binding.editTextText3.setText("")
            }
        }

        binding.editTextText4.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && binding.editTextText4.text.toString() == "Prazo") {
                binding.editTextText4.setText("")
            }
        }
    }

    private fun observarEstado() {
        // Observar estado da criação
        viewModel.state.observe(this) { state ->
            when (state) {
                is CriarEquipeState.Idle -> {
                    habilitarBotao()
                }
                is CriarEquipeState.Loading -> {
                    desabilitarBotao()
                }
                is CriarEquipeState.Success -> {
                    habilitarBotao()
                    Toast.makeText(this, "Equipe criada com sucesso!", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }
                is CriarEquipeState.Error -> {
                    habilitarBotao()
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()

                    // Se o erro for de projeto não encontrado, fechar tela
                    if (state.message.contains("fazer parte de um projeto")) {
                        finish()
                    }

                    viewModel.limparEstado()
                }
            }
        }

        // Observar data formatada
        viewModel.dataFormatada.observe(this) { data ->
            binding.editTextText4.setText(data)
        }
    }

    private fun abrirSeletorData() {
        val calendar = Calendar.getInstance()
        val ano = calendar.get(Calendar.YEAR)
        val mes = calendar.get(Calendar.MONTH)
        val dia = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, anoSelecionado, mesSelecionado, diaSelecionado ->
                viewModel.setData(diaSelecionado, mesSelecionado, anoSelecionado)
            },
            ano, mes, dia
        )

        // Definir data mínima como hoje
        datePickerDialog.datePicker.minDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun desabilitarBotao() {
        binding.button7.isEnabled = false
        binding.button7.text = "Criando..."
    }

    private fun habilitarBotao() {
        binding.button7.isEnabled = true
        binding.button7.text = "CRIAR EQUIPE"
    }
}