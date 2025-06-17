package com.example.taskflow.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ProgressBar
import androidx.recyclerview.widget.RecyclerView
import com.example.taskflow.R
import com.example.taskflow.models.Tarefa

class TarefasAdapter(
    private var tarefas: List<Tarefa> = emptyList(),
    private val layoutRes: Int,
    private val onItemClick: (Tarefa) -> Unit = {}
) : RecyclerView.Adapter<TarefasAdapter.TarefaViewHolder>() {

    override fun getItemCount(): Int = tarefas.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TarefaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(layoutRes, parent, false)
        return TarefaViewHolder(view)
    }

    override fun onBindViewHolder(holder: TarefaViewHolder, position: Int) {
        val tarefa = tarefas[position]
        holder.bind(tarefa)
        holder.itemView.setOnClickListener { onItemClick(tarefa) }
    }

    fun updateTarefas(novasTarefas: List<Tarefa>) {
        tarefas = novasTarefas
        notifyDataSetChanged()
    }

    class TarefaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(tarefa: Tarefa) {
            // Usar os IDs corretos do layout XML fornecido
            val titulo = itemView.findViewById<TextView>(R.id.taskNameText)
            val progressBar = itemView.findViewById<ProgressBar>(R.id.progressBar)

            // Definir o texto do título
            titulo?.text = tarefa.titulo

            // Se a tarefa tiver informações de progresso, você pode definir aqui
            // Assumindo que Tarefa tem uma propriedade 'progresso' (0-100)
            // progressBar?.progress = tarefa.progresso ?: 0

            // Se você quiser exibir a descrição em algum lugar,
            // precisará adicionar outro TextView ao layout XML
            // ou usar uma abordagem diferente
        }
    }
}