package com.example.taskflow.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.taskflow.databinding.ItemMembroBinding

class MembroAdapter : RecyclerView.Adapter<MembroAdapter.MembroViewHolder>() {

    override fun getItemCount(): Int = 4

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MembroViewHolder {
        val binding = ItemMembroBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MembroViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MembroViewHolder, position: Int) {
        // Por enquanto, sem binding de dados, depois podermos acrescentar os dados por aqui.
    }

    inner class MembroViewHolder(
        val binding: ItemMembroBinding
    ) : RecyclerView.ViewHolder(binding.root)
}
