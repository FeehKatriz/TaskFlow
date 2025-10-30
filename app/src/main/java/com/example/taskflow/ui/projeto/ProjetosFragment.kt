package com.example.taskflow.ui.projeto

import android.app.Activity
import android.content.Intent
import android.os.Bundle
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
import com.example.taskflow.adapters.ProjetosAdapter
import com.example.taskflow.databinding.DialogEntrarProjetoBinding
import com.example.taskflow.databinding.FragmentPrincipalProjetoBinding
import com.example.taskflow.ui.projeto.criar.CriarProjetoActivity

class ProjetosFragment : Fragment() {

    private val binding by lazy {
        FragmentPrincipalProjetoBinding.inflate(layoutInflater)
    }

    private val viewModel: ProjetosViewModel by viewModels()

    private lateinit var projetosAdapter: ProjetosAdapter

    private val criarProjetoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Projeto foi criado com sucesso, recarregar lista
            viewModel.carregarProjetos()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = binding.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupFabs()
        observarEstado()
        observarEntrarProjeto()

        // Carregar projetos na primeira vez
        viewModel.carregarProjetos()
    }

    private fun setupRecyclerView() {
        projetosAdapter = ProjetosAdapter { projeto ->
            val bundle = Bundle().apply {
                putString("param1", projeto.id)
            }
            findNavController().navigate(
                R.id.action_projetosFragment_to_projetoFragment,
                bundle
            )
        }

        binding.rvProjetos.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = projetosAdapter
        }
    }

    private fun setupFabs() {
        binding.fabCriarProjeto.setOnClickListener {
            val intent = Intent(requireContext(), CriarProjetoActivity::class.java)
            criarProjetoLauncher.launch(intent)
        }

        binding.fabEntrarProjeto.setOnClickListener {
            showEntrarProjetoDialog()
        }
    }

    private fun observarEstado() {
        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ProjetosState.Idle -> {
                    // Sem carregamento
                }
                is ProjetosState.Loading -> {
                    // Mostrar loading se necessário
                }
                is ProjetosState.Success -> {
                    if (state.projetos.isEmpty()) {
                        mostrarEstadoVazio()
                    } else {
                        mostrarProjetos(state.projetos)
                    }
                }
                is ProjetosState.Error -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    viewModel.limparEstado()
                }
            }
        }
    }

    private fun mostrarProjetos(projetos: List<com.example.taskflow.data.model.Projeto>) {
        binding.rvProjetos.visibility = View.VISIBLE
        binding.layoutEstadoVazio.visibility = View.GONE
        projetosAdapter.atualizarProjetos(projetos)
    }

    private fun mostrarEstadoVazio() {
        binding.rvProjetos.visibility = View.GONE
        binding.layoutEstadoVazio.visibility = View.VISIBLE
    }

    private fun observarEntrarProjeto() {
        viewModel.entrarProjetoState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ProjetosState.Idle -> {
                    // Sem carregamento
                }
                is ProjetosState.Loading -> {
                    // Mostrar loading se necessário
                }
                is ProjetosState.Success -> {
                    Toast.makeText(
                        requireContext(),
                        "Você entrou no projeto com sucesso!",
                        Toast.LENGTH_SHORT
                    ).show()
                    viewModel.limparEstadoEntrarProjeto()
                }
                is ProjetosState.Error -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    viewModel.limparEstadoEntrarProjeto()
                }
            }
        }
    }

    private fun showEntrarProjetoDialog() {
        val dialogBinding = DialogEntrarProjetoBinding.inflate(layoutInflater)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCancelar.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnEntrar.setOnClickListener {
            val codigo = dialogBinding.etCodigoProjeto.text.toString().trim()
            viewModel.entrarNoProjeto(codigo)
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        if (::projetosAdapter.isInitialized) {
            viewModel.carregarProjetos()
        }
    }
}