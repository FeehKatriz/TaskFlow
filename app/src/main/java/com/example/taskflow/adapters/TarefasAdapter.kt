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
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.taskflow.R
import com.example.taskflow.data.model.Tarefa
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.*

class TarefasAdapter(
    private var tarefas: List<Tarefa> = emptyList(),
    private val onItemClick: (Tarefa) -> Unit = {}
) : RecyclerView.Adapter<TarefasAdapter.TarefaViewHolder>() {

    private val storage = FirebaseStorage.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault())
    private val dateFormatDisplay = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun getItemCount(): Int = tarefas.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TarefaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tarefa, parent, false)
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

    inner class TarefaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titulo: TextView? = itemView.findViewById(R.id.taskNameText)
        private val statusText: TextView? = itemView.findViewById(R.id.taskStatusText)
        private val dateText: TextView? = itemView.findViewById(R.id.taskDateText)
        private val peopleLayout: FrameLayout? = itemView.findViewById(R.id.peopleLayout)
        private val container: androidx.constraintlayout.widget.ConstraintLayout? =
            itemView.findViewById(R.id.taskContainer)

        fun bind(tarefa: Tarefa) {
            // Definir o texto do título
            titulo?.text = tarefa.titulo

            // Verificar se a tarefa está atrasada
            val estaAtrasada = verificarSeEstaAtrasada(tarefa)

            // Definir cores baseado no status (ou se está atrasada)
            val cores = when {
                estaAtrasada && tarefa.status != "concluida" -> Cores(
                    background = "#e74c3c",
                    addIcon = R.drawable.add_afazer_img
                )
                tarefa.status == "pendente" -> Cores(
                    background = "#DEB8C9",
                    addIcon = R.drawable.add_afazer_img
                )
                tarefa.status == "em_andamento" -> Cores(
                    background = "#FFD7A6",
                    addIcon = R.drawable.add_andamento_img
                )
                tarefa.status == "concluida" -> Cores(
                    background = "#B8E19B",
                    addIcon = R.drawable.add_finalizada_img
                )
                else -> Cores(
                    background = "#DEB8C9",
                    addIcon = R.drawable.add_afazer_img
                )
            }

            // Aplicar cor de fundo
            container?.setBackgroundColor(Color.parseColor(cores.background))

            // Definir texto do status
            val statusTexto = when {
                estaAtrasada && tarefa.status != "concluida" -> "Atrasada"
                tarefa.status == "pendente" -> "Pendente"
                tarefa.status == "em_andamento" -> "Em Andamento"
                tarefa.status == "concluida" -> "Concluída"
                else -> "Pendente"
            }
            statusText?.text = statusTexto
            statusText?.setTextColor(Color.parseColor("#2a2a2a"))

            // Definir data de vencimento
            if (!tarefa.dataVencimento.isNullOrEmpty()) {
                try {
                    val data = dateFormat.parse(tarefa.dataVencimento)
                    dateText?.text = dateFormatDisplay.format(data)
                    dateText?.visibility = View.VISIBLE
                } catch (e: Exception) {
                    dateText?.visibility = View.GONE
                }
            } else {
                dateText?.visibility = View.GONE
            }

            // Carregar fotos dos responsáveis
            peopleLayout?.let { container ->
                carregarAvatares(container, tarefa.responsaveis, cores.addIcon)
            }
        }

        private fun verificarSeEstaAtrasada(tarefa: Tarefa): Boolean {
            // Se não tem data de vencimento, não está atrasada
            if (tarefa.dataVencimento.isNullOrEmpty()) return false

            return try {
                val dataPrazo = dateFormat.parse(tarefa.dataVencimento)
                if (dataPrazo != null) {
                    val agora = Calendar.getInstance().time
                    // Está atrasada se a data de vencimento já passou
                    dataPrazo.before(agora)
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
        }

        private fun carregarAvatares(
            container: FrameLayout,
            responsaveis: List<String>,
            addIcon: Int
        ) {
            container.removeAllViews()

            if (responsaveis.isEmpty()) {
                // Se não há responsáveis, esconder o container
                container.visibility = View.GONE
                return
            }

            container.visibility = View.VISIBLE

            // Máximo de 3 elementos no total (fotos + indicador)
            // Se tem mais de 3 responsáveis, mostra 2 fotos + indicador "+X"
            val mostrarIndicador = responsaveis.size > 3
            val quantidadeFotos = if (mostrarIndicador) 2 else responsaveis.size

            // Adicionar as fotos dos responsáveis com sobreposição
            responsaveis.take(quantidadeFotos).forEachIndexed { index, userId ->
                adicionarAvatar(container, userId, index)
            }

            // Se há mais de 3 responsáveis, mostrar círculo com número
            if (mostrarIndicador) {
                val numeroExtra = responsaveis.size - 2
                adicionarIndicadorExtra(container, quantidadeFotos, numeroExtra)
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
                setColor(Color.parseColor("#34495e")) // Cor do tema
                setStroke(4, Color.WHITE)
            }

            // Criar bitmap com o texto do número
            val bitmap = criarBitmapComTexto("+$numeroExtra", size, size)
            extraImageView.setImageBitmap(bitmap)

            // Elevar para ficar por cima de todas
            extraImageView.elevation = (index + 1) * 2f

            container.addView(extraImageView)
        }

        private fun adicionarImagemPadrao(container: FrameLayout, index: Int, addIcon: Int) {
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
                .load(addIcon)
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

    // Data class para organizar as cores de cada status
    private data class Cores(
        val background: String,
        val addIcon: Int
    )
}