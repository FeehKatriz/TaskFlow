package com.example.taskflow.ui.projetos

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.taskflow.databinding.ActivityTelaCriarProjetoBinding
import com.example.taskflow.data.model.Projeto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.random.Random

class CriarProjetoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTelaCriarProjetoBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityTelaCriarProjetoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.teste) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupClickListeners()
    }

    private fun setupClickListeners() {
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
            criarProjeto()
        }
    }

    private fun abrirSeletorCor() {
        // Lista de cores disponíveis (hex)
        val cores = arrayOf("#3F51B5", "#FF5722", "#4CAF50", "#FFC107", "#E91E63")
        val nomesCores = arrayOf("Azul", "Laranja", "Verde", "Amarelo", "Rosa")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Escolha a cor do projeto")
        builder.setItems(nomesCores) { _, index ->
            val corSelecionada = cores[index]
            binding.button11.setBackgroundColor(Color.parseColor(corSelecionada))
            binding.button11.tag = corSelecionada // Guardar a cor escolhida
            Toast.makeText(this, "Cor selecionada: ${nomesCores[index]}", Toast.LENGTH_SHORT).show()
        }
        builder.show()
    }

    private fun gerarCodigoProjeto(): String {
        // Gera um código de 10 caracteres alfanuméricos
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..10)
            .map { chars[Random.Default.nextInt(chars.length)] }
            .joinToString("")
    }

    private fun verificarCodigoUnico(codigo: String, callback: (Boolean) -> Unit) {
        db.collection("projetos")
            .whereEqualTo("codigo", codigo)
            .get()
            .addOnSuccessListener { documents ->
                callback(documents.isEmpty)
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    private fun criarProjetoComCodigoUnico() {
        val codigo = gerarCodigoProjeto()

        verificarCodigoUnico(codigo) { isUnico ->
            if (isUnico) {
                // Código é único, criar o projeto
                criarProjetoNoFirestore(codigo)
            } else {
                // Código já existe, tentar novamente
                criarProjetoComCodigoUnico()
            }
        }
    }

    private fun criarProjetoNoFirestore(codigo: String) {
        val nomeProjeto = binding.editTextText3.text.toString().trim()
        val usuarioAtual = auth.currentUser!!

        // Usar cor selecionada ou padrão
        val corProjeto = binding.button11.tag?.toString() ?: "#3F51B5"

        // Primeiro criar o documento para obter o ID
        val projetoRef = db.collection("projetos").document()
        val projetoId = projetoRef.id

        // Criar projeto usando o modelo com código, ID e cor selecionada
        val projeto = Projeto(
            id = projetoId,
            nome = nomeProjeto,
            criador = usuarioAtual.uid,
            membros = listOf(usuarioAtual.uid),
            cor = corProjeto,
        )

        // Salvar o projeto com o ID definido
        projetoRef.set(projeto)
            .addOnSuccessListener {
                println("Projeto criado com ID: $projetoId, código: $codigo, cor: $corProjeto")
                Toast.makeText(this, "Projeto '$nomeProjeto' criado!\nCódigo: $codigo", Toast.LENGTH_LONG).show()
                binding.editTextText3.setText("")

                // Definir resultado para informar que o projeto foi criado com sucesso
                setResult(RESULT_OK)
                finish()
            }
            .addOnFailureListener { e ->
                println("Erro ao criar projeto: ${e.message}")
                Toast.makeText(this, "Erro ao criar projeto: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun criarProjeto() {
        val nomeProjeto = binding.editTextText3.text.toString().trim()
        val usuarioAtual = auth.currentUser

        println("Nome do projeto: '$nomeProjeto'")
        println("Usuário atual: ${usuarioAtual?.uid}")

        if (nomeProjeto.isEmpty()) {
            Toast.makeText(this, "Por favor, insira o nome do projeto", Toast.LENGTH_SHORT).show()
            binding.editTextText3.requestFocus()
            return
        }

        if (usuarioAtual == null) {
            Toast.makeText(this, "Usuário não autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        // Mostrar que começou a criação
        Toast.makeText(this, "Criando projeto...", Toast.LENGTH_SHORT).show()

        // Criar projeto com código único
        criarProjetoComCodigoUnico()
    }
}