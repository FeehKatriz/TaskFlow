package com.example.taskflow.ui.equipes

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taskflow.R
import com.example.taskflow.adapters.TarefasAdapter
import com.example.taskflow.databinding.FragmentEquipeTarefaBinding
import com.example.taskflow.dialogs.GerenciarMembrosBottomSheet
import com.example.taskflow.data.model.Tarefa
import com.example.taskflow.ui.tarefa.criar.CriarTarefaActivity
import com.google.firebase.firestore.FirebaseFirestore

class EquipeTarefaFragment : Fragment() {
    private var equipeId: String? = null
    private var projetoId: String? = null

    private var _binding: FragmentEquipeTarefaBinding? = null
    private val binding get() = _binding!!

    private val firestore = FirebaseFirestore.getInstance()

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
        carregarTarefas()
        buscarProjetoDaEquipe()
    }

    private fun buscarProjetoDaEquipe() {
        // Se já temos o projetoId, não precisa buscar
        if (!projetoId.isNullOrEmpty()) return

        val eId = equipeId ?: return

        firestore.collection("equipes")
            .document(eId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    projetoId = document.getString("projetoId")
                }
            }
            .addOnFailureListener { e ->
                Log.e("EquipeTarefas", "Erro ao buscar projeto da equipe", e)
            }
    }

    private fun configurarAdapters() {
        // Adapter para tarefas em andamento
        adapterAndamento = TarefasAdapter(
            layoutRes = R.layout.item_tarefa_andamento
        ) { tarefa ->
            navegarParaTarefa(tarefa)
        }

        binding.rvTarefasAndamento.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = adapterAndamento
        }

        // Adapter para tarefas finalizadas
        adapterFinalizadas = TarefasAdapter(
            layoutRes = R.layout.item_tarefa_finalizada
        ) { tarefa ->
            navegarParaTarefa(tarefa)
        }

        binding.rvTarefasFinalizadas.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = adapterFinalizadas
        }

        // Adapter para tarefas a começar
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
                // Callback quando membros forem atualizados
                Toast.makeText(
                    requireContext(),
                    "Membros da equipe atualizados!",
                    Toast.LENGTH_SHORT
                ).show()

                // Opcional: recarregar dados se necessário
                // carregarTarefas()
            }
        )

        bottomSheet.show(childFragmentManager, "GerenciarMembrosBottomSheet")
    }

    private fun carregarTarefas() {
        val equipeIdAtual = equipeId ?: return

        firestore.collection("tarefas")
            .whereEqualTo("equipeId", equipeIdAtual)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("EquipeTarefas", "Erro ao carregar tarefas", error)
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

                    Log.d("EquipeTarefas", "Tarefas carregadas - Pendentes: ${tarefasPendentes.size}, Em andamento: ${tarefasAndamento.size}, Concluídas: ${tarefasConcluidas.size}")
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