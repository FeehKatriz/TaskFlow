package com.example.taskflow.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taskflow.R
import com.example.taskflow.telaCriarTarefa
import com.example.taskflow.adapters.TarefasAdapter
import com.example.taskflow.databinding.FragmentProjetoTarefasBinding
import com.example.taskflow.models.Tarefa
import com.google.firebase.firestore.FirebaseFirestore
import androidx.navigation.fragment.findNavController
import com.example.taskflow.dialogs.GerenciarMembrosBottomSheet // <- IMPORT ADICIONADO

private const val ARG_PROJETO_ID = "projetoId"
private const val ARG_EQUIPE_ID = "equipeId"

class ProjetoTarefasFragment : Fragment() {
    private var projetoId: String? = null
    private var equipeId: String? = null

    private var _binding: FragmentProjetoTarefasBinding? = null
    private val binding get() = _binding!!

    private val firestore = FirebaseFirestore.getInstance()

    private lateinit var adapterAndamento: TarefasAdapter
    private lateinit var adapterFinalizadas: TarefasAdapter
    private lateinit var adapterAComecar: TarefasAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            projetoId = it.getString(ARG_PROJETO_ID)
            equipeId = it.getString(ARG_EQUIPE_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProjetoTarefasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configurarAdapters()
        configurarFABs() // <- MÉTODO RENOMEADO
        carregarTarefas()
        buscarEquipeDoProjeto() // <- MÉTODO ADICIONADO
    }

    // MÉTODO ADICIONADO - Buscar equipeId se não foi passado
    private fun buscarEquipeDoProjeto() {
        // Se já temos o equipeId, não precisa buscar
        if (!equipeId.isNullOrEmpty()) return

        val pId = projetoId ?: return

        firestore.collection("projetos")
            .document(pId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    equipeId = document.getString("equipeId")
                }
            }
            .addOnFailureListener { e ->
                Log.e("ProjetoTarefas", "Erro ao buscar equipe do projeto", e)
            }
    }

    private fun configurarAdapters() {
        // Em andamento
        adapterAndamento = TarefasAdapter(
            layoutRes = R.layout.item_tarefa_andamento
        ) { tarefa ->
            navegarParaTarefa(tarefa)
        }

        binding.rvTarefasAndamento.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = adapterAndamento
        }

        // Finalizadas
        adapterFinalizadas = TarefasAdapter(
            layoutRes = R.layout.item_tarefa_finalizada
        ) { tarefa ->
            navegarParaTarefa(tarefa)
        }

        binding.rvTarefasFinalizadas.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = adapterFinalizadas
        }

        // A começar
        adapterAComecar = TarefasAdapter(
            layoutRes = R.layout.item_tarefa_afazer
        ) { tarefa ->
            navegarParaTarefa(tarefa)
        }

        binding.rvTarefasAComecar.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = adapterAComecar
        }
    }

    private fun navegarParaTarefa(tarefa: Tarefa) {
        val bundle = Bundle().apply {
            putString("tarefaId", tarefa.id)
            putString("tarefaTitulo", tarefa.titulo)
            putString("tarefaDescricao", tarefa.descricao)
            putString("tarefaProjetoId", tarefa.projetoId)
            putString("tarefaEquipeId", tarefa.equipeId)
            putString("tarefaCriadoPor", tarefa.criadoPor)
            putString("tarefaStatus", tarefa.status)
            putString("tarefaPrioridade", tarefa.prioridade)
            putString("tarefaDataVencimento", tarefa.dataVencimento)
            putLong("tarefaDataCriacao", tarefa.dataCriacao)
            putStringArrayList("tarefaAnexos", ArrayList(tarefa.anexos))
        }
        findNavController().navigate(R.id.action_projetoTarefasFragment_to_tarefa, bundle)
    }

    // MÉTODO RENOMEADO E EXPANDIDO
    private fun configurarFABs() {
        // FAB para criar tarefa (já existia)
        binding.fabCriarTarefa.setOnClickListener {
            val intent = Intent(requireContext(), telaCriarTarefa::class.java)
            intent.putExtra("projetoId", projetoId)
            intent.putExtra("equipeId", equipeId)
            startActivity(intent)
        }

        // FAB para gerenciar membros (NOVO) - se existir no layout
        binding.fabGerenciarMembros?.setOnClickListener {
            abrirGerenciadorMembros()
        }

        // OU - Se você preferir um botão no cabeçalho (ALTERNATIVA)
        binding.fabGerenciarMembros?.setOnClickListener {
            abrirGerenciadorMembros()
        }
    }

    // MÉTODO ADICIONADO - Abrir BottomSheet de membros
    private fun abrirGerenciadorMembros() {
        val pId = projetoId ?: run {
            Toast.makeText(requireContext(), "ID do projeto não encontrado", Toast.LENGTH_SHORT).show()
            return
        }

        val eId = equipeId ?: run {
            Toast.makeText(requireContext(), "ID da equipe não encontrado", Toast.LENGTH_SHORT).show()
            return
        }

        val bottomSheet = GerenciarMembrosBottomSheet.newInstance(
            projetoId = pId,
            equipeId = eId,
            onMembrosAtualizados = {
                // Callback quando membros forem atualizados
                Toast.makeText(
                    requireContext(),
                    "Membros do projeto atualizados!",
                    Toast.LENGTH_SHORT
                ).show()

                // Opcional: recarregar dados se necessário
                // carregarTarefas()
            }
        )

        bottomSheet.show(childFragmentManager, "GerenciarMembrosBottomSheet")
    }

    private fun carregarTarefas() {
        val projetoIdAtual = projetoId ?: return

        firestore.collection("tarefas")
            .whereEqualTo("projetoId", projetoIdAtual)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ProjetoTarefas", "Erro ao carregar tarefas", error)
                    Toast.makeText(context, "Erro ao carregar tarefas: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val todasTarefas = snapshot.toObjects(Tarefa::class.java)

                    // Organizar tarefas por status
                    val tarefasPendentes = todasTarefas.filter { it.status == "pendente" }
                    val tarefasAndamento = todasTarefas.filter { it.status == "em_andamento" }
                    val tarefasConcluidas = todasTarefas.filter { it.status == "concluida" }

                    // Atualizar adapters
                    adapterAComecar.updateTarefas(tarefasPendentes)
                    adapterAndamento.updateTarefas(tarefasAndamento)
                    adapterFinalizadas.updateTarefas(tarefasConcluidas)

                    Log.d("ProjetoTarefas", "Tarefas carregadas - Pendentes: ${tarefasPendentes.size}, Em andamento: ${tarefasAndamento.size}, Concluídas: ${tarefasConcluidas.size}")
                }
            }
    }

    override fun onResume() {
        super.onResume()
        // Recarregar tarefas quando voltar para o fragment
        carregarTarefas()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance(projetoId: String, equipeId: String = "") =
            ProjetoTarefasFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PROJETO_ID, projetoId)
                    putString(ARG_EQUIPE_ID, equipeId)
                }
            }
    }
}