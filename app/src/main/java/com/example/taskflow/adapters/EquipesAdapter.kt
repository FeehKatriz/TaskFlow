package com.example.taskflow.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.taskflow.databinding.ReusableLayoutMinhasEquipesBinding

class EquipesAdapter(
    private val onItemClick: () -> Unit
) : RecyclerView.Adapter<EquipesAdapter.EquipeViewHolder>() {

    override fun getItemCount(): Int = 6

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EquipeViewHolder {
        val binding = ReusableLayoutMinhasEquipesBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return EquipeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EquipeViewHolder, position: Int) {
        holder.binding.root.setOnClickListener {
            onItemClick()
        }
    }

    inner class EquipeViewHolder(
        val binding: ReusableLayoutMinhasEquipesBinding
    ) : RecyclerView.ViewHolder(binding.root)
}