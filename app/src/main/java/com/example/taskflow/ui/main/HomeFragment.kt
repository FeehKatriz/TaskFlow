package com.example.taskflow.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taskflow.R
import com.example.taskflow.adapters.TarefasAdapter
import com.example.taskflow.data.model.Tarefa
import com.example.taskflow.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var tarefasVencidasAdapter: TarefasAdapter
    private lateinit var tarefasUrgentesAdapter: TarefasAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configurarRecyclerViews()
        observarDados()
        viewModel.carregarTarefasUrgentes()
    }

    private fun configurarRecyclerViews() {
        // Adapter para tarefas vencidas
        tarefasVencidasAdapter = TarefasAdapter(
            onItemClick = { tarefa -> navegarParaTarefa(tarefa) }
        )

        binding.rvTarefasVencidas.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = tarefasVencidasAdapter
        }

        // Adapter para tarefas urgentes
        tarefasUrgentesAdapter = TarefasAdapter(
            onItemClick = { tarefa -> navegarParaTarefa(tarefa) }
        )

        binding.rvTarefasUrgentes.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = tarefasUrgentesAdapter
        }
    }

    private fun observarDados() {
        viewModel.tarefasVencidas.observe(viewLifecycleOwner) { tarefas ->
            if (tarefas.isEmpty()) {
                binding.secaoVencidas.visibility = View.GONE
            } else {
                binding.secaoVencidas.visibility = View.VISIBLE
                tarefasVencidasAdapter.updateTarefas(tarefas)
            }
            verificarEstadoVazio()
        }

        viewModel.tarefasUrgentes.observe(viewLifecycleOwner) { tarefas ->
            if (tarefas.isEmpty()) {
                binding.secaoUrgentes.visibility = View.GONE
            } else {
                binding.secaoUrgentes.visibility = View.VISIBLE
                tarefasUrgentesAdapter.updateTarefas(tarefas)
            }
            verificarEstadoVazio()
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // Você pode adicionar um ProgressBar se desejar
        }

        viewModel.erro.observe(viewLifecycleOwner) { erro ->
            erro?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.limparErro()
            }
        }
    }

    private fun verificarEstadoVazio() {
        val temVencidas = viewModel.tarefasVencidas.value?.isNotEmpty() == true
        val temUrgentes = viewModel.tarefasUrgentes.value?.isNotEmpty() == true

        if (!temVencidas && !temUrgentes) {
            binding.scrollContent.visibility = View.GONE
            binding.layoutEstadoVazio.visibility = View.VISIBLE
        } else {
            binding.scrollContent.visibility = View.VISIBLE
            binding.layoutEstadoVazio.visibility = View.GONE
        }
    }

    private fun navegarParaTarefa(tarefa: Tarefa) {
        val bundle = Bundle().apply {
            putString("tarefaId", tarefa.id)
            putString("tarefaTitulo", tarefa.titulo)
            putString("tarefaDescricao", tarefa.descricao)
            putString("tarefaStatus", tarefa.status)
            putString("prioridade", tarefa.prioridade)
            putString("dataVencimento", tarefa.dataVencimento)
        }

        try {
            findNavController().navigate(R.id.tarefaFragment, bundle)
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Erro ao abrir tarefa: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.carregarTarefasUrgentes()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}