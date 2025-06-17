package com.example.taskflow.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.taskflow.databinding.ReusableLayoutMinhasEquipesBinding
import com.example.taskflow.models.Equipe
import com.google.firebase.firestore.FirebaseFirestore

class EquipesAdapter(
    private val equipes: List<Equipe>,
    private val onItemClick: (Equipe) -> Unit
) : RecyclerView.Adapter<EquipesAdapter.EquipeViewHolder>() {

    private val firestore = FirebaseFirestore.getInstance()
    private val nicknameCache = mutableMapOf<String, String>()

    override fun getItemCount(): Int = equipes.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EquipeViewHolder {
        val binding = ReusableLayoutMinhasEquipesBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return EquipeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EquipeViewHolder, position: Int) {
        val equipe = equipes[position]
        holder.binding.textView7.text = equipe.nome

        // Buscar nickname do criador
        loadNickname(equipe.criador) { nickname ->
            holder.binding.textView45.text = nickname
        }

        holder.binding.root.setOnClickListener {
            onItemClick(equipe)
        }
    }

    private fun loadNickname(userId: String, callback: (String) -> Unit) {
        // Verificar cache primeiro
        if (nicknameCache.containsKey(userId)) {
            callback(nicknameCache[userId] ?: "Usuário")
            return
        }

        // Buscar no Firestore
        firestore.collection("usuarios")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                val nickname = document.getString("nickname") ?: "Usuário"
                nicknameCache[userId] = nickname
                callback(nickname)
            }
            .addOnFailureListener {
                callback("Usuário")
            }
    }

    inner class EquipeViewHolder(
        val binding: ReusableLayoutMinhasEquipesBinding
    ) : RecyclerView.ViewHolder(binding.root)
}