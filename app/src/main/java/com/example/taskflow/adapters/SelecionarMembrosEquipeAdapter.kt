package com.example.taskflow.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.taskflow.R
import com.example.taskflow.databinding.ItemSelecionarMembroBinding
import com.google.firebase.storage.FirebaseStorage

class SelecionarMembrosEquipeAdapter(
    private val onMembroSelecionado: (String, Boolean) -> Unit
) : RecyclerView.Adapter<SelecionarMembrosEquipeAdapter.ViewHolder>() {

    private var membros = listOf<Map<String, String>>()
    private var membrosSelecionados = setOf<String>()
    private val storage = FirebaseStorage.getInstance()

    fun atualizarMembros(novosMembros: List<Map<String, String>>, selecionados: Set<String>) {
        membros = novosMembros
        membrosSelecionados = selecionados
        notifyDataSetChanged()
    }

    fun atualizarSelecao(novaSelecao: Set<String>) {
        membrosSelecionados = novaSelecao
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = membros.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSelecionarMembroBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val membro = membros[position]
        holder.bind(membro, membrosSelecionados.contains(membro["uid"]))
    }

    inner class ViewHolder(private val binding: ItemSelecionarMembroBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(membro: Map<String, String>, selecionado: Boolean) {
            binding.apply {
                // Nome do membro
                tvNomeMembro.text = membro["nome"] ?: "Usuário"

                // Email do membro
                tvEmailMembro.text = membro["email"] ?: ""

                // Checkbox
                checkBoxMembro.isChecked = selecionado

                // Carregar foto do membro
                val userId = membro["fotoPerfil"] ?: ""
                if (userId.isNotEmpty()) {
                    val ref = storage.getReference("usuarios/$userId/fotoPerfil.jpg")
                    ref.downloadUrl.addOnSuccessListener { uri ->
                        Glide.with(ivFotoMembro.context)
                            .load(uri)
                            .placeholder(R.drawable.usertype)
                            .error(R.drawable.usertype)
                            .circleCrop()
                            .into(ivFotoMembro)
                    }.addOnFailureListener {
                        ivFotoMembro.setImageResource(R.drawable.usertype)
                    }
                } else {
                    ivFotoMembro.setImageResource(R.drawable.usertype)
                }

                // Click listeners
                root.setOnClickListener {
                    val novoEstado = !checkBoxMembro.isChecked
                    checkBoxMembro.isChecked = novoEstado
                    onMembroSelecionado(membro["uid"] ?: "", novoEstado)
                }

                checkBoxMembro.setOnClickListener {
                    onMembroSelecionado(membro["uid"] ?: "", checkBoxMembro.isChecked)
                }
            }
        }
    }
}