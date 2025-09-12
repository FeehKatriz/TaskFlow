package com.example.taskflow.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.taskflow.R
import com.example.taskflow.databinding.ReusableLayoutMinhasEquipesBinding
import com.example.taskflow.models.Equipe
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import android.graphics.Color


class EquipesAdapter(
    private val equipes: List<Equipe>,
    private val onItemClick: (Equipe) -> Unit
) : RecyclerView.Adapter<EquipesAdapter.EquipeViewHolder>() {

    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
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

        // Cor personalizada do card - AGORA MUITO MAIS SIMPLES!
        try {
            val color = Color.parseColor(equipe.cor)
            holder.binding.root.background.setTint(color)
        } catch (e: Exception) {
            // Se a cor for inválida, mantém a cor padrão do drawable
            holder.binding.root.background.clearColorFilter()
        }

        // Carregar as fotinhas dos membros
        carregarAvatares(holder.binding.containerIntegrantes, equipe.membros)

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

    private fun carregarAvatares(container: LinearLayout, membros: List<String>) {
        container.removeAllViews()

        for (userId in membros) {
            val imageView = ImageView(container.context)

            val params = LinearLayout.LayoutParams(100, 100)
            params.setMargins(8, 0, 8, 0)
            imageView.layoutParams = params
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP

            val ref = storage.getReference("usuarios/$userId/fotoPerfil.jpg")
            ref.downloadUrl.addOnSuccessListener { uri ->
                Glide.with(container.context)
                    .load(uri)
                    .placeholder(R.drawable.usertype)
                    .circleCrop()
                    .into(imageView)
            }.addOnFailureListener {
                imageView.setImageResource(R.drawable.usertype)
            }

            container.addView(imageView)
        }
    }

    inner class EquipeViewHolder(
        val binding: ReusableLayoutMinhasEquipesBinding
    ) : RecyclerView.ViewHolder(binding.root)
}