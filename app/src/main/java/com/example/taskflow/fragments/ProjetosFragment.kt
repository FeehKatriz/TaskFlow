package com.example.taskflow.fragments

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
import com.example.taskflow.CriarNovoProjeto
import com.example.taskflow.R
import com.example.taskflow.adapters.ProjetosEquipeAdapter // Reutilizando seu adapter de projetos
import com.example.taskflow.databinding.FragmentProjetosBinding
import com.example.taskflow.models.Projeto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProjetosFragment : Fragment() {

    // Usando o mesmo padrão 'lazy' para o binding, é mais limpo e seguro
    private val binding by lazy {
        FragmentProjetosBinding.inflate(layoutInflater)
    }

    // Instâncias do Firebase e do Adapter, como no EquipeFragment
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val projetosAdapter by lazy {
        ProjetosEquipeAdapter { projeto ->
            // Ação de clique: Navega para a tela de tarefas do projeto clicado
            val bundle = Bundle().apply {
                putString("projetoId", projeto.id)
                putString("projetoNome", projeto.nome)
            }
            // Certifique-se de que essa 'action' existe no seu nav_graph.xml
            findNavController().navigate(
                R.id.action_projetosFragment_to_projetoTarefasFragment,
                bundle
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Chama as funções de configuração
        setupRecyclerView()
        loadProjetosDoUsuario()

        // Mantém a lógica do botão para criar um novo projeto
        binding.btnCriarProjeto.setOnClickListener {
            val intent = Intent(requireContext(), CriarNovoProjeto::class.java)
            startActivity(intent)
        }
    }

    // Função para configurar a RecyclerView
    private fun setupRecyclerView() {
        binding.rvMeusProjetosEquipe.adapter = projetosAdapter // CORRETO
        binding.rvMeusProjetosEquipe.layoutManager = LinearLayoutManager(requireContext()) // CORRETO
    }

    // Função para carregar os dados do Firebase
    // Dentro de ProjetosFragment.kt
    private fun loadProjetosDoUsuario() {
        val userId = auth.currentUser?.uid

        Log.d("DEBUG_PROJETOS", "Iniciando busca de projetos...")

        if (userId == null) {
            Log.e("DEBUG_PROJETOS", "ERRO FATAL: ID do usuário é nulo. O usuário não está logado.")
            return
        }

        Log.d("DEBUG_PROJETOS", "Buscando projetos para o usuário com ID: $userId")

        firestore.collection("projetos")
            .whereArrayContains("membros", userId)
            .get()
            .addOnSuccessListener { documentos ->
                if (documentos.isEmpty) {
                    Log.w("DEBUG_PROJETOS", "SUCESSO, mas a consulta ao Firebase não retornou NENHUM documento.")
                } else {
                    Log.d("DEBUG_PROJETOS", "SUCESSO! Encontrados ${documentos.size()} documentos no Firebase.")
                }

                val listaProjetos = documentos.mapNotNull { doc ->
                    doc.toObject(Projeto::class.java).copy(id = doc.id)
                }

                Log.d("DEBUG_PROJETOS", "Total de projetos mapeados para a lista: ${listaProjetos.size}")

                projetosAdapter.atualizarProjetos(listaProjetos)
            }
            .addOnFailureListener { erro ->
                Log.e("DEBUG_PROJETOS", "FALHA na consulta ao Firebase: ${erro.message}", erro)
            }
    }

    // Garante que a lista seja atualizada sempre que o usuário voltar para esta tela
    override fun onResume() {
        super.onResume()
        loadProjetosDoUsuario()
    }
}