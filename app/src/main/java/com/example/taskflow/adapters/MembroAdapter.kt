package com.example.taskflow.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.taskflow.databinding.ItemMembroBinding

class MembroAdapter : RecyclerView.Adapter<MembroAdapter.MembroViewHolder>() {

    private var membros = listOf<Map<String, String>>()

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
            binding.root.findViewById<android.widget.TextView>(com.example.taskflow.R.id.nomeMembro)?.text =
                membro["nome"] ?: "Nome não disponível"

            binding.root.findViewById<android.widget.TextView>(com.example.taskflow.R.id.cargoMembro)?.text =
                membro["tipo"] ?: "Membro"
        }
    }
}