package com.example.taskflow.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.taskflow.R
import com.example.taskflow.databinding.ReusableLayoutMeusProjetosBinding
import com.example.taskflow.models.Projeto
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import android.graphics.Color

class ProjetosAdapter(
    private val onItemClick: (Projeto) -> Unit
) : RecyclerView.Adapter<ProjetosAdapter.ProjetoViewHolder>() {

    private var projetos = mutableListOf<Projeto>() // Lista mutável de projetos
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val nicknameCache = mutableMapOf<String, String>()

    // Método para atualizar os projetos
    fun atualizarProjetos(novosProjetos: List<Projeto>) {
        projetos.clear()
        projetos.addAll(novosProjetos)
        // Limpar cache ao atualizar para garantir dados frescos
        nicknameCache.clear()
        notifyDataSetChanged()
    }

    // Método para limpar cache se necessário
    fun limparCache() {
        nicknameCache.clear()
    }

    override fun getItemCount(): Int = projetos.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjetoViewHolder {
        val binding = ReusableLayoutMeusProjetosBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ProjetoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProjetoViewHolder, position: Int) {
        val projeto = projetos[position]

        holder.binding.textView7.text = projeto.nome

        // Buscar nickname do criador
        loadNickname(projeto.criador) { nickname ->
            holder.binding.textView45.text = nickname
        }

        // Cor personalizada do card
        try {
            val color = Color.parseColor(projeto.cor)
            holder.binding.root.background.setTint(color)
        } catch (e: Exception) {
            // Se a cor for inválida, mantém a cor padrão do drawable
            holder.binding.root.background.clearColorFilter()
        }

        // Carregar as fotinhas dos membros com sobreposição
        carregarAvatares(holder.binding.containerIntegrantes, projeto.membros)

        holder.binding.root.setOnClickListener {
            onItemClick(projeto)
        }
    }

    private fun loadNickname(userId: String, callback: (String) -> Unit) {
        // Verificar cache primeiro
        if (nicknameCache.containsKey(userId)) {
            callback(nicknameCache[userId] ?: "Usuário")
            return
        }

        // Buscar no Firestore
        firestore.collection("usuarios")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                val nickname = document.getString("nickname") ?: "Usuário"
                nicknameCache[userId] = nickname
                callback(nickname)
            }
            .addOnFailureListener {
                callback("Usuário")
            }
    }

    private fun carregarAvatares(container: LinearLayout, membros: List<String>) {
        container.removeAllViews()

        // Limitar a 4 membros (3 fotos + indicador de "+")
        val maxMembros = 3
        val membrosParaExibir = if (membros.size > maxMembros) {
            membros.take(3) // Mostrar só 3 fotos
        } else {
            membros
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

        // Se há mais membros que o limite, mostrar círculo com número
        if (membros.size > 3) {
            val numeroExtra = membros.size - 3
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

    inner class ProjetoViewHolder(
        val binding: ReusableLayoutMeusProjetosBinding
    ) : RecyclerView.ViewHolder(binding.root)
}