package com.example.taskflow.adapters

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import com.bumptech.glide.Glide
import com.example.taskflow.R
import com.example.taskflow.databinding.ItemEquipeBinding
import com.example.taskflow.data.model.Equipe
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class EquipesProjetoAdapter(
    private val onItemClick: (Equipe) -> Unit
) : RecyclerView.Adapter<EquipesProjetoAdapter.ViewHolder>() {

    private var equipes = mutableListOf<Equipe>()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val membrosCache = mutableMapOf<String, List<String>>() // Cache dos membros por equipe

    // Método para atualizar as equipes
    fun atualizarEquipes(novasEquipes: List<Equipe>) {
        equipes.clear()
        equipes.addAll(novasEquipes)
        // Limpar cache ao atualizar equipes para garantir dados atualizados
        membrosCache.clear()
        notifyDataSetChanged()
    }

    fun limparCache() {
        membrosCache.clear()
    }

    override fun getItemCount(): Int = equipes.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEquipeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val equipe = equipes[position]
        holder.bind(equipe)
    }

    // Método privado para carregar membros
    private fun carregarMembrosDaEquipe(binding: ItemEquipeBinding, equipeId: String) {
        // Verificar cache primeiro
        if (membrosCache.containsKey(equipeId)) {
            val membrosIds = membrosCache[equipeId] ?: emptyList()
            carregarAvatares(binding, membrosIds)
            return
        }

        // Buscar no Firestore
        firestore.collection("equipes")
            .document(equipeId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val membrosIds = document.get("membros") as? List<String> ?: emptyList()
                    membrosCache[equipeId] = membrosIds
                    carregarAvatares(binding, membrosIds)
                }
            }
            .addOnFailureListener {
                // Em caso de erro, limpar o container
                binding.containerIntegrantes.removeAllViews()
            }
    }

    // Método para carregar avatares dinamicamente com sobreposição
    private fun carregarAvatares(binding: ItemEquipeBinding, membrosIds: List<String>) {
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
        if (membrosIds.size > 3) {
            val numeroExtra = membrosIds.size - 3
            val extraImageView = ImageView(container.context)

            val params = LinearLayout.LayoutParams(100, 100)
            params.setMargins(-30, 0, 0, 0)
            extraImageView.layoutParams = params
            extraImageView.scaleType = ImageView.ScaleType.CENTER

            // Criar círculo com número
            extraImageView.background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(android.graphics.Color.parseColor("#666666")) // Cinza
                setStroke(4, android.graphics.Color.WHITE)
            }

            // Criar TextView para o número e convertê-lo em drawable
            val textView = android.widget.TextView(container.context)
            textView.text = "+$numeroExtra"
            textView.textSize = 14f
            textView.setTextColor(android.graphics.Color.WHITE)
            textView.gravity = android.view.Gravity.CENTER
            textView.setTypeface(null, android.graphics.Typeface.BOLD)

            // Converter TextView em Bitmap e depois em Drawable
            textView.measure(
                android.view.View.MeasureSpec.makeMeasureSpec(100, android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(100, android.view.View.MeasureSpec.EXACTLY)
            )
            textView.layout(0, 0, 100, 100)

            val bitmap = android.graphics.Bitmap.createBitmap(100, 100, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            textView.draw(canvas)

            extraImageView.setImageBitmap(bitmap)

            // Elevar para ficar por cima de todas
            extraImageView.elevation = (membrosParaExibir.size + 1) * 2f

            container.addView(extraImageView)
        }
    }

    inner class ViewHolder(val binding: ItemEquipeBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(equipe: Equipe) {
            binding.apply {
                // Nome da equipe
                textView9.text = equipe.nome

                // Data de vencimento
                textView10.text = equipe.dataVencimento

                // Progresso
                progressBar3.progress = equipe.progresso

                // Total de tarefas
                checkBox.text = "${equipe.totalTarefas} Tarefas"
                checkBox.isChecked = false
                checkBox.isClickable = false

                // Click listener para o item inteiro
                root.setOnClickListener { onItemClick(equipe) }
            }

            // Carregar membros da equipe
            carregarMembrosDaEquipe(binding, equipe.id)
        }
    }
}