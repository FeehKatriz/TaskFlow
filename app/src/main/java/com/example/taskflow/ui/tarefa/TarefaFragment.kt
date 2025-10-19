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
    private var equipeNome: String? = null
    private var prioridade: String? = null
    private var prazo: String? = null
    private val PICK_FILE_REQUEST = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { bundle ->
            tarefaId = bundle.getString("tarefaId")
            val tarefaTitulo = bundle.getString("tarefaTitulo")
            val tarefaDescricao = bundle.getString("tarefaDescricao")
            val tarefaStatus = bundle.getString("tarefaStatus")
            equipeNome = bundle.getString("equipeNome", "Equipe Desconhecida")
            prioridade = bundle.getString("prioridade", "media") // baixa, media, alta
            prazo = bundle.getString("dataVencimento", "")

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
        configurarHeader()
        configurarToggleButtons()
        configurarBotoesStatus()
        observarEstado()
        observarDados()
    }

    private fun configurarHeader() {
        // Configurar equipe
        binding.textViewEquipe.text = "Equipe: $equipeNome"

        // Configurar chip de prioridade
        val (corPrioridade, textoPrioridade) = when (prioridade?.uppercase()) {
            "ALTA" -> Pair("#E53935", "ALTA")
            "MÉDIA", "MEDIA" -> Pair("#FB8C00", "MÉDIA")
            "BAIXA" -> Pair("#43A047", "BAIXA")
            else -> Pair("#757575", "NORMAL")
        }
        binding.chipPrioridade.text = textoPrioridade
        binding.chipPrioridade.setChipBackgroundColorResource(android.R.color.transparent)
        binding.chipPrioridade.chipBackgroundColor = android.content.res.ColorStateList.valueOf(
            android.graphics.Color.parseColor(corPrioridade)
        )

        // Configurar prazo
        binding.textViewPrazo.text = prazo ?: "Sem prazo definido"

        // TODO: Calcular e exibir status do prazo (faltam X dias, atrasado, etc)
        calcularStatusPrazo(prazo)
    }

    private fun calcularStatusPrazo(prazo: String?) {
        if (prazo.isNullOrEmpty() || prazo == "Sem prazo definido") {
            binding.textViewPrazoStatus.text = "Sem prazo definido"
            binding.textViewPrazoStatus.setTextColor(
                android.graphics.Color.parseColor("#999999")
            )
            return
        }

        // TODO: Implementar cálculo real de dias restantes
        // Por enquanto, apenas exibe uma mensagem padrão
        binding.textViewPrazoStatus.text = "Prazo definido"
        binding.textViewPrazoStatus.setTextColor(
            android.graphics.Color.parseColor("#4CAF50")
        )
    }

    private fun configurarToggleButtons() {
        binding.toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnDetalhes -> mostrarDetalhes()
                    R.id.btnArquivos -> mostrarArquivos()
                    R.id.btnComentarios -> mostrarComentarios()
                }
            }
        }
        mostrarDetalhes()
    }

    private fun mostrarDetalhes() {
        binding.detalhesContainer.visibility = View.VISIBLE
        binding.arquivosContainer.visibility = View.GONE
        binding.comentariosContainer.visibility = View.GONE
    }

    private fun mostrarArquivos() {
        binding.detalhesContainer.visibility = View.GONE
        binding.arquivosContainer.visibility = View.VISIBLE
        binding.comentariosContainer.visibility = View.GONE
        viewModel.carregarArquivos(tarefaId)
    }

    private fun mostrarComentarios() {
        binding.detalhesContainer.visibility = View.GONE
        binding.arquivosContainer.visibility = View.GONE
        binding.comentariosContainer.visibility = View.VISIBLE

        // TODO: Implementar lógica de comentários
        Toast.makeText(
            requireContext(),
            "Funcionalidade de comentários em desenvolvimento",
            Toast.LENGTH_SHORT
        ).show()
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

        // Ajusta opacidade dos botões desabilitados
        binding.btnStatusPendente.alpha = if (status == "pendente") 0.5f else 1.0f
        binding.btnStatusProgresso.alpha = if (status == "em_andamento") 0.5f else 1.0f
        binding.btnStatusConcluida.alpha = if (status == "concluida") 0.5f else 1.0f
    }

    private fun exibirArquivos(arquivos: List<Arquivo>) {
        binding.arquivosContainer.removeAllViews()

        // Botão para adicionar arquivo
        val btnUpload = MaterialButton(requireContext()).apply {
            text = "📎 Adicionar Arquivo"
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(android.graphics.Color.parseColor("#4285F4"))
            setPadding(32, 24, 32, 24)
            setOnClickListener { escolherArquivo() }
        }
        binding.arquivosContainer.addView(btnUpload)

        if (arquivos.isEmpty()) {
            val tv = TextView(requireContext()).apply {
                text = "Nenhum arquivo anexado ainda.\nClique no botão acima para adicionar."
                textSize = 16f
                setTextColor(android.graphics.Color.parseColor("#888888"))
                gravity = android.view.Gravity.CENTER
                setPadding(32, 64, 32, 32)
            }
            binding.arquivosContainer.addView(tv)
        } else {
            // Título da seção
            val tvTitulo = TextView(requireContext()).apply {
                text = "Arquivos Anexados (${arquivos.size})"
                textSize = 18f
                setTextColor(android.graphics.Color.parseColor("#333333"))
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(0, 32, 0, 16)
            }
            binding.arquivosContainer.addView(tvTitulo)

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
                Toast.makeText(requireContext(), "Enviando arquivo...", Toast.LENGTH_SHORT).show()
                viewModel.uploadArquivo(tarefaId, fileUri)
            }
        }
    }

    private fun abrirArquivo(fileRef: com.google.firebase.storage.StorageReference) {
        viewModel.obterDownloadUrl(fileRef) { resultado ->
            resultado.onSuccess { uri ->
                val intent = Intent(Intent.ACTION_VIEW, uri)
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(
                        requireContext(),
                        "Nenhum aplicativo disponível para abrir este arquivo",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }.onFailure {
                Toast.makeText(
                    requireContext(),
                    "Erro ao abrir arquivo",
                    Toast.LENGTH_SHORT
                ).show()
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