package com.example.taskflow.ui.equipe.tarefa

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taskflow.R
import com.example.taskflow.adapters.TarefasAdapter
import com.example.taskflow.databinding.FragmentEquipeTarefaBinding
import com.example.taskflow.dialogs.GerenciarMembrosBottomSheet
import com.example.taskflow.data.model.Tarefa
import com.example.taskflow.ui.tarefa.criar.CriarTarefaActivity

class EquipeTarefaFragment : Fragment() {

    private var equipeId: String? = null
    private var projetoId: String? = null

    private var _binding: FragmentEquipeTarefaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EquipeTarefaViewModel by viewModels()

    private lateinit var adapterAndamento: TarefasAdapter
    private lateinit var adapterFinalizadas: TarefasAdapter
    private lateinit var adapterAComecar: TarefasAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            equipeId = it.getString(ARG_EQUIPE_ID)
            projetoId = it.getString(ARG_PROJETO_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEquipeTarefaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configurarAdapters()
        configurarFABs()
        configurarEdicao()
        observarEstado()
        observarPermissoes() // ✅ NOVO

        // Inicializar dados
        val eId = equipeId ?: return
        viewModel.inicializarDados(eId, projetoId)
    }

    // ==================== OBSERVAR PERMISSÕES (NOVO) ====================

    private fun observarPermissoes() {
        // ✅ Observar se usuário pode editar
        viewModel.podeEditar.observe(viewLifecycleOwner) { podeEditar ->
            configurarVisibilidadeControles(podeEditar)
        }

        // Informações de debug (remover em produção)
        viewModel.isCreator.observe(viewLifecycleOwner) { isCreator ->
            // Log.d("EquipeTarefaFragment", "É criador: $isCreator")
        }

        viewModel.isAdmin.observe(viewLifecycleOwner) { isAdmin ->
            // Log.d("EquipeTarefaFragment", "É admin: $isAdmin")
        }
    }

    /**
     * ✅ Configura visibilidade dos controles de edição baseado em permissões
     */
    private fun configurarVisibilidadeControles(podeEditar: Boolean) {
        if (podeEditar) {
            // ✅ ADMIN/CRIADOR: Mostra todos os controles
            binding.tvEquipeName.isEnabled = true
            binding.btnExcluirEquipe.visibility = View.VISIBLE
            binding.fabGerenciarMembros.visibility = View.VISIBLE

            // Adicionar indicador visual de que pode editar
            binding.tvEquipeName.alpha = 1.0f
        } else {
            // ❌ MEMBRO: Desabilita edição
            binding.tvEquipeName.isEnabled = false
            binding.btnExcluirEquipe.visibility = View.GONE
            binding.fabGerenciarMembros.visibility = View.GONE

            // Indicador visual de que não pode editar
            binding.tvEquipeName.alpha = 0.7f
        }
    }

    // ==================== CONFIGURAÇÃO DE EDIÇÃO ====================

    private fun configurarEdicao() {
        // Edição do nome da equipe
        binding.tvEquipeName.setOnClickListener {
            // ✅ Verificar permissão antes de mostrar dialog
            if (!viewModel.verificarSeUsuarioPodeEditar()) {
                Toast.makeText(
                    context,
                    "Apenas criadores e administradores podem editar equipes",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            mostrarDialogEditarNome()
        }

        // Exclusão da equipe
        binding.btnExcluirEquipe.setOnClickListener {
            // ✅ Verificar permissão antes de mostrar dialog
            if (!viewModel.verificarSeUsuarioPodeEditar()) {
                Toast.makeText(
                    context,
                    "Apenas criadores e administradores podem excluir equipes",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            mostrarDialogExcluirEquipe()
        }
    }

    private fun mostrarDialogEditarNome() {
        val eId = equipeId ?: return

        val inputLayout = layoutInflater.inflate(R.layout.dialog_edit_text, null)
        val editText = inputLayout.findViewById<EditText>(R.id.editTextDialog)
        editText.setText(viewModel.nomeEquipe.value)
        editText.hint = "Nome da equipe"
        editText.requestFocus()

        AlertDialog.Builder(requireContext())
            .setTitle("Editar Nome da Equipe")
            .setView(inputLayout)
            .setPositiveButton("Salvar") { _, _ ->
                val novoNome = editText.text.toString().trim()
                if (novoNome.isNotEmpty()) {
                    viewModel.atualizarNomeEquipe(eId, novoNome)
                } else {
                    Toast.makeText(context, "Nome não pode estar vazio", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogExcluirEquipe() {
        val eId = equipeId ?: return

        AlertDialog.Builder(requireContext())
            .setTitle("Excluir Equipe")
            .setMessage("Esta ação não pode ser desfeita.\n\nTodas as tarefas desta equipe também serão excluídas permanentemente.\n\nDeseja continuar?")
            .setPositiveButton("Excluir") { _, _ ->
                viewModel.excluirEquipe(eId)
            }
            .setNegativeButton("Cancelar", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    // ==================== CONFIGURAÇÕES ORIGINAIS ====================

    private fun configurarAdapters() {
        // Adapter para tarefas em andamento
        adapterAndamento = TarefasAdapter { tarefa ->
            navegarParaTarefa(tarefa)
        }

        binding.rvTarefasAndamento.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = adapterAndamento
        }

        // Adapter para tarefas a começar
        adapterAComecar = TarefasAdapter { tarefa ->
            navegarParaTarefa(tarefa)
        }

        binding.rvTarefasAComecar.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = adapterAComecar
        }

        // Adapter para tarefas finalizadas
        adapterFinalizadas = TarefasAdapter { tarefa ->
            navegarParaTarefa(tarefa)
        }

        binding.rvTarefasFinalizadas.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = adapterFinalizadas
        }
    }

    private fun observarEstado() {
        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is EquipeTarefaState.Idle -> {
                    // Sem carregamento
                }
                is EquipeTarefaState.Loading -> {
                    // Mostrar loading se necessário
                }
                is EquipeTarefaState.Success -> {
                    // Atualizar adapters com as tarefas organizadas
                    adapterAndamento.updateTarefas(state.tarefas.tarefasAndamento)
                    adapterAComecar.updateTarefas(state.tarefas.tarefasPendentes)
                    adapterFinalizadas.updateTarefas(state.tarefas.tarefasConcluidas)
                }
                is EquipeTarefaState.NomeAtualizado -> {
                    Toast.makeText(context, "Nome atualizado com sucesso", Toast.LENGTH_SHORT).show()
                    viewModel.limparEstado()
                }
                is EquipeTarefaState.EquipeExcluida -> {
                    Toast.makeText(context, "Equipe excluída com sucesso", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
                is EquipeTarefaState.Error -> {
                    Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                    viewModel.limparEstado()
                }
            }
        }

        viewModel.projetoId.observe(viewLifecycleOwner) { pId ->
            projetoId = pId
        }

        // Observar nome da equipe
        viewModel.nomeEquipe.observe(viewLifecycleOwner) { nome ->
            binding.tvEquipeName.text = nome
        }
    }

    private fun navegarParaTarefa(tarefa: Tarefa) {
        val bundle = Bundle().apply {
            putString("tarefaId", tarefa.id)
            putString("tarefaTitulo", tarefa.titulo)
            putString("tarefaDescricao", tarefa.descricao)
            putString("tarefaEquipeId", tarefa.equipeId)
            putString("tarefaProjetoId", tarefa.projetoId)
            putString("tarefaCriadoPor", tarefa.criadoPor)
            putString("tarefaStatus", tarefa.status)
            putString("tarefaPrioridade", tarefa.prioridade)
            putString("tarefaDataVencimento", tarefa.dataVencimento)
            putLong("tarefaDataCriacao", tarefa.dataCriacao)
            putStringArrayList("tarefaAnexos", ArrayList(tarefa.anexos))
        }
        findNavController().navigate(R.id.action_equipeTarefasFragment_to_tarefaFragment, bundle)
    }

    private fun configurarFABs() {
        // FAB para criar nova tarefa
        binding.fabCriarTarefa.setOnClickListener {
            val intent = Intent(requireContext(), CriarTarefaActivity::class.java)
            intent.putExtra("equipeId", equipeId)
            intent.putExtra("projetoId", projetoId)
            startActivity(intent)
        }

        // FAB para gerenciar membros da equipe
        binding.fabGerenciarMembros.setOnClickListener {
            // ✅ Verificar permissão antes de abrir
            if (!viewModel.verificarSeUsuarioPodeEditar()) {
                Toast.makeText(
                    requireContext(),
                    "Apenas criadores e administradores podem gerenciar membros",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            abrirGerenciadorMembros()
        }
    }

    private fun abrirGerenciadorMembros() {
        val eId = equipeId ?: run {
            Toast.makeText(requireContext(), "ID da equipe não encontrado", Toast.LENGTH_SHORT).show()
            return
        }

        val pId = projetoId ?: run {
            Toast.makeText(requireContext(), "ID do projeto não encontrado", Toast.LENGTH_SHORT).show()
            return
        }

        val bottomSheet = GerenciarMembrosBottomSheet.Companion.newInstance(
            equipeId = eId,
            projetoId = pId,
            onMembrosAtualizados = {
                Toast.makeText(
                    requireContext(),
                    "Membros da equipe atualizados!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )

        bottomSheet.show(childFragmentManager, "GerenciarMembrosBottomSheet")
    }

    override fun onResume() {
        super.onResume()
        // Recarregar tarefas quando voltar para o fragment
        val eId = equipeId ?: return
        viewModel.carregarTarefas(eId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance(equipeId: String, projetoId: String = "") =
            EquipeTarefaFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_EQUIPE_ID, equipeId)
                    putString(ARG_PROJETO_ID, projetoId)
                }
            }
    }
}

private const val ARG_EQUIPE_ID = "equipeId"
private const val ARG_PROJETO_ID = "projetoId"