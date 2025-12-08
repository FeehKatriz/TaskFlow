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
    private val membrosCache = mutableMapOf<String, List<String>>()
    private val progressoCache = mutableMapOf<String, Pair<Int, Int>>() // Cache: equipeId -> (concluídas, total)

    fun atualizarEquipes(novasEquipes: List<Equipe>) {
        equipes.clear()
        equipes.addAll(novasEquipes)
        membrosCache.clear()
        progressoCache.clear()
        notifyDataSetChanged()
    }

    fun limparCache() {
        membrosCache.clear()
        progressoCache.clear()
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

    private fun carregarProgressoDaEquipe(binding: ItemEquipeBinding, equipeId: String) {
        if (progressoCache.containsKey(equipeId)) {
            val (concluidas, total) = progressoCache[equipeId]!!
            atualizarProgresso(binding, concluidas, total)
            return
        }

        firestore.collection("tarefas")
            .whereEqualTo("equipeId", equipeId)
            .get()
            .addOnSuccessListener { documents ->
                val total = documents.size()
                val concluidas = documents.count { doc ->
                    doc.getString("status") == "concluida"
                }

                progressoCache[equipeId] = Pair(concluidas, total)
                atualizarProgresso(binding, concluidas, total)
            }
            .addOnFailureListener {
                atualizarProgresso(binding, 0, 0)
            }
    }

    private fun atualizarProgresso(binding: ItemEquipeBinding, concluidas: Int, total: Int) {
        binding.apply {
            checkBox.text = "$total Tarefas"
            checkBox.isChecked = false
            checkBox.isClickable = false

            if (total > 0) {
                val porcentagem = ((concluidas.toFloat() / total.toFloat()) * 100).toInt()
                progressBar3.progress = porcentagem
                tvProgressoPorcentagem.text = "$porcentagem%"
            } else {
                progressBar3.progress = 0
                tvProgressoPorcentagem.text = "0%"
            }
        }
    }

    private fun carregarMembrosDaEquipe(binding: ItemEquipeBinding, equipeId: String) {
        if (membrosCache.containsKey(equipeId)) {
            val membrosIds = membrosCache[equipeId] ?: emptyList()
            carregarAvatares(binding, membrosIds)
            return
        }

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
                binding.containerIntegrantes.removeAllViews()
            }
    }

    private fun carregarAvatares(binding: ItemEquipeBinding, membrosIds: List<String>) {
        val container = binding.containerIntegrantes
        container.removeAllViews()

        // Máximo de 5 elementos no total (fotos + indicador)
        // Se tem mais de 5 membros, mostra 4 fotos + indicador "+X"
        val mostrarIndicador = membrosIds.size > 5
        val quantidadeFotos = if (mostrarIndicador) 4 else membrosIds.size

        membrosIds.take(quantidadeFotos).forEachIndexed { index, userId ->
            val imageView = ImageView(container.context)

            val params = LinearLayout.LayoutParams(100, 100)
            if (index > 0) {
                params.setMargins(-30, 0, 0, 0)
            } else {
                params.setMargins(0, 0, 0, 0)
            }

            imageView.layoutParams = params
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP

            imageView.background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(android.graphics.Color.WHITE)
                setStroke(4, android.graphics.Color.WHITE)
            }

            imageView.elevation = (index + 1) * 2f

            val ref = storage.getReference("usuarios/$userId/fotoPerfil.jpg")
            ref.downloadUrl.addOnSuccessListener { uri ->
                Glide.with(container.context)
                    .load(uri)
                    .placeholder(R.drawable.usertype)
                    .circleCrop()
                    .into(imageView)
            }.addOnFailureListener {
                // fallback para Firestore
                firestore.collection("usuarios")
                    .document(userId)
                    .get()
                    .addOnSuccessListener { doc ->
                        val foto = doc?.getString("fotoUrl")
                        if (!foto.isNullOrEmpty()) {
                            Glide.with(container.context)
                                .load(foto)
                                .placeholder(R.drawable.usertype)
                                .circleCrop()
                                .into(imageView)
                        } else {
                            Glide.with(container.context)
                                .load(R.drawable.usertype)
                                .circleCrop()
                                .into(imageView)
                        }
                    }
                    .addOnFailureListener {
                        Glide.with(container.context)
                            .load(R.drawable.usertype)
                            .circleCrop()
                            .into(imageView)
                    }
            }

            container.addView(imageView)
        }

        if (mostrarIndicador) {
            val numeroExtra = membrosIds.size - 4
            val extraImageView = ImageView(container.context)

            val params = LinearLayout.LayoutParams(100, 100)
            params.setMargins(-30, 0, 0, 0)
            extraImageView.layoutParams = params
            extraImageView.scaleType = ImageView.ScaleType.CENTER

            extraImageView.background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(android.graphics.Color.parseColor("#34495e"))
                setStroke(4, android.graphics.Color.WHITE)
            }

            val textView = android.widget.TextView(container.context)
            textView.text = "+$numeroExtra"
            textView.textSize = 14f
            textView.setTextColor(android.graphics.Color.WHITE)
            textView.gravity = android.view.Gravity.CENTER
            textView.setTypeface(null, android.graphics.Typeface.BOLD)

            textView.measure(
                android.view.View.MeasureSpec.makeMeasureSpec(100, android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(100, android.view.View.MeasureSpec.EXACTLY)
            )
            textView.layout(0, 0, 100, 100)

            val bitmap = android.graphics.Bitmap.createBitmap(100, 100, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            textView.draw(canvas)

            extraImageView.setImageBitmap(bitmap)
            extraImageView.elevation = (quantidadeFotos + 1) * 2f

            container.addView(extraImageView)
        }
    }

    inner class ViewHolder(val binding: ItemEquipeBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(equipe: Equipe) {
            binding.apply {
                // Nome da equipe
                textView9.text = equipe.nome

                // Click listener para o item inteiro
                root.setOnClickListener { onItemClick(equipe) }
            }

            // Carregar progresso real das tarefas
            carregarProgressoDaEquipe(binding, equipe.id)

            // Carregar membros da equipe
            carregarMembrosDaEquipe(binding, equipe.id)
        }
    }
}