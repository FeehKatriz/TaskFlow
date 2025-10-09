package com.example.taskflow.ui.tarefa.criar

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.taskflow.R
import com.example.taskflow.databinding.ActivityCriarTarefaBinding

class CriarTarefaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCriarTarefaBinding
    private val viewModel: CriarTarefaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityCriarTarefaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.teste)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val projetoId = intent.getStringExtra("projetoId")
        val equipeId = intent.getStringExtra("equipeId")

        viewModel.inicializarDados(projetoId, equipeId)

        configurarListeners()
        observarEstado()
    }

    private fun configurarListeners() {
        binding.button4.setOnClickListener {
            finish()
        }

        binding.button7.setOnClickListener {
            val titulo = binding.editTextText3.text.toString()
            val descricao = binding.editTextText4.text.toString()
            viewModel.criarTarefa(titulo, descricao)
        }

        binding.editTextText3.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && binding.editTextText3.text.toString() == "Titulo da Tarefa") {
                binding.editTextText3.setText("")
            }
        }

        binding.editTextText4.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && binding.editTextText4.text.toString() == "Descrição") {
                binding.editTextText4.setText("")
            }
        }
    }

    private fun observarEstado() {
        viewModel.state.observe(this) { state ->
            when (state) {
                is CriarTarefaState.Idle -> {}
                is CriarTarefaState.DadosCarregando -> {
                    // Aguardando dados
                }
                is CriarTarefaState.DadosCarregados -> {
                    habilitarBotao()
                }
                is CriarTarefaState.Loading -> {
                    desabilitarBotao()
                }
                is CriarTarefaState.Success -> {
                    habilitarBotao()
                    Toast.makeText(this, "Tarefa criada com sucesso!", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }
                is CriarTarefaState.Error -> {
                    habilitarBotao()
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                    viewModel.limparEstado()
                }
            }
        }

        viewModel.tituloErro.observe(this) { erro ->
            // Mostrar erro se necessário
            if (erro != null) {
                Toast.makeText(this, erro, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.descricaoErro.observe(this) { erro ->
            // Mostrar erro se necessário
            if (erro != null) {
                Toast.makeText(this, erro, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun desabilitarBotao() {
        binding.button7.isEnabled = false
        binding.button7.text = "Criando..."
    }

    private fun habilitarBotao() {
        binding.button7.isEnabled = true
        binding.button7.text = "CRIAR TAREFA"
    }
}