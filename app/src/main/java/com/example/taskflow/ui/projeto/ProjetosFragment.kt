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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taskflow.R
import com.example.taskflow.adapters.ProjetosAdapter
import com.example.taskflow.databinding.DialogEntrarProjetoBinding
import com.example.taskflow.databinding.FragmentProjetosBinding
import com.example.taskflow.data.model.Projeto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProjetosFragment: Fragment() {

    private val binding by lazy {
        FragmentProjetosBinding.inflate(layoutInflater)
    }

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Criar o adapter uma única vez
    private lateinit var projetosAdapter: ProjetosAdapter

    // Launcher para criar projeto com callback de resultado
    private val criarProjetoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Projeto foi criado com sucesso, recarregar lista
            loadProjetos()
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
        loadProjetos()

        // Configurar FABs
        setupFabs()
    }

    private fun setupRecyclerView() {
        // Criar adapter uma única vez
        projetosAdapter = ProjetosAdapter { projeto ->
            // Passar o ID do projeto selecionado para o ProjetoFragment
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
        // FAB para criar novo projeto
        binding.fabCriarProjeto.setOnClickListener {
            val intent = Intent(requireContext(), CriarProjetoActivity::class.java)
            criarProjetoLauncher.launch(intent)
        }

        // FAB para entrar em projeto existente
        binding.fabEntrarProjeto.setOnClickListener {
            showEntrarProjetoDialog()
        }
    }

    private fun loadProjetos() {
        val currentUserId = auth.currentUser?.uid ?: return

        firestore.collection("projetos")
            .whereArrayContains("membros", currentUserId)
            .get()
            .addOnSuccessListener { documents ->
                val projetos = documents.map { doc ->
                    val projeto = doc.toObject(Projeto::class.java)
                    // Definir o ID do projeto se não estiver definido
                    if (projeto.id.isEmpty()) {
                        projeto.id = doc.id
                    }
                    projeto
                }

                // Atualizar o adapter com os projetos carregados
                projetosAdapter.atualizarProjetos(projetos)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    requireContext(),
                    "Erro ao carregar projetos: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
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
            val codigo = dialogBinding.etCodigoProjeto.text.toString().trim().uppercase()

            if (codigo.isEmpty()) {
                Toast.makeText(requireContext(), "Digite o código do projeto", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            entrarNoProjeto(codigo)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun entrarNoProjeto(codigo: String) {
        val currentUserId = auth.currentUser?.uid ?: return

        firestore.collection("projetos")
            .whereEqualTo("codigo", codigo)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(requireContext(), "Código do projeto não encontrado", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val projetoDoc = documents.first()
                val projeto = projetoDoc.toObject(Projeto::class.java)

                // Verificar se o usuário já está no projeto
                if (projeto.membros.contains(currentUserId)) {
                    Toast.makeText(requireContext(), "Você já faz parte deste projeto", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // Adicionar o usuário ao projeto
                val novosMembros = projeto.membros.toMutableList()
                novosMembros.add(currentUserId)

                projetoDoc.reference.update("membros", novosMembros)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Você entrou no projeto: ${projeto.nome}", Toast.LENGTH_SHORT).show()
                        loadProjetos() // Recarregar a lista de projetos automaticamente
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Erro ao entrar no projeto", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Erro ao buscar projeto", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onResume() {
        super.onResume()
        // Recarregar dados quando voltar para o fragment
        if (::projetosAdapter.isInitialized) {
            loadProjetos()
        }
    }
}