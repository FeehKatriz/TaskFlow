package com.example.taskflow

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.taskflow.databinding.ActivityTelaCriarNovaEquipeBinding
import com.example.taskflow.models.Equipe
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class CriarNovaEquipe : AppCompatActivity() {

    private lateinit var binding: ActivityTelaCriarNovaEquipeBinding
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var projetoIdSelecionado: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityTelaCriarNovaEquipeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.teste)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Receber ID do projeto se foi passado via Intent
        projetoIdSelecionado = intent.getStringExtra("projetoId") ?: ""

        configurarListeners()

        // Se não tem projeto definido, buscar o primeiro do usuário
        if (projetoIdSelecionado.isEmpty()) {
            carregarProjetosDoUsuario()
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

        // Botão criar equipe
        binding.button7.setOnClickListener {
            criarEquipe()
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

    private fun carregarProjetosDoUsuario() {
        val userId = auth.currentUser?.uid ?: return

        // Buscar o primeiro projeto do usuário (pode ser expandido para seleção)
        firestore.collection("projetos")
            .whereArrayContains("membros", userId)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    projetoIdSelecionado = snapshot.documents[0].id
                } else {
                    Toast.makeText(this, "Você precisa fazer parte de um projeto primeiro", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao carregar projetos: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun criarEquipe() {
        val nomeEquipe = binding.editTextText3.text.toString().trim()
        val prazo = binding.editTextText4.text.toString().trim()

        // Validações
        if (nomeEquipe.isEmpty() || nomeEquipe == "Nome da Equipe") {
            Toast.makeText(this, "Digite o nome da equipe", Toast.LENGTH_SHORT).show()
            return
        }

        if (prazo.isEmpty() || prazo == "Prazo") {
            Toast.makeText(this, "Selecione o prazo da equipe", Toast.LENGTH_SHORT).show()
            return
        }

        if (projetoIdSelecionado.isEmpty()) {
            Toast.makeText(this, "Erro: Projeto não selecionado", Toast.LENGTH_SHORT).show()
            return
        }

        // Desabilitar botão durante criação
        binding.button7.isEnabled = false
        binding.button7.text = "Criando..."

        // Criar objeto equipe
        val equipe = Equipe(
            nome = nomeEquipe,
            descricao = "", // Pode adicionar campo de descrição se necessário
            dataVencimento = prazo,
            progresso = 0,
            totalTarefas = 0,
            projetoId = projetoIdSelecionado
        )

        // Salvar no Firebase
        firestore.collection("equipes")
            .add(equipe)
            .addOnSuccessListener { documentReference ->
                // Atualizar a equipe com o ID gerado pelo Firebase
                documentReference.update("id", documentReference.id)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Equipe criada com sucesso!", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Equipe criada, mas erro ao definir ID: ${e.message}", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK) // Mesmo assim foi criada
                        finish()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao criar equipe: ${e.message}", Toast.LENGTH_SHORT).show()
                // Reabilitar botão
                binding.button7.isEnabled = true
                binding.button7.text = "CRIAR EQUIPE"
            }
    }
}