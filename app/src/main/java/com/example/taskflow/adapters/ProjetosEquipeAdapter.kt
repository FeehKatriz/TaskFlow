package com.example.taskflow.adapters

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.taskflow.databinding.ItemProjetosBinding
import com.example.taskflow.models.Projeto

class ProjetosEquipeAdapter(
    private val onItemClick: (Projeto) -> Unit
) : RecyclerView.Adapter<ProjetosEquipeAdapter.ViewHolder>() {

    private var projetos = mutableListOf<Projeto>()

    fun atualizarProjetos(novosProjetos: List<Projeto>) {
        projetos.clear()
        projetos.addAll(novosProjetos)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = projetos.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProjetosBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val projeto = projetos[position]
        holder.bind(projeto)
    }

    inner class ViewHolder(val binding: ItemProjetosBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(projeto: Projeto) {
            binding.apply {
                // Nome do projeto - usando textView9 conforme o XML
                textView9.text = projeto.nome

                // Data de vencimento - usando textView10 conforme o XML
                textView10.text = projeto.dataVencimento

                // Progresso - usando progressBar3 conforme o XML
                progressBar3.progress = projeto.progresso

                // Total de tarefas - usando checkBox conforme o XML
                checkBox.text = "${projeto.totalTarefas} Tarefas"

                // Click listener para o item inteiro
                root.setOnClickListener {
                    onItemClick(projeto)
                }

                // Configurar o checkbox (se necessário)
                checkBox.isChecked = false // ou baseado em alguma propriedade do projeto
                checkBox.isClickable = false // para evitar cliques acidentais no checkbox
            }
        }
    }
}