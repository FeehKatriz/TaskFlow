package com.example.taskflow.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taskflow.R
import com.example.taskflow.TelaCriarEquipe
import com.example.taskflow.adapters.EquipesAdapter
import com.example.taskflow.databinding.FragmentEquipesBinding
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

        binding.btnCriarEquipe.setOnClickListener {
            val intent = Intent(requireContext(), TelaCriarEquipe::class.java)
            startActivity(intent)
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
}