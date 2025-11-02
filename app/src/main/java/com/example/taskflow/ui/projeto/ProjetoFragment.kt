package com.example.taskflow.ui.projeto

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taskflow.R
import com.example.taskflow.adapters.EquipesProjetoAdapter
import com.example.taskflow.adapters.MembroAdapter
import com.example.taskflow.databinding.FragmentProjetoBinding
import com.example.taskflow.ui.equipe.criar.CriarEquipeActivity

class ProjetoFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null

    private val binding by lazy {
        FragmentProjetoBinding.inflate(layoutInflater)
    }

    private val viewModel: ProjetoViewModel by viewModels()

    private val equipesAdapter by lazy {
        EquipesProjetoAdapter { equipe ->
            val bundle = Bundle().apply {
                putString("equipeId", equipe.id)
                putString("equipeNome", equipe.nome)
            }
            findNavController().navigate(
                R.id.action_projetoFragment_to_equipeTarefasFragment,
                bundle
            )
        }
    }

    private val membrosAdapter by lazy {
        MembroAdapter(
            projetoId = param1 ?: "",
            onMembroRemovido = {
                val projetoId = param1 ?: return@MembroAdapter
                viewModel.recarregarPermissoes(projetoId)
                // ✅ NÃO PRECISA MAIS: viewModel.carregarMembros(projetoId)
                // O listener em tempo real já atualiza automaticamente!
            },
            onPromoverAdmin = { userId ->
                val projetoId = param1 ?: return@MembroAdapter
                viewModel.promoverParaAdmin(projetoId, userId)
            },
            onRemoverAdmin = { userId ->
                val projetoId = param1 ?: return@MembroAdapter
                viewModel.removerAdmin(projetoId, userId)
            }
        )
    }

    private val criarEquipeLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // ✅ NÃO PRECISA MAIS: viewModel.carregarEquipes(projetoId)
        // O listener em tempo real já detecta a nova equipe automaticamente!
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(context, "Equipe criada com sucesso!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = binding.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvProjetosEquipe.layoutManager = LinearLayoutManager(requireContext())

        val projetoId = param1 ?: return

        // ✅ Iniciar monitoramentos em tempo real
        viewModel.iniciarMonitoramentoPermissoes(projetoId)
        viewModel.carregarInfoProjeto(projetoId)

        configurarEdicao()

        binding.fabCriarProjeto?.setOnClickListener {
            if (!verificarPermissoesAntesDeAcao("criar equipes")) return@setOnClickListener

            val intent = Intent(requireContext(), CriarEquipeActivity::class.java)
            intent.putExtra("projetoId", projetoId)
            criarEquipeLauncher.launch(intent)
        }

        binding.btnCopiarCodigo.setOnClickListener {
            val codigo = viewModel.codigoProjeto.value ?: return@setOnClickListener
            if (codigo.isNotEmpty()) {
                copiarCodigoParaClipboard(codigo)
            }
        }

        binding.btnAtualizarCodigo.setOnClickListener {
            if (!verificarPermissoesAntesDeAcao("gerar um novo código")) return@setOnClickListener
            confirmarGerarNovoCodigo(projetoId)
        }

        // ✅ Configurar adapter inicial e iniciar listener apropriado
        binding.rvProjetosEquipe.adapter = when (binding.toggleGroup.checkedButtonId) {
            R.id.btnMembros -> {
                atualizarVisibilidadeFab()
                viewModel.iniciarMonitoramentoMembros(projetoId)
                membrosAdapter
            }
            else -> {
                atualizarVisibilidadeFab()
                viewModel.iniciarMonitoramentoEquipes(projetoId)
                equipesAdapter
            }
        }

        // ✅ MODIFICADO: Gerenciar listeners ao trocar de aba
        binding.toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener

            binding.rvProjetosEquipe.adapter = when (checkedId) {
                R.id.btnProjetos -> {
                    atualizarVisibilidadeFab()
                    // Parar listener de membros e iniciar de equipes
                    viewModel.pararMonitoramentoMembros()
                    viewModel.iniciarMonitoramentoEquipes(projetoId)
                    equipesAdapter
                }
                R.id.btnMembros -> {
                    binding.fabCriarProjeto?.visibility = View.GONE
                    // Parar listener de equipes e iniciar de membros
                    viewModel.pararMonitoramentoEquipes()
                    viewModel.iniciarMonitoramentoMembros(projetoId)
                    membrosAdapter
                }
                else -> equipesAdapter
            }
        }

        observarEstado()
        observarDados()
        observarPermissoes()
    }

    // ==================== CONFIGURAÇÃO DE EDIÇÃO ====================

    private fun configurarEdicao() {
        val projetoId = param1 ?: return

        binding.textView15.setOnClickListener {
            if (!verificarPermissoesAntesDeAcao("editar o nome")) return@setOnClickListener
            mostrarDialogEditarNome(projetoId)
        }

        binding.btnEditarCor.setOnClickListener {
            if (!verificarPermissoesAntesDeAcao("editar a cor")) return@setOnClickListener
            mostrarDialogEditarCor(projetoId)
        }
    }

    private fun verificarPermissoesAntesDeAcao(acao: String): Boolean {
        val isCreatorOrAdmin = viewModel.isCreator.value == true || viewModel.isAdmin.value == true

        if (!isCreatorOrAdmin) {
            Toast.makeText(
                requireContext(),
                "Você não tem mais permissão para $acao",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        return true
    }

    private fun mostrarDialogEditarNome(projetoId: String) {
        val inputLayout = layoutInflater.inflate(R.layout.dialog_edit_text, null)
        val editText = inputLayout.findViewById<EditText>(R.id.editTextDialog)
        editText.setText(viewModel.nomeProjeto.value)
        editText.hint = "Nome do projeto"
        editText.requestFocus()

        AlertDialog.Builder(requireContext())
            .setTitle("Editar Nome do Projeto")
            .setView(inputLayout)
            .setPositiveButton("Salvar") { _, _ ->
                if (!verificarPermissoesAntesDeAcao("editar o nome")) return@setPositiveButton

                val novoNome = editText.text.toString().trim()
                if (novoNome.isNotEmpty()) {
                    viewModel.atualizarNomeProjeto(projetoId, novoNome)
                } else {
                    Toast.makeText(context, "Nome não pode estar vazio", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogEditarCor(projetoId: String) {
        val cores = arrayOf(
            "Azul" to "#3F51B5",
            "Laranja" to "#FF5722",
            "Verde" to "#4CAF50",
            "Amarelo" to "#FFC107",
            "Rosa" to "#E91E63",
            "Vermelho" to "#E53935",
            "Roxo" to "#8E24AA",
            "Turquesa" to "#00ACC1"
        )

        val nomesCores = cores.map { it.first }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Selecionar Cor do Projeto")
            .setSingleChoiceItems(nomesCores, -1) { dialog, which ->
                if (!verificarPermissoesAntesDeAcao("editar a cor")) {
                    dialog.dismiss()
                    return@setSingleChoiceItems
                }

                val novaCor = cores[which].second
                viewModel.atualizarCorProjeto(projetoId, novaCor)
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ==================== OBSERVADORES ====================

    private fun observarEstado() {
        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ProjetoState.Idle -> {}
                is ProjetoState.Loading -> {}
                is ProjetoState.InfoCarregada -> {
                    binding.textView15.text = state.nomeProjeto

                    if (state.codigoProjeto.isNotEmpty()) {
                        binding.layoutCodigoEquipe.visibility = View.VISIBLE
                        binding.tvCodigoEquipe.text = "Código: ${state.codigoProjeto}"
                        binding.btnAtualizarCodigo.visibility =
                            if (state.isCreator || state.isAdmin) View.VISIBLE else View.GONE
                    } else {
                        binding.layoutCodigoEquipe.visibility = View.GONE
                    }

                    atualizarVisibilidadeFab()
                }
                is ProjetoState.EquipesCarregadas -> {
                    if (state.equipes.isEmpty()) {
                        binding.rvProjetosEquipe.visibility = View.GONE
                        binding.layoutEstadoVazio.visibility = View.VISIBLE
                    } else {
                        binding.rvProjetosEquipe.visibility = View.VISIBLE
                        binding.layoutEstadoVazio.visibility = View.GONE
                        equipesAdapter.atualizarEquipes(state.equipes)
                    }
                }
                is ProjetoState.MembrosCarregados -> {
                    binding.rvProjetosEquipe.visibility = View.VISIBLE
                    binding.layoutEstadoVazio.visibility = View.GONE

                    membrosAdapter.atualizarMembros(
                        state.membros,
                        state.criadorId,
                        state.adminsIds,
                        viewModel.isCreator.value ?: false,
                        viewModel.isAdmin.value ?: false
                    )
                }
                is ProjetoState.CampoAtualizado -> {
                    Toast.makeText(context, state.mensagem, Toast.LENGTH_SHORT).show()
                    viewModel.limparEstado()
                }
                is ProjetoState.UsuarioRemovidoDoProjeto -> {
                    Toast.makeText(
                        requireContext(),
                        "Você foi removido deste projeto",
                        Toast.LENGTH_LONG
                    ).show()
                    findNavController().popBackStack()
                }
                is ProjetoState.PermissoesRevogadas -> {
                    Toast.makeText(
                        requireContext(),
                        "Suas permissões de administrador foram removidas",
                        Toast.LENGTH_SHORT
                    ).show()
                    atualizarVisibilidadeFab()
                    viewModel.limparEstado()
                }
                is ProjetoState.Error -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    viewModel.limparEstado()
                }
            }
        }
    }

    private fun observarDados() {
        viewModel.nomeProjeto.observe(viewLifecycleOwner) { nome ->
            binding.textView15.text = nome
        }

        viewModel.codigoProjeto.observe(viewLifecycleOwner) { codigo ->
            binding.tvCodigoEquipe.text = "Código: $codigo"
        }
    }

    private fun observarPermissoes() {
        var wasAdmin = false

        viewModel.isAdmin.observe(viewLifecycleOwner) { isAdmin ->
            if (wasAdmin && !isAdmin) {
                Toast.makeText(
                    requireContext(),
                    "Suas permissões de administrador foram removidas",
                    Toast.LENGTH_SHORT
                ).show()
            }
            wasAdmin = isAdmin
            atualizarVisibilidadeFab()
        }

        viewModel.isCreator.observe(viewLifecycleOwner) {
            atualizarVisibilidadeFab()
        }

        viewModel.estaNoProjeto.observe(viewLifecycleOwner) { estaNoProjeto ->
            if (estaNoProjeto == false) {
                Toast.makeText(
                    requireContext(),
                    "Você foi removido deste projeto",
                    Toast.LENGTH_LONG
                ).show()
                findNavController().popBackStack()
            }
        }
    }

    private fun atualizarVisibilidadeFab() {
        val isCreatorOrAdmin = viewModel.isCreator.value == true || viewModel.isAdmin.value == true
        val isEquipesTab = binding.toggleGroup.checkedButtonId == R.id.btnProjetos

        binding.fabCriarProjeto?.visibility = if (isEquipesTab && isCreatorOrAdmin) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private fun confirmarGerarNovoCodigo(projetoId: String) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Confirmar alteração")
        builder.setMessage("Tem certeza que deseja gerar um novo código para o projeto?\n\nO código atual ficará inválido e você precisará compartilhar o novo código com os membros.")
        builder.setPositiveButton("Sim, gerar novo") { _, _ ->
            if (!verificarPermissoesAntesDeAcao("gerar um novo código")) return@setPositiveButton

            binding.btnAtualizarCodigo.isEnabled = false
            viewModel.atualizarCodigo(projetoId)
            binding.btnAtualizarCodigo.isEnabled = true
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun copiarCodigoParaClipboard(codigo: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Código do Projeto", codigo)
        clipboard.setPrimaryClip(clip)

        Toast.makeText(
            requireContext(),
            "Código '$codigo' copiado para área de transferência!",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onResume() {
        super.onResume()
        val projetoId = param1 ?: return

        // ✅ Recarregar permissões ao retornar
        viewModel.recarregarPermissoes(projetoId)

        // ✅ NÃO PRECISA MAIS: Listeners em tempo real já mantém tudo atualizado!
        // Removido: viewModel.carregarEquipes(projetoId)
        // Removido: viewModel.carregarMembros(projetoId)
    }

    // ✅ Limpar listeners ao destruir
    override fun onDestroyView() {
        super.onDestroyView()
        val projetoId = param1 ?: return

        // Parar todos os monitoramentos
        viewModel.pararMonitoramentoPermissoes(projetoId)
        viewModel.pararMonitoramentoEquipes()
        viewModel.pararMonitoramentoMembros()
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ProjetoFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"