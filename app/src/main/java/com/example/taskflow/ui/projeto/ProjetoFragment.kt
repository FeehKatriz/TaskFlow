package com.example.taskflow.ui.projeto

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.example.taskflow.data.model.Equipe
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
                viewModel.carregarMembros(projetoId)
                viewModel.verificarSeUsuarioEstaNoProjeto(projetoId) { estaNoProjeto ->
                    if (!estaNoProjeto) {
                        Toast.makeText(
                            requireContext(),
                            "Você foi removido deste projeto",
                            Toast.LENGTH_SHORT
                        ).show()
                        findNavController().popBackStack()
                    }
                }
            }
        )
    }

    private val criarEquipeLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val projetoId = param1 ?: return@registerForActivityResult
            viewModel.carregarEquipes(projetoId)
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
        viewModel.carregarInfoProjeto(projetoId)

        binding.fabCriarProjeto?.setOnClickListener {
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
            if (viewModel.isCreator.value == true) {
                confirmarGerarNovoCodigo(projetoId)
            } else {
                Toast.makeText(
                    requireContext(),
                    "Apenas o criador do projeto pode gerar um novo código",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.rvProjetosEquipe.adapter = when (binding.toggleGroup.checkedButtonId) {
            R.id.btnMembros -> {
                binding.fabCriarProjeto?.visibility = View.GONE
                viewModel.carregarMembros(projetoId)
                membrosAdapter
            }
            else -> {
                binding.fabCriarProjeto?.visibility = View.VISIBLE
                viewModel.carregarEquipes(projetoId)
                equipesAdapter
            }
        }

        binding.toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            binding.rvProjetosEquipe.adapter = when (checkedId) {
                R.id.btnProjetos -> {
                    binding.fabCriarProjeto?.visibility = View.VISIBLE
                    viewModel.carregarEquipes(projetoId)
                    equipesAdapter
                }
                R.id.btnMembros -> {
                    binding.fabCriarProjeto?.visibility = View.GONE
                    viewModel.carregarMembros(projetoId)
                    membrosAdapter
                }
                else -> equipesAdapter
            }
        }

        observarEstado()
        observarDados()
    }

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
                        binding.btnAtualizarCodigo.visibility = if (state.isCreator) View.VISIBLE else View.GONE
                    } else {
                        binding.layoutCodigoEquipe.visibility = View.GONE
                    }
                }
                is ProjetoState.EquipesCarregadas -> {
                    equipesAdapter.atualizarEquipes(state.equipes)
                }
                is ProjetoState.MembrosCarregados -> {
                    membrosAdapter.atualizarMembros(state.membros)
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

    private fun confirmarGerarNovoCodigo(projetoId: String) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Confirmar alteração")
        builder.setMessage("Tem certeza que deseja gerar um novo código para o projeto?\n\nO código atual ficará inválido e você precisará compartilhar o novo código com os membros.")
        builder.setPositiveButton("Sim, gerar novo") { _, _ ->
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
        when (binding.toggleGroup.checkedButtonId) {
            R.id.btnProjetos -> viewModel.carregarEquipes(projetoId)
            R.id.btnMembros -> viewModel.carregarMembros(projetoId)
        }
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