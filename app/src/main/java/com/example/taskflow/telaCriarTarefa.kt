package com.example.taskflow

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.taskflow.databinding.ActivityTelaCriarTarefaBinding
import com.example.taskflow.models.Tarefa
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class telaCriarTarefa : AppCompatActivity() {

    private lateinit var binding: ActivityTelaCriarTarefaBinding
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private var projetoIdSelecionado: String = ""
    private var equipeIdSelecionada: String = ""
    private val anexosSelecionados = mutableListOf<Uri>()
    private val anexosUploadUrls = mutableListOf<String>()

    // Flag para controlar se os dados foram carregados
    private var dadosCarregados = false

    // Launcher para seleção de arquivos
    private val selecionarArquivo = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            anexosSelecionados.addAll(uris)
            atualizarTextoAnexos()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityTelaCriarTarefaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.teste)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Receber ID do projeto se foi passado via Intent
        projetoIdSelecionado = intent.getStringExtra("projetoId") ?: ""
        equipeIdSelecionada = intent.getStringExtra("equipeId") ?: ""

        configurarListeners()

        // Se tem projeto e equipe definidos, marcar como carregado
        if (projetoIdSelecionado.isNotEmpty() && equipeIdSelecionada.isNotEmpty()) {
            dadosCarregados = true
        } else if (projetoIdSelecionado.isEmpty()) {
            // Se não tem projeto definido, buscar o primeiro disponível
            carregarProjetosDoUsuario()
        } else {
            // Se tem projeto mas não tem equipe, buscar a equipe do projeto
            buscarEquipeDoProjeto()
        }
    }

    private fun configurarListeners() {
        // Botão voltar
        binding.button4.setOnClickListener {
            finish()
        }

        // Botão anexar
        binding.button13.setOnClickListener {
            selecionarArquivo.launch("*/*")
        }

        // Botão criar tarefa
        binding.button7.setOnClickListener {
            criarTarefa()
        }

        // Limpar placeholder ao focar
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

    private fun atualizarTextoAnexos() {
        val quantidade = anexosSelecionados.size
        if (quantidade > 0) {
            binding.button13.text = "Anexar ($quantidade arquivo${if (quantidade > 1) "s" else ""})"
        } else {
            binding.button13.text = "Anexar"
        }
    }

    private fun buscarEquipeDoProjeto() {
        if (projetoIdSelecionado.isEmpty()) return

        firestore.collection("projetos")
            .document(projetoIdSelecionado)
            .get()
            .addOnSuccessListener { documento ->
                if (documento.exists()) {
                    equipeIdSelecionada = documento.getString("equipeId") ?: ""
                    dadosCarregados = true

                    if (equipeIdSelecionada.isEmpty()) {
                        Toast.makeText(this, "Projeto sem equipe associada", Toast.LENGTH_LONG).show()
                        finish()
                    }
                } else {
                    Toast.makeText(this, "Projeto não encontrado", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao buscar dados do projeto: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun carregarProjetosDoUsuario() {
        val userId = auth.currentUser?.uid ?: return

        // Buscar equipes do usuário primeiro
        firestore.collection("equipes")
            .whereArrayContains("membros", userId)
            .get()
            .addOnSuccessListener { equipesSnapshot ->
                if (!equipesSnapshot.isEmpty) {
                    val equipeIds = equipesSnapshot.documents.map { it.id }

                    // Buscar projetos das equipes do usuário
                    firestore.collection("projetos")
                        .whereIn("equipeId", equipeIds)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { projetosSnapshot ->
                            if (!projetosSnapshot.isEmpty) {
                                val projeto = projetosSnapshot.documents[0]
                                projetoIdSelecionado = projeto.id
                                equipeIdSelecionada = projeto.getString("equipeId") ?: ""
                                dadosCarregados = true

                                // Verificar se a equipe foi obtida corretamente
                                if (equipeIdSelecionada.isEmpty()) {
                                    Toast.makeText(this, "Erro: Projeto sem equipe associada", Toast.LENGTH_LONG).show()
                                    finish()
                                }
                            } else {
                                Toast.makeText(this, "Nenhum projeto encontrado. Crie um projeto primeiro.", Toast.LENGTH_LONG).show()
                                finish()
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Erro ao carregar projetos: ${e.message}", Toast.LENGTH_SHORT).show()
                            finish()
                        }
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

    private fun criarTarefa() {
        val titulo = binding.editTextText3.text.toString().trim()
        val descricao = binding.editTextText4.text.toString().trim()

        // Validações
        if (titulo.isEmpty() || titulo == "Titulo da Tarefa") {
            Toast.makeText(this, "Digite o título da tarefa", Toast.LENGTH_SHORT).show()
            return
        }

        if (descricao.isEmpty() || descricao == "Descrição") {
            Toast.makeText(this, "Digite a descrição da tarefa", Toast.LENGTH_SHORT).show()
            return
        }

        // Verificar se os dados foram carregados
        if (!dadosCarregados) {
            Toast.makeText(this, "Aguarde o carregamento dos dados...", Toast.LENGTH_SHORT).show()
            return
        }

        if (projetoIdSelecionado.isEmpty()) {
            Toast.makeText(this, "Erro: Projeto não selecionado", Toast.LENGTH_SHORT).show()
            return
        }

        if (equipeIdSelecionada.isEmpty()) {
            Toast.makeText(this, "Erro: Equipe não selecionada", Toast.LENGTH_SHORT).show()
            return
        }

        // Desabilitar botão durante criação
        binding.button7.isEnabled = false
        binding.button7.text = "Criando..."

        // Se há anexos, fazer upload primeiro
        if (anexosSelecionados.isNotEmpty()) {
            uploadAnexos { anexosUrls ->
                salvarTarefa(titulo, descricao, anexosUrls)
            }
        } else {
            salvarTarefa(titulo, descricao, emptyList())
        }
    }

    private fun uploadAnexos(callback: (List<String>) -> Unit) {
        val urls = mutableListOf<String>()
        var uploadsCompletos = 0
        val totalUploads = anexosSelecionados.size

        anexosSelecionados.forEachIndexed { index, uri ->
            val nomeArquivo = obterNomeArquivo(uri) ?: "anexo_$index"
            val referencia = storage.reference
                .child("tarefas")
                .child(projetoIdSelecionado)
                .child("${System.currentTimeMillis()}_$nomeArquivo")

            referencia.putFile(uri)
                .addOnSuccessListener {
                    referencia.downloadUrl
                        .addOnSuccessListener { downloadUrl ->
                            urls.add(downloadUrl.toString())
                            uploadsCompletos++

                            if (uploadsCompletos == totalUploads) {
                                callback(urls)
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Erro ao obter URL do anexo: ${e.message}", Toast.LENGTH_SHORT).show()
                            reabilitarBotao()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Erro ao fazer upload do anexo: ${e.message}", Toast.LENGTH_SHORT).show()
                    reabilitarBotao()
                }
        }
    }

    private fun obterNomeArquivo(uri: Uri): String? {
        var nome: String? = null
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nomeIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nomeIndex != -1) {
                nome = cursor.getString(nomeIndex)
            }
        }
        return nome
    }

    private fun salvarTarefa(titulo: String, descricao: String, anexosUrls: List<String>) {
        val userId = auth.currentUser?.uid ?: return

        // Criar objeto tarefa
        val tarefa = Tarefa(
            titulo = titulo,
            descricao = descricao,
            projetoId = projetoIdSelecionado,
            equipeId = equipeIdSelecionada,
            criadoPor = userId,
            anexos = anexosUrls
        )

        // Salvar no Firebase
        firestore.collection("tarefas")
            .add(tarefa)
            .addOnSuccessListener { documentReference ->
                // Atualizar a tarefa com o ID gerado pelo Firebase
                documentReference.update("id", documentReference.id)
                    .addOnSuccessListener {
                        // Atualizar contador de tarefas do projeto
                        atualizarContadorTarefasProjeto()
                        Toast.makeText(this, "Tarefa criada com sucesso!", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Tarefa criada, mas erro ao definir ID: ${e.message}", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK) // Mesmo assim foi criada
                        finish()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao criar tarefa: ${e.message}", Toast.LENGTH_SHORT).show()
                reabilitarBotao()
            }
    }

    private fun atualizarContadorTarefasProjeto() {
        firestore.collection("tarefas")
            .whereEqualTo("projetoId", projetoIdSelecionado)
            .get()
            .addOnSuccessListener { snapshot ->
                val totalTarefas = snapshot.size()

                firestore.collection("projetos")
                    .document(projetoIdSelecionado)
                    .update("totalTarefas", totalTarefas)
            }
    }

    private fun reabilitarBotao() {
        binding.button7.isEnabled = true
        binding.button7.text = "CRIAR TAREFA"
    }
}