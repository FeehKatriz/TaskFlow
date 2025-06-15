package com.example.taskflow

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.taskflow.databinding.ActivityTelaCriarNovoProjetoBinding
import com.example.taskflow.models.Projeto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class CriarNovoProjeto : AppCompatActivity() {

    private lateinit var binding: ActivityTelaCriarNovoProjetoBinding
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var equipeIdSelecionada: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityTelaCriarNovoProjetoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.teste)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Receber ID da equipe se foi passado via Intent
        equipeIdSelecionada = intent.getStringExtra("equipeId") ?: ""

        configurarListeners()

        // Se não tem equipe definida, buscar a primeira do usuário
        if (equipeIdSelecionada.isEmpty()) {
            carregarEquipesDoUsuario()
        }
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

        // Botão criar projeto
        binding.button7.setOnClickListener {
            criarProjeto()
        }

        // Limpar placeholder ao focar
        binding.editTextText3.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && binding.editTextText3.text.toString() == "Nome do Projeto") {
                binding.editTextText3.setText("")
            }
        }

        binding.editTextText4.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && binding.editTextText4.text.toString() == "Prazo") {
                binding.editTextText4.setText("")
            }
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
                val dataFormatada = String.format(
                    "%02d/%02d/%d",
                    diaSelecionado,
                    mesSelecionado + 1,
                    anoSelecionado
                )
                binding.editTextText4.setText(dataFormatada)
            },
            ano, mes, dia
        )

        // Definir data mínima como hoje
        datePickerDialog.datePicker.minDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun carregarEquipesDoUsuario() {
        val userId = auth.currentUser?.uid ?: return

        // Buscar a primeira equipe do usuário (pode ser expandido para seleção)
        firestore.collection("equipes")
            .whereArrayContains("membros", userId)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    equipeIdSelecionada = snapshot.documents[0].id
                } else {
                    Toast.makeText(this, "Você precisa fazer parte de uma equipe primeiro", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao carregar equipes: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun criarProjeto() {
        val nomeProjeto = binding.editTextText3.text.toString().trim()
        val prazo = binding.editTextText4.text.toString().trim()

        // Validações
        if (nomeProjeto.isEmpty() || nomeProjeto == "Nome do Projeto") {
            Toast.makeText(this, "Digite o nome do projeto", Toast.LENGTH_SHORT).show()
            return
        }

        if (prazo.isEmpty() || prazo == "Prazo") {
            Toast.makeText(this, "Selecione o prazo do projeto", Toast.LENGTH_SHORT).show()
            return
        }

        if (equipeIdSelecionada.isEmpty()) {
            Toast.makeText(this, "Erro: Equipe não selecionada", Toast.LENGTH_SHORT).show()
            return
        }

        // Desabilitar botão durante criação
        binding.button7.isEnabled = false
        binding.button7.text = "Criando..."

        // Criar objeto projeto
        val projeto = Projeto(
            nome = nomeProjeto,
            descricao = "", // Pode adicionar campo de descrição se necessário
            dataVencimento = prazo,
            progresso = 0,
            totalTarefas = 0,
            equipeId = equipeIdSelecionada
        )

        // Salvar no Firebase
        firestore.collection("projetos")
            .add(projeto)
            .addOnSuccessListener { documentReference ->
                // Atualizar o projeto com o ID gerado pelo Firebase
                documentReference.update("id", documentReference.id)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Projeto criado com sucesso!", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Projeto criado, mas erro ao definir ID: ${e.message}", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK) // Mesmo assim foi criado
                        finish()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao criar projeto: ${e.message}", Toast.LENGTH_SHORT).show()
                // Reabilitar botão
                binding.button7.isEnabled = true
                binding.button7.text = "CRIAR PROJETO"
            }
    }
}