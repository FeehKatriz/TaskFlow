package com.example.taskflow.adapters

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.taskflow.R
import com.example.taskflow.data.model.Tarefa
import com.google.firebase.storage.FirebaseStorage

class TarefasAdapter(
    private var tarefas: List<Tarefa> = emptyList(),
    private val layoutRes: Int? = null,  // Agora é opcional
    private val onItemClick: (Tarefa) -> Unit = {}
) : RecyclerView.Adapter<TarefasAdapter.TarefaViewHolder>() {

    private val storage = FirebaseStorage.getInstance()

    override fun getItemCount(): Int = tarefas.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TarefaViewHolder {
        // Se layoutRes foi fornecido, usa ele (modo antigo - compatibilidade)
        // Se não, usa o viewType que corresponde ao layout baseado no status
        val layout = layoutRes ?: viewType

        val view = LayoutInflater.from(parent.context)
            .inflate(layout, parent, false)
        return TarefaViewHolder(view)
    }

    override fun getItemViewType(position: Int): Int {
        // Se layoutRes foi fornecido, ignora o viewType
        if (layoutRes != null) return 0

        // Retorna o layout baseado no status da tarefa
        return when (tarefas[position].status) {
            "pendente" -> R.layout.item_tarefa_afazer  // Roxo
            "em_andamento" -> R.layout.item_tarefa_andamento  // Amarelo
            "concluida" -> R.layout.item_tarefa_finalizada  // Verde
            else -> R.layout.item_tarefa_afazer  // Padrão roxo
        }
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

    inner class TarefaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titulo: TextView? = itemView.findViewById(R.id.taskNameText)
        private val progressBar: ProgressBar? = itemView.findViewById(R.id.progressBar)
        private val peopleLayout: FrameLayout? = itemView.findViewById(R.id.peopleLayout)

        fun bind(tarefa: Tarefa) {
            // Definir o texto do título
            titulo?.text = tarefa.titulo

            // Calcular progresso baseado no status
            val progresso = when (tarefa.status) {
                "pendente" -> 0
                "em_andamento" -> 50
                "concluida" -> 100
                else -> 0
            }
            progressBar?.progress = progresso

            // Carregar fotos dos responsáveis
            peopleLayout?.let { container ->
                carregarAvatares(container, tarefa.responsaveis)
            }
        }

        private fun carregarAvatares(container: FrameLayout, responsaveis: List<String>) {
            container.removeAllViews()

            if (responsaveis.isEmpty()) {
                // Se não há responsáveis, mostrar ícone padrão
                adicionarImagemPadrao(container, 0)
                return
            }

            // Limitar a 3 membros (2 fotos + indicador de "+X")
            val maxMembros = 2
            val responsaveisParaExibir = if (responsaveis.size > maxMembros) {
                responsaveis.take(2) // Mostrar só 2 fotos
            } else {
                responsaveis
            }

            // Adicionar as fotos dos responsáveis com sobreposição
            responsaveisParaExibir.forEachIndexed { index, userId ->
                adicionarAvatar(container, userId, index)
            }

            // Se há mais responsáveis que o limite, mostrar círculo com número
            if (responsaveis.size > maxMembros) {
                val numeroExtra = responsaveis.size - maxMembros
                adicionarIndicadorExtra(container, responsaveisParaExibir.size, numeroExtra)
            }
        }

        private fun adicionarAvatar(container: FrameLayout, userId: String, index: Int) {
            val imageView = ImageView(container.context)

            // Converter dp para px
            val size = (40 * container.context.resources.displayMetrics.density).toInt()
            val overlap = (22 * container.context.resources.displayMetrics.density).toInt()

            val params = FrameLayout.LayoutParams(size, size)
            params.marginStart = index * overlap
            imageView.layoutParams = params
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP

            // Adicionar borda branca para destacar a sobreposição
            imageView.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.WHITE)
                setStroke(4, Color.WHITE)
            }

            // Elevar a imagem para ficar por cima das anteriores
            imageView.elevation = (index + 1) * 2f

            val ref = storage.getReference("usuarios/$userId/fotoPerfil.jpg")
            ref.downloadUrl.addOnSuccessListener { uri ->
                Glide.with(container.context)
                    .load(uri)
                    .placeholder(R.drawable.user_img)
                    .circleCrop()
                    .into(imageView)
            }.addOnFailureListener {
                Glide.with(container.context)
                    .load(R.drawable.user_img)
                    .circleCrop()
                    .into(imageView)
            }

            container.addView(imageView)
        }

        private fun adicionarIndicadorExtra(container: FrameLayout, index: Int, numeroExtra: Int) {
            val extraImageView = ImageView(container.context)

            // Converter dp para px
            val size = (40 * container.context.resources.displayMetrics.density).toInt()
            val overlap = (22 * container.context.resources.displayMetrics.density).toInt()

            val params = FrameLayout.LayoutParams(size, size)
            params.marginStart = index * overlap
            extraImageView.layoutParams = params
            extraImageView.scaleType = ImageView.ScaleType.CENTER

            // Criar círculo com número
            extraImageView.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#A98B98")) // Cor do tema
                setStroke(4, Color.WHITE)
            }

            // Criar bitmap com o texto do número
            val bitmap = criarBitmapComTexto("+$numeroExtra", size, size)
            extraImageView.setImageBitmap(bitmap)

            // Elevar para ficar por cima de todas
            extraImageView.elevation = (index + 1) * 2f

            container.addView(extraImageView)
        }

        private fun adicionarImagemPadrao(container: FrameLayout, index: Int) {
            val imageView = ImageView(container.context)

            // Converter dp para px
            val size = (40 * container.context.resources.displayMetrics.density).toInt()
            val overlap = (22 * container.context.resources.displayMetrics.density).toInt()

            val params = FrameLayout.LayoutParams(size, size)
            params.marginStart = index * overlap
            imageView.layoutParams = params
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP

            // Adicionar borda branca
            imageView.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.WHITE)
                setStroke(4, Color.WHITE)
            }

            imageView.elevation = (index + 1) * 2f

            Glide.with(container.context)
                .load(R.drawable.add_afazer_img)
                .circleCrop()
                .into(imageView)

            container.addView(imageView)
        }

        private fun criarBitmapComTexto(texto: String, largura: Int, altura: Int): Bitmap {
            val textView = TextView(itemView.context)
            textView.text = texto
            textView.textSize = 16f
            textView.setTextColor(Color.WHITE)
            textView.gravity = Gravity.CENTER
            textView.setTypeface(null, Typeface.BOLD)

            textView.measure(
                View.MeasureSpec.makeMeasureSpec(largura, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(altura, View.MeasureSpec.EXACTLY)
            )
            textView.layout(0, 0, largura, altura)

            val bitmap = Bitmap.createBitmap(largura, altura, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            textView.draw(canvas)

            return bitmap
        }
    }
}