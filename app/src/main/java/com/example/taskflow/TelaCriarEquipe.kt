package com.example.taskflow

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.taskflow.databinding.ActivityTelaCriarEquipeBinding
import com.example.taskflow.models.Equipe
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TelaCriarEquipe : AppCompatActivity() {

    private lateinit var binding: ActivityTelaCriarEquipeBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityTelaCriarEquipeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.teste) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Limpar o texto padrão do EditText
        binding.editTextText3.setText("")
        binding.editTextText3.hint = "Digite o nome da equipe"

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Botão Voltar
        binding.button4.setOnClickListener {
            finish()
        }

        // Botão Escolher Cor
        binding.button11.setOnClickListener {
            Toast.makeText(this, "Funcionalidade de escolher cor em desenvolvimento", Toast.LENGTH_SHORT).show()
        }

        // Botão Criar Equipe
        binding.button7.setOnClickListener {
            criarEquipe()
        }
    }

    private fun criarEquipe() {
        val nomeEquipe = binding.editTextText3.text.toString().trim()
        val usuarioAtual = auth.currentUser

        // Debug - adicionar logs
        println("Nome da equipe: '$nomeEquipe'")
        println("Usuário atual: ${usuarioAtual?.uid}")

        if (nomeEquipe.isEmpty()) {
            Toast.makeText(this, "Por favor, insira o nome da equipe", Toast.LENGTH_SHORT).show()
            binding.editTextText3.requestFocus()
            return
        }

        if (usuarioAtual == null) {
            Toast.makeText(this, "Usuário não autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        // Mostrar que começou a criação
        Toast.makeText(this, "Criando equipe...", Toast.LENGTH_SHORT).show()

        // Criar equipe usando o modelo
        val equipe = Equipe(
            nome = nomeEquipe,
            criador = usuarioAtual.uid,
            membros = listOf(usuarioAtual.uid),
            cor = "#3F51B5" // Cor padrão por enquanto
        )


        db.collection("equipes")
            .add(equipe)
            .addOnSuccessListener { documentReference ->
                println("Equipe criada com ID: ${documentReference.id}")
                Toast.makeText(this, "Equipe '$nomeEquipe' criada com sucesso!", Toast.LENGTH_SHORT).show()
                binding.editTextText3.setText("")
                finish()
            }
            .addOnFailureListener { e ->
                println("Erro ao criar equipe: ${e.message}")
                Toast.makeText(this, "Erro ao criar equipe: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}