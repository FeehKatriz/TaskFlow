package com.example.taskflow.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.taskflow.R
import com.example.taskflow.databinding.ItemMembroBinding
import com.google.firebase.storage.FirebaseStorage

class MembroAdapter : RecyclerView.Adapter<MembroAdapter.MembroViewHolder>() {

    private var membros = listOf<Map<String, String>>() // espera "uid", "nome", "tipo"
    private val storage = FirebaseStorage.getInstance()

    fun atualizarMembros(novosMembros: List<Map<String, String>>) {
        membros = novosMembros
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = membros.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MembroViewHolder {
        val binding = ItemMembroBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MembroViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MembroViewHolder, position: Int) {
        val membro = membros[position]
        holder.bind(membro)
    }

    inner class MembroViewHolder(
        private val binding: ItemMembroBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(membro: Map<String, String>) {
            binding.nomeMembro.text = membro["nome"] ?: "Nome não disponível"
            binding.cargoMembro.text = membro["tipo"] ?: "Membro"

            val uid = membro["uid"] ?: ""
            val imageView: ImageView = binding.imgUsuario

            if (uid.isNotEmpty()) {
                val ref = storage.getReference("usuarios/$uid/fotoPerfil.jpg")
                ref.downloadUrl.addOnSuccessListener { uri ->
                    Glide.with(imageView.context)
                        .load(uri)
                        .placeholder(R.drawable.usertype)
                        .circleCrop()
                        .into(imageView)
                }.addOnFailureListener {
                    imageView.setImageResource(R.drawable.usertype)
                }
            } else {
                imageView.setImageResource(R.drawable.usertype)
            }
        }
    }
}
