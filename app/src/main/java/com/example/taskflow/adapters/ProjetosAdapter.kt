package com.example.taskflow.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.taskflow.R
import com.example.taskflow.databinding.ReusableLayoutMeusProjetosBinding
import com.example.taskflow.data.model.Projeto
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class ProjetosAdapter(
    private val onItemClick: (Projeto) -> Unit
) : RecyclerView.Adapter<ProjetosAdapter.ProjetoViewHolder>() {

    private var projetos = mutableListOf<Projeto>()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val nomeCache = mutableMapOf<String, String>()

    fun atualizarProjetos(novosProjetos: List<Projeto>) {
        projetos.clear()
        projetos.addAll(novosProjetos)
        nomeCache.clear()
        notifyDataSetChanged()
    }

    fun limparCache() {
        nomeCache.clear()
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

        holder.binding.apply {
            // Nome do projeto
            textView7.text = projeto.nome

            // Buscar nome do criador
            loadNome(projeto.criador) { nome ->
                textView45.text = "por $nome"
            }

            // Aplicar cor no header e criar gradiente no body
            try {
                val color = Color.parseColor(projeto.cor)

                // Header com cor sólida
                headerColorido.setBackgroundColor(color)

                // Body com gradiente da mesma cor (20% -> 12% opacidade)
                val startColor = Color.argb(51, Color.red(color), Color.green(color), Color.blue(color))
                val endColor = Color.argb(31, Color.red(color), Color.green(color), Color.blue(color))

                val gradientDrawable = android.graphics.drawable.GradientDrawable(
                    android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                    intArrayOf(startColor, endColor)
                )
                bodyGradiente.background = gradientDrawable

            } catch (e: Exception) {
                // Cor padrão caso haja erro
                headerColorido.setBackgroundColor(Color.parseColor("#3F51B5"))
                val defaultStartColor = Color.argb(51, 63, 81, 181)
                val defaultEndColor = Color.argb(31, 63, 81, 181)
                val defaultGradient = android.graphics.drawable.GradientDrawable(
                    android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                    intArrayOf(defaultStartColor, defaultEndColor)
                )
                bodyGradiente.background = defaultGradient
            }

            // Carregar avatares dos membros
            carregarAvatares(containerIntegrantes, projeto.membros)

            // Click no card
            root.setOnClickListener {
                onItemClick(projeto)
            }
        }
    }

    private fun loadNome(userId: String, callback: (String) -> Unit) {
        if (nomeCache.containsKey(userId)) {
            callback(nomeCache[userId] ?: "Usuário")
            return
        }

        firestore.collection("usuarios")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                val nome = document.getString("nome") ?: "Usuário"
                nomeCache[userId] = nome
                callback(nome)
            }
            .addOnFailureListener {
                callback("Usuário")
            }
    }

    private fun carregarAvatares(container: LinearLayout, membros: List<String>) {
        container.removeAllViews()

        val mostrarIndicador = membros.size > 10
        val quantidadeFotos = if (mostrarIndicador) 9 else membros.size

        membros.take(quantidadeFotos).forEachIndexed { index, userId ->
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
                // tentar buscar fotoUrl no Firestore
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("usuarios")
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
            val numeroExtra = membros.size - 9
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

    inner class ProjetoViewHolder(
        val binding: ReusableLayoutMeusProjetosBinding
    ) : RecyclerView.ViewHolder(binding.root)
}