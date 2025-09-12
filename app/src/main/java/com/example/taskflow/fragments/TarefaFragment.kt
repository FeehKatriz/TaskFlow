package com.example.taskflow.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.taskflow.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class TarefaFragment : Fragment() {

    private var tarefaId: String? = null
    private var tarefaTitulo: String? = null
    private var tarefaDescricao: String? = null
    private var tarefaStatus: String? = null

    private lateinit var textViewTaskName: TextView
    private lateinit var toggleGroup: MaterialButtonToggleGroup
    private lateinit var btnDescricao: MaterialButton
    private lateinit var btnArquivos: MaterialButton
    private lateinit var descricaoContainer: ScrollView
    private lateinit var arquivosContainer: LinearLayout
    private lateinit var textViewDescricao: TextView
    private lateinit var textViewStatusAtual: TextView
    private lateinit var btnStatusPendente: Button
    private lateinit var btnStatusProgresso: Button
    private lateinit var btnStatusConcluida: Button

    private val storageRef = FirebaseStorage.getInstance().reference
    private val PICK_FILE_REQUEST = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { bundle ->
            tarefaId = bundle.getString("tarefaId")
            tarefaTitulo = bundle.getString("tarefaTitulo")
            tarefaDescricao = bundle.getString("tarefaDescricao")
            tarefaStatus = bundle.getString("tarefaStatus")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_tarefa, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        configurarBotaoVoltar(view)
        configurarToggleButtons()
        configurarDadosTarefa()
        configurarBotoesStatus()
    }

    private fun initViews(view: View) {
        textViewTaskName = view.findViewById(R.id.textViewTaskName)
        toggleGroup = view.findViewById(R.id.toggleGroup)
        btnDescricao = view.findViewById(R.id.btnDescricao)
        btnArquivos = view.findViewById(R.id.btnArquivos)
        descricaoContainer = view.findViewById(R.id.descricaoContainer)
        arquivosContainer = view.findViewById(R.id.arquivosContainer)
        textViewDescricao = view.findViewById(R.id.textViewDescricao)
        textViewStatusAtual = view.findViewById(R.id.textViewStatusAtual)
        btnStatusPendente = view.findViewById(R.id.btnStatusPendente)
        btnStatusProgresso = view.findViewById(R.id.btnStatusProgresso)
        btnStatusConcluida = view.findViewById(R.id.btnStatusConcluida)
    }

    private fun configurarBotaoVoltar(view: View) {
        view.findViewById<View>(R.id.btnVoltar).setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun configurarToggleButtons() {
        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnDescricao -> mostrarDescricao()
                    R.id.btnArquivos -> mostrarArquivos()
                }
            }
        }
        mostrarDescricao()
    }

    private fun mostrarDescricao() {
        descricaoContainer.visibility = View.VISIBLE
        arquivosContainer.visibility = View.GONE
    }

    private fun mostrarArquivos() {
        descricaoContainer.visibility = View.GONE
        arquivosContainer.visibility = View.VISIBLE

        carregarArquivos()
    }

    private fun configurarDadosTarefa() {
        textViewTaskName.text = tarefaTitulo ?: "Tarefa"
        textViewDescricao.text = if (tarefaDescricao.isNullOrBlank()) {
            "Nenhuma descrição disponível para esta tarefa."
        } else {
            tarefaDescricao
        }
        atualizarStatusDisplay()
    }

    private fun configurarBotoesStatus() {
        btnStatusPendente.setOnClickListener {
            alterarStatus("pendente")
        }

        btnStatusProgresso.setOnClickListener {
            alterarStatus("em_andamento")
        }

        btnStatusConcluida.setOnClickListener {
            alterarStatus("concluida")
        }
    }

    private fun alterarStatus(novoStatus: String) {
        tarefaStatus = novoStatus
        atualizarStatusDisplay()

        val db = FirebaseFirestore.getInstance()
        val id = tarefaId

        if (id != null) {
            db.collection("tarefas")
                .document(id)
                .update("status", novoStatus)
                .addOnSuccessListener {
                    Toast.makeText(
                        context,
                        "Status alterado para: ${traduzirStatusParaUsuario(novoStatus)}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Erro ao atualizar status no Firebase.", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(context, "ID da tarefa não encontrado.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun atualizarStatusDisplay() {
        val statusAtual = tarefaStatus ?: "não_definido"
        textViewStatusAtual.text = "Status Atual: ${traduzirStatusParaUsuario(statusAtual)}"

        btnStatusPendente.isEnabled = statusAtual != "pendente"
        btnStatusProgresso.isEnabled = statusAtual != "em_andamento"
        btnStatusConcluida.isEnabled = statusAtual != "concluida"
    }

    private fun traduzirStatusParaUsuario(status: String?): String {
        return when (status) {
            "pendente" -> "Pendente"
            "em_andamento" -> "Em andamento"
            "concluida" -> "Concluída"
            else -> "Status desconhecido"
        }
    }

    // -----------------------------
    // 🔹 Parte de Arquivos
    // -----------------------------

    private fun carregarArquivos() {
        arquivosContainer.removeAllViews()

        val tarefaId = tarefaId ?: return
        val tarefaStorageRef = storageRef.child("tarefas/$tarefaId")

        // Botão de upload
        val btnUpload = Button(requireContext())
        btnUpload.text = "Adicionar Arquivo"
        btnUpload.setOnClickListener { escolherArquivo() }
        arquivosContainer.addView(btnUpload)

        tarefaStorageRef.listAll()
            .addOnSuccessListener { listResult ->
                if (listResult.items.isEmpty()) {
                    val tv = TextView(requireContext())
                    tv.text = "Nenhum arquivo disponível."
                    tv.setPadding(16, 16, 16, 16)
                    arquivosContainer.addView(tv)
                } else {
                    for (itemRef in listResult.items) {
                        val btn = Button(requireContext())
                        btn.text = itemRef.name
                        btn.setOnClickListener { abrirArquivo(itemRef) }
                        arquivosContainer.addView(btn)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Erro ao carregar arquivos", Toast.LENGTH_SHORT).show()
            }
    }

    private fun escolherArquivo() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*" // aceita qualquer tipo
        startActivityForResult(intent, PICK_FILE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FILE_REQUEST && resultCode == Activity.RESULT_OK) {
            val fileUri: Uri? = data?.data
            if (fileUri != null) {
                uploadArquivo(fileUri)
            }
        }
    }

    private fun uploadArquivo(fileUri: Uri) {
        val tarefaId = tarefaId ?: return
        val fileName = System.currentTimeMillis().toString() + "_" + (fileUri.lastPathSegment ?: "arquivo")

        val fileRef = storageRef.child("tarefas/$tarefaId/$fileName")

        val uploadTask = fileRef.putFile(fileUri)
        uploadTask.addOnSuccessListener {
            Toast.makeText(context, "Arquivo enviado com sucesso!", Toast.LENGTH_SHORT).show()
            carregarArquivos() // recarrega lista
        }.addOnFailureListener {
            Toast.makeText(context, "Erro ao enviar arquivo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun abrirArquivo(fileRef: com.google.firebase.storage.StorageReference) {
        fileRef.downloadUrl
            .addOnSuccessListener { uri ->
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            }
            .addOnFailureListener {
                Toast.makeText(context, "Erro ao abrir arquivo", Toast.LENGTH_SHORT).show()
            }
    }
}