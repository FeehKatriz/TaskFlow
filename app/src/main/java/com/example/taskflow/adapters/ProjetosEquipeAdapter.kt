package com.example.taskflow.adapters
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.taskflow.databinding.ItemProjetosBinding

class ProjetosEquipeAdapter : RecyclerView.Adapter<ProjetosEquipeAdapter.ViewHolder>() {

    // 4 itens fixos
    override fun getItemCount(): Int = 4

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProjetosBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // não faz nada por enquanto
    }

    inner class ViewHolder(val binding: ItemProjetosBinding)
        : RecyclerView.ViewHolder(binding.root)
}