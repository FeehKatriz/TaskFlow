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
    private lateinit var equipesAdapter: EquipesAdapter // Movido para ser propriedade da classe

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = binding.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadEquipes()

        binding.fabCriarEquipe.setOnClickListener {
            val intent = Intent(requireContext(), TelaCriarEquipe::class.java)
            startActivity(intent)
        }

        binding.fabEntrarEquipe.setOnClickListener {
            showEntrarEquipeDialog()
        }
    }

    private fun setupRecyclerView() {
        // Crie o adapter aqui, uma única vez
        equipesAdapter = EquipesAdapter { equipe ->
            val bundle = Bundle().apply {
                putString("param1", equipe.id)
            }
            findNavController().navigate(
                R.id.action_equipesFragment_to_equipeFragment,
                bundle
            )
        }
        binding.rvEquipes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEquipes.adapter = equipesAdapter
    }


    private fun loadEquipes() {
        val currentUserId = auth.currentUser?.uid ?: return

        firestore.collection("equipes")
            .whereArrayContains("membros", currentUserId)
            .get()
            .addOnSuccessListener { documents ->
                val equipes = documents.map { doc ->
                    val equipe = doc.toObject(Equipe::class.java)
                    equipe.id = doc.id
                    equipe
                }
                // Apenas atualize a lista no adapter que já existe
                equipesAdapter.atualizarEquipes(equipes)

            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Erro ao carregar equipes: ${exception.message}", Toast.LENGTH_SHORT).show()
            } // <- CHAVE FALTANDO AQUI NO SEU CÓDIGO ORIGINAL
    } // <- CHAVE FALTANDO AQUI NO SEU CÓDIGO ORIGINAL

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

                if (equipe.membros.contains(currentUserId)) {
                    Toast.makeText(requireContext(), "Você já faz parte desta equipe", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

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