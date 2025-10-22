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
    private lateinit var tarefasAdapter: TarefasAdapter

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

        configurarRecyclerView()
        observarDados()
        viewModel.carregarTarefasUrgentes()
    }

    private fun configurarRecyclerView() {
        tarefasAdapter = TarefasAdapter(
            // NÃO passa layoutRes - deixa ele escolher automaticamente!
            onItemClick = { tarefa ->
                navegarParaTarefa(tarefa)
            }
        )

        binding.rvProjetos.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = tarefasAdapter
        }
    }

    private fun observarDados() {
        viewModel.tarefasUrgentes.observe(viewLifecycleOwner) { tarefas ->
            if (tarefas.isEmpty()) {
                mostrarEstadoVazio()
            } else {
                mostrarTarefas(tarefas)
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

    private fun mostrarTarefas(tarefas: List<Tarefa>) {
        binding.rvProjetos.visibility = View.VISIBLE
        binding.layoutEstadoVazio.visibility = View.GONE
        tarefasAdapter.updateTarefas(tarefas)
    }

    private fun mostrarEstadoVazio() {
        binding.rvProjetos.visibility = View.GONE
        binding.layoutEstadoVazio.visibility = View.VISIBLE
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
            // Navegar diretamente pelo ID global
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
        // Recarregar quando voltar para o fragment
        viewModel.carregarTarefasUrgentes()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}