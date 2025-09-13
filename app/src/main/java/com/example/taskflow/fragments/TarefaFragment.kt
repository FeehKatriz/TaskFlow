package com.example.taskflow.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.taskflow.R
import com.example.taskflow.databinding.FragmentTarefaBinding
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class TarefaFragment : Fragment() {

    private var _binding: FragmentTarefaBinding? = null
    private val binding get() = _binding!!

    private var tarefaId: String? = null
    private var tarefaTitulo: String? = null
    private var tarefaDescricao: String? = null
    private var tarefaStatus: String? = null

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
    ): View {
        _binding = FragmentTarefaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configurarBotaoVoltar()
        configurarToggleButtons()
        configurarDadosTarefa()
        configurarBotoesStatus()
    }

    private fun configurarBotaoVoltar() {
        binding.btnVoltar.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun configurarToggleButtons() {
        binding.toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
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
        binding.descricaoContainer.visibility = View.VISIBLE
        binding.arquivosContainer.visibility = View.GONE
    }

    private fun mostrarArquivos() {
        binding.descricaoContainer.visibility = View.GONE
        binding.arquivosContainer.visibility = View.VISIBLE
        carregarArquivos()
    }

    private fun configurarDadosTarefa() {
        binding.textViewTaskName.text = tarefaTitulo ?: "Tarefa"
        binding.textViewDescricao.text = if (tarefaDescricao.isNullOrBlank()) {
            "Nenhuma descrição disponível para esta tarefa."
        } else {
            tarefaDescricao
        }
        atualizarStatusDisplay()
    }

    private fun configurarBotoesStatus() {
        binding.btnStatusPendente.setOnClickListener { alterarStatus("pendente") }
        binding.btnStatusProgresso.setOnClickListener { alterarStatus("em_andamento") }
        binding.btnStatusConcluida.setOnClickListener { alterarStatus("concluida") }
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
        binding.textViewStatusAtual.text = "Status Atual: ${traduzirStatusParaUsuario(statusAtual)}"

        binding.btnStatusPendente.isEnabled = statusAtual != "pendente"
        binding.btnStatusProgresso.isEnabled = statusAtual != "em_andamento"
        binding.btnStatusConcluida.isEnabled = statusAtual != "concluida"
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
        binding.arquivosContainer.removeAllViews()

        val tarefaId = tarefaId ?: return
        val tarefaStorageRef = storageRef.child("tarefas/$tarefaId")

        // Botão de upload
        val btnUpload = MaterialButton(requireContext())
        btnUpload.text = "Adicionar Arquivo"
        btnUpload.setOnClickListener { escolherArquivo() }
        binding.arquivosContainer.addView(btnUpload)

        tarefaStorageRef.listAll()
            .addOnSuccessListener { listResult ->
                if (listResult.items.isEmpty()) {
                    val tv = TextView(requireContext())
                    tv.text = "Nenhum arquivo disponível."
                    tv.setPadding(16, 16, 16, 16)
                    binding.arquivosContainer.addView(tv)
                } else {
                    for (itemRef in listResult.items) {
                        // Pegar metadados para descobrir o MIME
                        itemRef.metadata.addOnSuccessListener { metadata ->
                            val mimeType = metadata.contentType ?: ""
                            val iconRes = getFileIconByMime(mimeType)

                            val itemView = layoutInflater.inflate(R.layout.item_arquivo, binding.arquivosContainer, false)
                            val imgIcon = itemView.findViewById<ImageView>(R.id.imgFileIcon)
                            val txtName = itemView.findViewById<TextView>(R.id.txtFileName)

                            txtName.text = itemRef.name
                            imgIcon.setImageResource(iconRes)

                            itemView.setOnClickListener { abrirArquivo(itemRef) }

                            binding.arquivosContainer.addView(itemView)
                        }.addOnFailureListener {
                            // Se falhar ao pegar metadados, mostra genérico
                            val itemView = layoutInflater.inflate(R.layout.item_arquivo, binding.arquivosContainer, false)
                            val imgIcon = itemView.findViewById<ImageView>(R.id.imgFileIcon)
                            val txtName = itemView.findViewById<TextView>(R.id.txtFileName)

                            txtName.text = itemRef.name
                            imgIcon.setImageResource(R.drawable.file)

                            itemView.setOnClickListener { abrirArquivo(itemRef) }

                            binding.arquivosContainer.addView(itemView)
                        }
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Erro ao carregar arquivos", Toast.LENGTH_SHORT).show()
            }
    }

    private fun escolherArquivo() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
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
            carregarArquivos()
        }.addOnFailureListener {
            Toast.makeText(context, "Erro ao enviar arquivo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun abrirArquivo(fileRef: StorageReference) {
        fileRef.downloadUrl
            .addOnSuccessListener { uri ->
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            }
            .addOnFailureListener {
                Toast.makeText(context, "Erro ao abrir arquivo", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getFileIconByMime(mimeType: String): Int {
        return when {
            mimeType.startsWith("image/") -> R.drawable.ic_file_image
            mimeType == "application/pdf" -> R.drawable.pdf
            mimeType == "application/msword" || mimeType.contains("wordprocessingml") -> R.drawable.word
            mimeType == "application/vnd.ms-excel" || mimeType.contains("spreadsheetml") -> R.drawable.xls
            else -> R.drawable.file
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}