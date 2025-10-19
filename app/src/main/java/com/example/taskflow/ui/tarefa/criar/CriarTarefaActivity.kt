package com.example.taskflow.ui.tarefa.criar

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.taskflow.R
import com.example.taskflow.databinding.ActivityCriarTarefaBinding
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.*

class CriarTarefaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCriarTarefaBinding
    private val viewModel: CriarTarefaViewModel by viewModels()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault())
    private var selectedDate: Calendar? = null
    private val responsaveisSelecionados = mutableListOf<Pair<String, String>>() // id, nome

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
            criarTarefa()
        }

        // Seletor de data/hora
        binding.btnSelecionarData.setOnClickListener {
            mostrarSeletorDataHora()
        }

        // Botão para selecionar responsáveis
        binding.btnSelecionarResponsaveis.setOnClickListener {
            viewModel.carregarMembrosEquipe()
        }
    }

    private fun mostrarSeletorDataHora() {
        val calendar = selectedDate ?: Calendar.getInstance()

        // Primeiro seleciona a data
        DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, day)

                // Depois seleciona a hora
                TimePickerDialog(
                    this,
                    { _, hour, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hour)
                        calendar.set(Calendar.MINUTE, minute)

                        selectedDate = calendar
                        binding.textViewDataSelecionada.text = dateFormat.format(calendar.time)
                        binding.textViewDataSelecionada.setTextColor(
                            resources.getColor(android.R.color.black, null)
                        )
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = System.currentTimeMillis()
        }.show()
    }

    private fun adicionarChipResponsavel(userId: String, userName: String) {
        val chip = Chip(this).apply {
            text = userName
            isCloseIconVisible = true
            setOnCloseIconClickListener {
                responsaveisSelecionados.removeIf { it.first == userId }
                binding.chipGroupResponsaveis.removeView(this)
            }
        }
        binding.chipGroupResponsaveis.addView(chip)
        responsaveisSelecionados.add(Pair(userId, userName))
    }

    private fun getPrioridadeSelecionada(): String {
        return when (binding.radioGroupPrioridade.checkedRadioButtonId) {
            R.id.radioBaixa -> "baixa"
            R.id.radioMedia -> "media"
            R.id.radioAlta -> "alta"
            else -> "media"
        }
    }

    private fun criarTarefa() {
        val titulo = binding.editTextText3.text.toString()
        val descricao = binding.editTextText4.text.toString()
        val prioridade = getPrioridadeSelecionada()
        val dataVencimento = selectedDate?.let { dateFormat.format(it.time) }
        val responsaveis = responsaveisSelecionados.map { it.first }

        viewModel.criarTarefa(
            titulo = titulo,
            descricao = descricao,
            prioridade = prioridade,
            dataVencimento = dataVencimento,
            responsaveis = responsaveis
        )
    }

    private fun observarEstado() {
        viewModel.state.observe(this) { state ->
            when (state) {
                is CriarTarefaState.Idle -> {}
                is CriarTarefaState.DadosCarregando -> {
                    binding.button7.isEnabled = false
                }
                is CriarTarefaState.DadosCarregados -> {
                    binding.button7.isEnabled = true
                }
                is CriarTarefaState.MembrosCarregados -> {
                    mostrarDialogSelecionarMembros(state.membros)
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
            binding.tilTaskTitle.error = erro
        }

        viewModel.descricaoErro.observe(this) { erro ->
            binding.tilTaskDescription.error = erro
        }
    }

    private fun mostrarDialogSelecionarMembros(membros: List<Pair<String, String>>) {
        if (membros.isEmpty()) {
            Toast.makeText(this, "Nenhum membro encontrado na equipe", Toast.LENGTH_SHORT).show()
            return
        }

        val nomes = membros.map { it.second }.toTypedArray()
        val selecionados = BooleanArray(nomes.size) { false }

        // Marcar os já selecionados
        membros.forEachIndexed { index, membro ->
            if (responsaveisSelecionados.any { it.first == membro.first }) {
                selecionados[index] = true
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Selecionar Responsáveis")
            .setMultiChoiceItems(nomes, selecionados) { _, which, isChecked ->
                selecionados[which] = isChecked
            }
            .setPositiveButton("Confirmar") { _, _ ->
                // Limpar chips atuais
                binding.chipGroupResponsaveis.removeAllViews()
                responsaveisSelecionados.clear()

                // Adicionar os selecionados
                membros.forEachIndexed { index, membro ->
                    if (selecionados[index]) {
                        adicionarChipResponsavel(membro.first, membro.second)
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
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