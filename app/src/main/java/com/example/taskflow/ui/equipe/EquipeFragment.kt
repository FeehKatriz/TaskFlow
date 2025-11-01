package com.example.taskflow.ui.equipe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taskflow.R
import com.example.taskflow.adapters.EquipesProjetoAdapter
import com.example.taskflow.databinding.FragmentEquipeBinding
import androidx.navigation.fragment.findNavController

class EquipeFragment : Fragment() {

    private var _binding: FragmentEquipeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EquipeViewModel by viewModels()
    private lateinit var equipesAdapter: EquipesProjetoAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEquipeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configurarRecyclerView()
        observarDados()
        viewModel.carregarMinhasEquipes()
    }

    private fun configurarRecyclerView() {
        equipesAdapter = EquipesProjetoAdapter { equipe ->
            navegarParaEquipeTarefas(equipe)
        }

        binding.rvMinhasEquipes.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = equipesAdapter
        }
    }

    private fun navegarParaEquipeTarefas(equipe: com.example.taskflow.data.model.Equipe) {
        val bundle = Bundle().apply {
            putString("equipeId", equipe.id)
            putString("projetoId", equipe.projetoId)
        }

        try {
            findNavController().navigate(
                R.id.action_fragment_equipe_to_equipeTarefasFragment,
                bundle
            )
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Erro ao abrir equipe: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun observarDados() {
        viewModel.minhasEquipes.observe(viewLifecycleOwner) { equipes ->
            if (equipes.isEmpty()) {
                mostrarEstadoVazio()
            } else {
                mostrarEquipes(equipes)
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // Você pode adicionar um ProgressBar no layout se quiser
            // binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.erro.observe(viewLifecycleOwner) { erro ->
            erro?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.limparErro()
            }
        }
    }

    private fun mostrarEquipes(equipes: List<com.example.taskflow.data.model.Equipe>) {
        binding.rvMinhasEquipes.visibility = View.VISIBLE
        binding.layoutEstadoVazio.visibility = View.GONE
        equipesAdapter.atualizarEquipes(equipes)
    }

    private fun mostrarEstadoVazio() {
        binding.rvMinhasEquipes.visibility = View.GONE
        binding.layoutEstadoVazio.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        // Recarregar quando voltar para o fragment
        viewModel.carregarMinhasEquipes()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}