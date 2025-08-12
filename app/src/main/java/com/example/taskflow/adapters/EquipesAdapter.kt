package com.example.taskflow.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.taskflow.databinding.ReusableLayoutMinhasEquipesBinding // Verifique se o nome do binding está correto
import com.example.taskflow.models.Equipe
import com.google.firebase.firestore.FirebaseFirestore

// O CONSTRUTOR SÓ RECEBE A AÇÃO DE CLIQUE
class EquipesAdapter(
    private val onItemClick: (Equipe) -> Unit
) : RecyclerView.Adapter<EquipesAdapter.EquipeViewHolder>() {

    // A lista é interna e começa vazia
    private var equipes = mutableListOf<Equipe>()
    private val firestore = FirebaseFirestore.getInstance()
    private val nicknameCache = mutableMapOf<String, String>()

    // A função para ATUALIZAR a lista
    fun atualizarEquipes(novasEquipes: List<Equipe>) {
        this.equipes.clear()
        this.equipes.addAll(novasEquipes)
        this.notifyDataSetChanged()
    }

    override fun getItemCount(): Int = equipes.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EquipeViewHolder {
        val binding = ReusableLayoutMinhasEquipesBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return EquipeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EquipeViewHolder, position: Int) {
        val equipe = equipes[position]
        // O resto do seu onBindViewHolder...
        holder.binding.textView7.text = equipe.nome // Exemplo
        loadNickname(equipe.criador) { nickname ->
            holder.binding.textView45.text = "Criador: $nickname" // Exemplo
        }
        holder.binding.root.setOnClickListener {
            onItemClick(equipe)
        }
    }

    private fun loadNickname(userId: String, callback: (String) -> Unit) {
        // ... (seu código de loadNickname)
        if (nicknameCache.containsKey(userId)) {
            callback(nicknameCache[userId] ?: "Usuário")
            return
        }
        firestore.collection("usuarios").document(userId).get()
            .addOnSuccessListener { document ->
                val nickname = document.getString("nickname") ?: "Usuário"
                nicknameCache[userId] = nickname
                callback(nickname)
            }
            .addOnFailureListener { callback("Usuário") }
    }

    inner class EquipeViewHolder(
        val binding: ReusableLayoutMinhasEquipesBinding
    ) : RecyclerView.ViewHolder(binding.root)
}