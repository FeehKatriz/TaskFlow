package com.example.taskflow.ui.equipe.criar

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.taskflow.R
import com.example.taskflow.databinding.ActivityCriarEquipeBinding
import com.google.android.material.chip.Chip
import java.util.Calendar

class CriarEquipeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCriarEquipeBinding
    private val viewModel: CriarEquipeViewModel by viewModels()
    private val membrosSelecionados = mutableListOf<Pair<String, String>>() // id, nome

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

        val projetoIdRecebido = intent.getStringExtra("projetoId")
        viewModel.inicializarProjeto(projetoIdRecebido)

        configurarListeners()
        observarEstado()
    }

    private fun configurarListeners() {
        binding.button4.setOnClickListener {
            finish()
        }

        binding.imageView22.setOnClickListener {
            abrirSeletorData()
        }

        binding.editTextText4.setOnClickListener {
            abrirSeletorData()
        }

        // Novo: Botão para selecionar membros
        binding.btnSelecionarMembros.setOnClickListener {
            viewModel.carregarUsuariosDisponiveis()
        }

        binding.button7.setOnClickListener {
            val nomeEquipe = binding.editTextText3.text.toString().trim()
            val prazo = binding.editTextText4.text.toString().trim()
            val membros = membrosSelecionados.map { it.first }
            viewModel.criarEquipe(nomeEquipe, prazo, membros)
        }

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
                is CriarEquipeState.UsuariosCarregados -> {
                    mostrarDialogSelecionarMembros(state.usuarios)
                }
                is CriarEquipeState.Error -> {
                    habilitarBotao()
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()

                    if (state.message.contains("fazer parte de um projeto")) {
                        finish()
                    }

                    viewModel.limparEstado()
                }
            }
        }

        viewModel.dataFormatada.observe(this) { data ->
            binding.editTextText4.setText(data)
        }
    }

    private fun mostrarDialogSelecionarMembros(usuarios: List<Pair<String, String>>) {
        if (usuarios.isEmpty()) {
            Toast.makeText(this, "Nenhum usuário disponível no projeto", Toast.LENGTH_SHORT).show()
            return
        }

        val nomes = usuarios.map { it.second }.toTypedArray()
        val selecionados = BooleanArray(nomes.size) { false }

        // Marcar os já selecionados
        usuarios.forEachIndexed { index, usuario ->
            if (membrosSelecionados.any { it.first == usuario.first }) {
                selecionados[index] = true
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Adicionar Membros")
            .setMultiChoiceItems(nomes, selecionados) { _, which, isChecked ->
                selecionados[which] = isChecked
            }
            .setPositiveButton("Confirmar") { _, _ ->
                // Limpar chips atuais
                binding.chipGroupMembros.removeAllViews()
                membrosSelecionados.clear()

                // Adicionar os selecionados
                usuarios.forEachIndexed { index, usuario ->
                    if (selecionados[index]) {
                        adicionarChipMembro(usuario.first, usuario.second)
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun adicionarChipMembro(userId: String, userName: String) {
        val chip = Chip(this).apply {
            text = userName
            isCloseIconVisible = true
            setOnCloseIconClickListener {
                membrosSelecionados.removeIf { it.first == userId }
                binding.chipGroupMembros.removeView(this)
            }
        }
        binding.chipGroupMembros.addView(chip)
        membrosSelecionados.add(Pair(userId, userName))
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