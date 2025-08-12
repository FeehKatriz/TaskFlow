package com.example.taskflow.activity // Verifique se o nome do pacote está correto

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taskflow.TelaEntrar
import com.example.taskflow.adapters.EquipesAdapter // Importe seu adapter de equipes
import com.example.taskflow.databinding.ActivityTelaMeusProjetosBinding // Importe o binding correto
import com.example.taskflow.models.Equipe // Importe seu modelo de dados
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TelaMeusProjetos : AppCompatActivity() {

    // Configuração do ViewBinding
    private val binding by lazy {
        ActivityTelaMeusProjetosBinding.inflate(layoutInflater)
    }

    // Instâncias do Firebase
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Adapter para a RecyclerView
    private lateinit var equipesAdapter: EquipesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setupRecyclerView()
        loadEquipesDoUsuario()
        setupLogoutButton()
    }

    private fun setupRecyclerView() {
        // Agora esta chamada está correta, pois o adapter só espera a ação de clique
        equipesAdapter = EquipesAdapter { equipe ->
            Toast.makeText(this, "Equipe clicada: ${equipe.nome}", Toast.LENGTH_SHORT).show()
        }

        binding.rvMeusProjetos.apply {
            layoutManager = LinearLayoutManager(this@TelaMeusProjetos)
            adapter = equipesAdapter
        }
    }

    private fun loadEquipesDoUsuario() {
        val userId = auth.currentUser?.uid ?: return // Pega o ID do usuário logado

        // Busca no Firestore as equipes onde o array 'membros' contém o ID do usuário
        firestore.collection("equipes")
            .whereArrayContains("membros", userId)
            .get()
            .addOnSuccessListener { documents ->
                val listaDeEquipes = documents.toObjects(Equipe::class.java)
                // Assumindo que seu EquipesAdapter tem uma função para atualizar a lista
                equipesAdapter.atualizarEquipes(listaDeEquipes)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Erro ao carregar equipes: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupLogoutButton() {
        binding.button6.setOnClickListener {
            auth.signOut() // Desloga o usuário do Firebase
            val intent = Intent(this, TelaEntrar::class.java) // Prepara para ir para a tela de login
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Limpa o histórico de telas
            startActivity(intent)
            finish() // Fecha a tela atual
        }
    }
}