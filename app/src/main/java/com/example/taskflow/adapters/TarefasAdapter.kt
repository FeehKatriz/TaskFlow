package com.example.taskflow.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView

class TarefasAdapter(
    private val itemCount: Int,
    @LayoutRes private val layoutRes: Int,
    private val onItemClick: () -> Unit
) : RecyclerView.Adapter<TarefasAdapter.TarefaViewHolder>() {

    override fun getItemCount(): Int = itemCount

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TarefaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(layoutRes, parent, false)
        return TarefaViewHolder(view)
    }

    override fun onBindViewHolder(holder: TarefaViewHolder, position: Int) {
        holder.itemView.setOnClickListener { onItemClick() }
    }

    class TarefaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
