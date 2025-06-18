package com.example.taskflow.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taskflow.R
import com.example.taskflow.TelaCriarEquipe
import com.example.taskflow.adapters.EquipesAdapter
import com.example.taskflow.databinding.FragmentEquipesBinding
import com.example.taskflow.databinding.DialogEntrarEquipeBinding
import com.example.taskflow.models.Equipe
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EquipesFragment: Fragment() {

    private val binding by lazy {
        FragmentEquipesBinding.inflate(layoutInflater)
    }

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = binding.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadEquipes()

        // FAB para criar equipe
        binding.fabCriarEquipe.setOnClickListener {
            val intent = Intent(requireContext(), TelaCriarEquipe::class.java)
            startActivity(intent)
        }

        // FAB para entrar em equipe
        binding.fabEntrarEquipe.setOnClickListener {
            showEntrarEquipeDialog()
        }
    }

    private fun setupRecyclerView() {
        binding.rvEquipes.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun loadEquipes() {
        val currentUserId = auth.currentUser?.uid ?: return

        firestore.collection("equipes")
            .whereArrayContains("membros", currentUserId)
            .get()
            .addOnSuccessListener { documents ->
                val equipes = documents.map { doc ->
                    val equipe = doc.toObject(Equipe::class.java)
                    // Definir o ID da equipe se não estiver definido
                    if (equipe.id.isEmpty()) {
                        equipe.id = doc.id
                    }
                    equipe
                }

                binding.rvEquipes.adapter = EquipesAdapter(equipes) { equipe ->
                    // Passar o ID da equipe selecionada
                    val bundle = Bundle().apply {
                        putString("param1", equipe.id) // usando param1 que já existe
                    }

                    findNavController().navigate(
                        R.id.action_equipesFragment_to_equipeFragment,
                        bundle
                    )
                }
            }
            .addOnFailureListener { exception ->
                // Handle error
            }
    }

    private fun showEntrarEquipeDialog() {
        val dialogBinding = DialogEntrarEquipeBinding.inflate(layoutInflater)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCancelar.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnEntrar.setOnClickListener {
            val codigo = dialogBinding.etCodigoEquipe.text.toString().trim().uppercase()

            if (codigo.isEmpty()) {
                Toast.makeText(requireContext(), "Digite o código da equipe", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            entrarNaEquipe(codigo)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun entrarNaEquipe(codigo: String) {
        val currentUserId = auth.currentUser?.uid ?: return

        firestore.collection("equipes")
            .whereEqualTo("codigo", codigo)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(requireContext(), "Código da equipe não encontrado", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val equipeDoc = documents.first()
                val equipe = equipeDoc.toObject(Equipe::class.java)

                // Verificar se o usuário já está na equipe
                if (equipe.membros.contains(currentUserId)) {
                    Toast.makeText(requireContext(), "Você já faz parte desta equipe", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // Adicionar o usuário à equipe
                val novosMembros = equipe.membros.toMutableList()
                novosMembros.add(currentUserId)

                equipeDoc.reference.update("membros", novosMembros)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Você entrou na equipe: ${equipe.nome}", Toast.LENGTH_SHORT).show()
                        loadEquipes() // Recarregar a lista de equipes
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Erro ao entrar na equipe", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Erro ao buscar equipe", Toast.LENGTH_SHORT).show()
            }
    }
}