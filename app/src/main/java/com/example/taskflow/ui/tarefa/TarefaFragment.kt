package com.example.taskflow.ui.tarefa

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taskflow.R
import com.example.taskflow.databinding.FragmentTarefaBinding
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

class TarefaFragment : Fragment() {

    private var _binding: FragmentTarefaBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TarefaViewModel by viewModels()
    private var tarefaId: String? = null
    private var equipeNome: String? = null
    private val PICK_FILE_REQUEST = 200
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault())

    private lateinit var commentAdapter: CommentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { bundle ->
            tarefaId = bundle.getString("tarefaId")
            val tarefaTitulo = bundle.getString("tarefaTitulo")
            val tarefaDescricao = bundle.getString("tarefaDescricao")
            val tarefaStatus = bundle.getString("tarefaStatus")
            equipeNome = bundle.getString("equipeNome", "Equipe Desconhecida")

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
        configurarComentarios()
        configurarEdicao() // ✅ NOVO
        configurarExclusao() // ✅ NOVO
        observarEstado()
        observarDados()
    }

    // ==================== CONFIGURAÇÃO DE EDIÇÃO (NOVO) ====================

    private fun configurarEdicao() {
        // Edição de Título
        binding.textViewTaskName.setOnClickListener {
            mostrarDialogEditarTitulo()
        }

        // Edição de Descrição
        binding.cardDescricao.setOnClickListener {
            mostrarDialogEditarDescricao()
        }

        // Edição de Prazo
        binding.cardPrazo.setOnClickListener {
            mostrarDateTimePicker()
        }

        // Edição de Prioridade
        binding.chipPrioridade.setOnClickListener {
            mostrarDialogPrioridade()
        }

        // Edição de Responsáveis
        binding.cardResponsaveis.setOnClickListener {
            viewModel.carregarMembrosEquipe()
        }
    }

    private fun mostrarDialogEditarTitulo() {
        val inputLayout = layoutInflater.inflate(R.layout.dialog_edit_text, null)
        val editText = inputLayout.findViewById<EditText>(R.id.editTextDialog)
        editText.setText(viewModel.titulo.value)
        editText.hint = "Título da tarefa"
        editText.requestFocus()

        AlertDialog.Builder(requireContext())
            .setTitle("Editar Título")
            .setView(inputLayout)
            .setPositiveButton("Salvar") { _, _ ->
                val novoTitulo = editText.text.toString().trim()
                if (novoTitulo.isNotEmpty()) {
                    viewModel.atualizarTitulo(tarefaId, novoTitulo)
                } else {
                    Toast.makeText(context, "Título não pode estar vazio", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogEditarDescricao() {
        val inputLayout = layoutInflater.inflate(R.layout.dialog_edit_text, null)
        val editText = inputLayout.findViewById<EditText>(R.id.editTextDialog)
        editText.setText(viewModel.descricao.value)
        editText.hint = "Descrição da tarefa"
        editText.minLines = 4
        editText.maxLines = 8
        editText.requestFocus()

        AlertDialog.Builder(requireContext())
            .setTitle("Editar Descrição")
            .setView(inputLayout)
            .setPositiveButton("Salvar") { _, _ ->
                val novaDescricao = editText.text.toString().trim()
                viewModel.atualizarDescricao(tarefaId, novaDescricao)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDateTimePicker() {
        val calendar = Calendar.getInstance()

        // Parse prazo atual se existir
        try {
            val prazoAtual = viewModel.prazo.value
            if (!prazoAtual.isNullOrEmpty() && prazoAtual != "Sem prazo definido") {
                val dataPrazo = dateFormat.parse(prazoAtual)
                if (dataPrazo != null) {
                    calendar.time = dataPrazo
                }
            }
        } catch (e: Exception) {
            // Usa data/hora atual se houver erro ao parsear
        }

        // DatePicker
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                // TimePicker
                TimePickerDialog(
                    requireContext(),
                    { _, hour, minute ->
                        calendar.set(year, month, day, hour, minute)
                        val prazoFormatado = dateFormat.format(calendar.time)
                        viewModel.atualizarPrazo(tarefaId, prazoFormatado)
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
            // Opção para remover prazo
            setButton(DatePickerDialog.BUTTON_NEUTRAL, "Sem prazo") { _, _ ->
                viewModel.atualizarPrazo(tarefaId, null)
            }
        }.show()
    }

    private fun mostrarDialogPrioridade() {
        val opcoes = arrayOf("Alta", "Média", "Baixa")
        val prioridadeAtual = when (viewModel.prioridade.value?.lowercase()) {
            "alta" -> 0
            "media", "média" -> 1
            "baixa" -> 2
            else -> 1
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Selecionar Prioridade")
            .setSingleChoiceItems(opcoes, prioridadeAtual) { dialog, which ->
                val prioridade = when (which) {
                    0 -> "alta"
                    1 -> "media"
                    2 -> "baixa"
                    else -> "media"
                }
                viewModel.atualizarPrioridade(tarefaId, prioridade)
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogResponsaveis(membros: List<Pair<String, String>>) {
        if (membros.isEmpty()) {
            Toast.makeText(context, "Nenhum membro disponível na equipe", Toast.LENGTH_SHORT).show()
            return
        }

        val responsaveisAtuais = viewModel.responsaveis.value ?: emptyList()
        val selecionados = BooleanArray(membros.size) { index ->
            membros[index].first in responsaveisAtuais
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Selecionar Responsáveis")
            .setMultiChoiceItems(
                membros.map { it.second }.toTypedArray(),
                selecionados
            ) { _, which, isChecked ->
                selecionados[which] = isChecked
            }
            .setPositiveButton("Salvar") { _, _ ->
                val novosResponsaveis = membros.filterIndexed { index, _ ->
                    selecionados[index]
                }.map { it.first }
                viewModel.atualizarResponsaveis(tarefaId, novosResponsaveis)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ==================== CONFIGURAÇÃO DE EXCLUSÃO (NOVO) ====================

    private fun configurarExclusao() {
        binding.btnDelete.setOnClickListener {
            mostrarDialogExcluir()
        }
    }

    private fun mostrarDialogExcluir() {
        AlertDialog.Builder(requireContext())
            .setTitle("Excluir Tarefa")
            .setMessage("Esta ação não pode ser desfeita. Deseja continuar?")
            .setPositiveButton("Excluir") { _, _ ->
                viewModel.excluirTarefa(tarefaId)
            }
            .setNegativeButton("Cancelar", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    // ==================== CONFIGURAÇÕES ORIGINAIS ====================

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

    private fun mostrarComentarios() {
        binding.detalhesContainer.visibility = View.GONE
        binding.arquivosContainer.visibility = View.GONE
        binding.comentariosContainer.visibility = View.VISIBLE

        // ✅ NOVO: Marca que está visualizando comentários
        tarefaId?.let { id ->
            viewModel.marcarVisualizandoComentarios(id)
        }

        viewModel.carregarComentarios(tarefaId)
    }

    private fun mostrarDetalhes() {
        binding.detalhesContainer.visibility = View.VISIBLE
        binding.arquivosContainer.visibility = View.GONE
        binding.comentariosContainer.visibility = View.GONE

        // ✅ NOVO: Remove visualização ao sair dos comentários
        tarefaId?.let { id ->
            viewModel.removerVisualizacaoComentarios(id)
        }
    }

    private fun mostrarArquivos() {
        binding.detalhesContainer.visibility = View.GONE
        binding.arquivosContainer.visibility = View.VISIBLE
        binding.comentariosContainer.visibility = View.GONE

        // ✅ NOVO: Remove visualização ao sair dos comentários
        tarefaId?.let { id ->
            viewModel.removerVisualizacaoComentarios(id)
        }

        viewModel.carregarArquivos(tarefaId)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // ✅ NOVO: Remove visualização ao sair da tela
        tarefaId?.let { id ->
            viewModel.removerVisualizacaoComentarios(id)
        }

        _binding = null
    }

    private fun configurarComentarios() {
        commentAdapter = CommentAdapter { comment ->
            viewModel.deletarComentario(tarefaId, comment.id)
        }

        binding.recyclerViewComentarios.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = commentAdapter
        }

        binding.btnEnviarComentario.setOnClickListener {
            val mensagem = binding.editTextComentario.text.toString()

            if (mensagem.isNotBlank()) {
                viewModel.adicionarComentario(tarefaId, mensagem)
                binding.editTextComentario.text?.clear()
            } else {
                Toast.makeText(context, "Digite uma mensagem", Toast.LENGTH_SHORT).show()
            }
        }
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
                is TarefaState.Idle -> {
                    // Estado inicial
                }
                is TarefaState.Loading -> {
                    // Mostrar loading se necessário
                }
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
                is TarefaState.ArquivoEnviado -> {
                    Toast.makeText(
                        context,
                        "Arquivo ${state.nomeArquivo} enviado com sucesso!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is TarefaState.ComentariosCarregados -> {
                    // Comentários são observados diretamente pelo LiveData
                }
                // ✅ NOVOS ESTADOS
                is TarefaState.CampoAtualizado -> {
                    Toast.makeText(context, state.campo, Toast.LENGTH_SHORT).show()
                    viewModel.limparEstado()
                }
                is TarefaState.TarefaExcluida -> {
                    Toast.makeText(context, "Tarefa excluída com sucesso", Toast.LENGTH_SHORT).show()
                    // Voltar para tela anterior
                    requireActivity().onBackPressed()
                }
                is TarefaState.MembrosEquipeCarregados -> {
                    mostrarDialogResponsaveis(state.membros)
                    viewModel.limparEstado()
                }
                is TarefaState.Error -> {
                    Toast.makeText(
                        requireContext(),
                        state.message,
                        Toast.LENGTH_LONG
                    ).show()
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

        viewModel.prazo.observe(viewLifecycleOwner) { prazo ->
            configurarPrazo(prazo)
        }

        viewModel.prioridade.observe(viewLifecycleOwner) { prioridade ->
            configurarPrioridade(prioridade)
        }

        viewModel.responsaveis.observe(viewLifecycleOwner) { responsaveis ->
            configurarResponsaveis(responsaveis)
        }

        viewModel.responsaveisNomes.observe(viewLifecycleOwner) { nomes ->
            if (nomes.isNotEmpty()) {
                exibirNomesResponsaveis(nomes)
            }
        }

        viewModel.equipeNome.observe(viewLifecycleOwner) { nomeEquipe ->
            if (nomeEquipe != null) {
                binding.textViewEquipe.text = "Equipe: $nomeEquipe"
            }
        }

        viewModel.comentarios.observe(viewLifecycleOwner) { comentarios ->
            commentAdapter.submitList(comentarios)
        }
    }

    private fun configurarPrazo(prazo: String?) {
        if (prazo.isNullOrEmpty()) {
            binding.textViewPrazo.text = "Sem prazo definido"
            binding.textViewPrazoStatus.text = ""
            binding.textViewPrazoStatus.visibility = View.GONE
        } else {
            binding.textViewPrazo.text = prazo
            calcularStatusPrazo(prazo)
        }
    }

    private fun calcularStatusPrazo(prazo: String?) {
        if (prazo.isNullOrEmpty() || prazo == "Sem prazo definido") {
            binding.textViewPrazoStatus.text = ""
            binding.textViewPrazoStatus.visibility = View.GONE
            return
        }

        binding.textViewPrazoStatus.visibility = View.VISIBLE

        try {
            val dataPrazo = dateFormat.parse(prazo)
            val hoje = Calendar.getInstance().time

            if (dataPrazo != null) {
                val diffMillis = dataPrazo.time - hoje.time
                val diasRestantes = (diffMillis / (1000 * 60 * 60 * 24)).toInt()

                when {
                    diasRestantes < 0 -> {
                        binding.textViewPrazoStatus.text = "⚠️ Atrasado há ${-diasRestantes} dia(s)"
                        binding.textViewPrazoStatus.setTextColor(
                            android.graphics.Color.parseColor("#E53935")
                        )
                    }
                    diasRestantes == 0 -> {
                        binding.textViewPrazoStatus.text = "⏰ Vence hoje!"
                        binding.textViewPrazoStatus.setTextColor(
                            android.graphics.Color.parseColor("#FB8C00")
                        )
                    }
                    diasRestantes <= 3 -> {
                        binding.textViewPrazoStatus.text = "⚡ Faltam $diasRestantes dia(s)"
                        binding.textViewPrazoStatus.setTextColor(
                            android.graphics.Color.parseColor("#FB8C00")
                        )
                    }
                    else -> {
                        binding.textViewPrazoStatus.text = "✓ Faltam $diasRestantes dia(s)"
                        binding.textViewPrazoStatus.setTextColor(
                            android.graphics.Color.parseColor("#4CAF50")
                        )
                    }
                }
            }
        } catch (e: Exception) {
            binding.textViewPrazoStatus.text = "Prazo definido"
            binding.textViewPrazoStatus.setTextColor(
                android.graphics.Color.parseColor("#4CAF50")
            )
        }
    }

    private fun configurarPrioridade(prioridade: String?) {
        val (corPrioridade, textoPrioridade) = when (prioridade?.lowercase()) {
            "alta" -> Pair("#E53935", "ALTA")
            "média", "media" -> Pair("#FB8C00", "MÉDIA")
            "baixa" -> Pair("#43A047", "BAIXA")
            else -> Pair("#757575", "NORMAL")
        }

        binding.chipPrioridade.text = textoPrioridade
        binding.chipPrioridade.setChipBackgroundColorResource(android.R.color.transparent)
        binding.chipPrioridade.chipBackgroundColor = android.content.res.ColorStateList.valueOf(
            android.graphics.Color.parseColor(corPrioridade)
        )
    }

    private fun configurarResponsaveis(responsaveis: List<String>) {
        if (responsaveis.isEmpty()) {
            binding.textViewResponsaveis?.text = "Nenhum responsável atribuído"
        } else {
            binding.textViewResponsaveis?.text = "👥 ${responsaveis.size} responsável(is)"
        }
    }

    private fun exibirNomesResponsaveis(nomes: List<String>) {
        if (nomes.isEmpty()) {
            binding.textViewResponsaveis?.text = "Nenhum responsável atribuído"
        } else {
            val nomesFormatados = nomes.joinToString(", ")
            binding.textViewResponsaveis?.text = "👥 $nomesFormatados"
        }
    }

    private fun atualizarStatusDisplay(status: String) {
        binding.textViewStatusAtual.text = "Status Atual: ${viewModel.traduzirStatus(status)}"

        binding.btnStatusPendente.isEnabled = status != "pendente"
        binding.btnStatusProgresso.isEnabled = status != "em_andamento"
        binding.btnStatusConcluida.isEnabled = status != "concluida"

        binding.btnStatusPendente.alpha = if (status == "pendente") 0.5f else 1.0f
        binding.btnStatusProgresso.alpha = if (status == "em_andamento") 0.5f else 1.0f
        binding.btnStatusConcluida.alpha = if (status == "concluida") 0.5f else 1.0f
    }

    private fun exibirArquivos(arquivos: List<Arquivo>) {
        binding.arquivosContainer.removeAllViews()

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

    /*override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }*/
}