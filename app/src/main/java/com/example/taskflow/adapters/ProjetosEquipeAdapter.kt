package com.example.taskflow.adapters

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import com.bumptech.glide.Glide
import com.example.taskflow.R
import com.example.taskflow.databinding.ItemProjetosBinding
import com.example.taskflow.models.Projeto
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class ProjetosEquipeAdapter(
    private val onItemClick: (Projeto) -> Unit
) : RecyclerView.Adapter<ProjetosEquipeAdapter.ViewHolder>() {

    private var projetos = mutableListOf<Projeto>()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val membrosCache = mutableMapOf<String, List<String>>() // Cache dos membros por projeto

    // Método para atualizar os projetos
    fun atualizarProjetos(novosProjetos: List<Projeto>) {
        projetos.clear()
        projetos.addAll(novosProjetos)
        // Limpar cache ao atualizar projetos para garantir dados atualizados
        membrosCache.clear()
        notifyDataSetChanged()
    }

    fun limparCache() {
        membrosCache.clear()
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

    // Método privado para carregar membros
    private fun carregarMembrosDoProjeto(binding: ItemProjetosBinding, projetoId: String) {
        // Verificar cache primeiro
        if (membrosCache.containsKey(projetoId)) {
            val membrosIds = membrosCache[projetoId] ?: emptyList()
            carregarAvataresProjeto(binding, membrosIds)
            return
        }

        // Buscar no Firestore
        firestore.collection("projetos")
            .document(projetoId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val membrosIds = document.get("membros") as? List<String> ?: emptyList()
                    membrosCache[projetoId] = membrosIds
                    carregarAvataresProjeto(binding, membrosIds)
                }
            }
            .addOnFailureListener {
                // Em caso de erro, limpar o container
                binding.containerIntegrantes.removeAllViews()
            }
    }

    // Método para carregar avatares dinamicamente com sobreposição
    private fun carregarAvataresProjeto(binding: ItemProjetosBinding, membrosIds: List<String>) {
        val container = binding.containerIntegrantes
        container.removeAllViews()

        // Limitar a 4 membros (3 fotos + indicador de "+")
        val maxMembros = 4
        val membrosParaExibir = if (membrosIds.size > maxMembros) {
            membrosIds.take(3) // Mostrar só 3 fotos
        } else {
            membrosIds
        }

        // Adicionar as fotos dos membros com sobreposição
        membrosParaExibir.forEachIndexed { index, userId ->
            val imageView = ImageView(container.context)

            val params = LinearLayout.LayoutParams(100, 100)
            // Criar efeito de sobreposição: cada imagem "empurra" a anterior
            if (index > 0) {
                params.setMargins(-30, 0, 0, 0) // Margem negativa para sobrepor
            } else {
                params.setMargins(0, 0, 0, 0) // Primeira imagem sem margem
            }

            imageView.layoutParams = params
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP

            // Adicionar borda branca para destacar a sobreposição
            imageView.background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(android.graphics.Color.WHITE)
                setStroke(4, android.graphics.Color.WHITE)
            }

            // Elevar a imagem para ficar por cima das anteriores
            imageView.elevation = (index + 1) * 2f

            val ref = storage.getReference("usuarios/$userId/fotoPerfil.jpg")
            ref.downloadUrl.addOnSuccessListener { uri ->
                Glide.with(container.context)
                    .load(uri)
                    .placeholder(R.drawable.usertype)
                    .circleCrop()
                    .into(imageView)
            }.addOnFailureListener {
                imageView.setImageResource(R.drawable.usertype)
                // Aplicar círculo também na imagem padrão
                Glide.with(container.context)
                    .load(R.drawable.usertype)
                    .circleCrop()
                    .into(imageView)
            }

            container.addView(imageView)
        }

        // Se há mais membros que o limite, adicionar indicador "+"
        if (membrosIds.size > 1) {
            val plusImageView = ImageView(container.context)

            val params = LinearLayout.LayoutParams(100, 100)
            params.setMargins(-30, 0, 0, 0) // Também sobrepor o indicador "+"
            plusImageView.layoutParams = params
            plusImageView.scaleType = ImageView.ScaleType.CENTER_CROP

            // Usar sua imagem pronta do "+" - muito mais simples!
            plusImageView.setImageResource(R.drawable.add_circular) // Sua imagem já tem tudo pronto

            // Elevar para ficar por cima de todas
            plusImageView.elevation = (membrosParaExibir.size + 1) * 2f

            container.addView(plusImageView)
        }
    }

    inner class ViewHolder(val binding: ItemProjetosBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(projeto: Projeto) {
            binding.apply {
                // Nome do projeto
                textView9.text = projeto.nome

                // Data de vencimento
                textView10.text = projeto.dataVencimento

                // Progresso
                progressBar3.progress = projeto.progresso

                // Total de tarefas
                checkBox.text = "${projeto.totalTarefas} Tarefas"
                checkBox.isChecked = false
                checkBox.isClickable = false

                // Click listener para o item inteiro
                root.setOnClickListener { onItemClick(projeto) }
            }

            // Carregar membros do projeto
            carregarMembrosDoProjeto(binding, projeto.id)
        }
    }
}