package com.example.taskflow.ui.tarefa

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
import androidx.fragment.app.viewModels
import com.example.taskflow.R
import com.example.taskflow.databinding.FragmentTarefaBinding
import com.google.android.material.button.MaterialButton

class TarefaFragment : Fragment() {

    private var _binding: FragmentTarefaBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TarefaViewModel by viewModels()
    private var tarefaId: String? = null
    private val PICK_FILE_REQUEST = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { bundle ->
            tarefaId = bundle.getString("tarefaId")
            val tarefaTitulo = bundle.getString("tarefaTitulo")
            val tarefaDescricao = bundle.getString("tarefaDescricao")
            val tarefaStatus = bundle.getString("tarefaStatus")
            viewModel.inicializarDados(tarefaId, tarefaTitulo, tarefaDescricao, tarefaStatus)
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
        configurarToggleButtons()
        configurarBotoesStatus()
        observarEstado()
        observarDados()
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
        viewModel.carregarArquivos(tarefaId)
    }

    private fun configurarBotoesStatus() {
        binding.btnStatusPendente.setOnClickListener {
            viewModel.alterarStatus(tarefaId, "pendente")
        }
        binding.btnStatusProgresso.setOnClickListener {
            viewModel.alterarStatus(tarefaId, "em_andamento")
        }
        binding.btnStatusConcluida.setOnClickListener {
            viewModel.alterarStatus(tarefaId, "concluida")
        }
    }

    private fun observarEstado() {
        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is TarefaState.Idle -> {}
                is TarefaState.Loading -> {}
                is TarefaState.DadosCarregados -> {
                    atualizarStatusDisplay(state.status)
                    // Mostra o Toast apenas quando o status foi alterado pelo usuário
                    if (state.mostrarMensagem) {
                        Toast.makeText(
                            context,
                            "Status alterado para: ${viewModel.traduzirStatus(state.status)}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                is TarefaState.ArquivosCarregados -> {
                    exibirArquivos(state.arquivos)
                }
                is TarefaState.Error -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    viewModel.limparEstado()
                }
            }
        }
    }

    private fun observarDados() {
        viewModel.titulo.observe(viewLifecycleOwner) { titulo ->
            binding.textViewTaskName.text = titulo
        }

        viewModel.descricao.observe(viewLifecycleOwner) { descricao ->
            binding.textViewDescricao.text = descricao
        }

        viewModel.statusAtual.observe(viewLifecycleOwner) { status ->
            atualizarStatusDisplay(status)
        }
    }

    private fun atualizarStatusDisplay(status: String) {
        binding.textViewStatusAtual.text = "Status Atual: ${viewModel.traduzirStatus(status)}"

        // Desabilita o botão do status atual
        binding.btnStatusPendente.isEnabled = status != "pendente"
        binding.btnStatusProgresso.isEnabled = status != "em_andamento"
        binding.btnStatusConcluida.isEnabled = status != "concluida"
    }

    private fun exibirArquivos(arquivos: List<Arquivo>) {
        binding.arquivosContainer.removeAllViews()

        val btnUpload = MaterialButton(requireContext())
        btnUpload.text = "Adicionar Arquivo"
        btnUpload.setOnClickListener {
            escolherArquivo()
        }
        binding.arquivosContainer.addView(btnUpload)

        if (arquivos.isEmpty()) {
            val tv = TextView(requireContext())
            tv.text = "Nenhum arquivo disponível."
            tv.setPadding(16, 16, 16, 16)
            binding.arquivosContainer.addView(tv)
        } else {
            for (arquivo in arquivos) {
                val itemView = layoutInflater.inflate(
                    R.layout.item_arquivo,
                    binding.arquivosContainer,
                    false
                )
                val imgIcon = itemView.findViewById<ImageView>(R.id.imgFileIcon)
                val txtName = itemView.findViewById<TextView>(R.id.txtFileName)

                txtName.text = arquivo.nome
                imgIcon.setImageResource(getFileIconByMime(arquivo.mimeType))

                itemView.setOnClickListener {
                    abrirArquivo(arquivo.ref)
                }

                binding.arquivosContainer.addView(itemView)
            }
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
                viewModel.uploadArquivo(tarefaId, fileUri)
            }
        }
    }

    private fun abrirArquivo(fileRef: com.google.firebase.storage.StorageReference) {
        viewModel.obterDownloadUrl(fileRef) { resultado ->
            resultado.onSuccess { uri ->
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            }.onFailure {
                Toast.makeText(requireContext(), "Erro ao abrir arquivo", Toast.LENGTH_SHORT).show()
            }
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