package com.example.taskflow.ui.notificacoes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.taskflow.R
import com.example.taskflow.data.model.Notificacao
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class NotificacaoAdapter(
    private val onItemClick: (Notificacao) -> Unit,
    private val onDeleteClick: (Notificacao) -> Unit
) : ListAdapter<Notificacao, NotificacaoAdapter.NotificacaoViewHolder>(NotificacaoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificacaoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notificacao, parent, false)
        return NotificacaoViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificacaoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NotificacaoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardNotificacao: MaterialCardView = itemView.findViewById(R.id.cardNotificacao)
        private val indicadorNaoLida: View = itemView.findViewById(R.id.indicadorNaoLida)
        private val ivIcone: ImageView = itemView.findViewById(R.id.ivIcone)
        private val tvTitulo: TextView = itemView.findViewById(R.id.tvTitulo)
        private val tvMensagem: TextView = itemView.findViewById(R.id.tvMensagem)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
        private val btnDeletar: ImageButton = itemView.findViewById(R.id.btnDeletar)

        fun bind(notificacao: Notificacao) {
            // Indicador de não lida
            indicadorNaoLida.isVisible = !notificacao.lida

            // Background do card (mais claro se já lida)
            cardNotificacao.alpha = if (notificacao.lida) 0.6f else 1.0f

            // Ícone baseado no tipo
            val iconeRes = when (notificacao.tipo) {
                Notificacao.TIPO_TAREFA_ATUALIZADA -> R.drawable.notificationbllue
                Notificacao.TIPO_TAREFA_CRIADA -> R.drawable.notificationbllue
                Notificacao.TIPO_NOVO_COMENTARIO -> R.drawable.notificationbllue
                else -> R.drawable.notificationbllue
            }
            ivIcone.setImageResource(iconeRes)

            // Título
            tvTitulo.text = notificacao.titulo

            // Mensagem
            tvMensagem.text = notificacao.mensagem

            // Timestamp relativo
            tvTimestamp.text = formatarTempoRelativo(notificacao.timestamp.toDate())

            // Click listeners
            cardNotificacao.setOnClickListener {
                onItemClick(notificacao)
            }

            btnDeletar.setOnClickListener {
                onDeleteClick(notificacao)
            }
        }

        private fun formatarTempoRelativo(data: Date): String {
            val agora = Date()
            val diff = agora.time - data.time

            return when {
                diff < TimeUnit.MINUTES.toMillis(1) -> "Agora"
                diff < TimeUnit.HOURS.toMillis(1) -> {
                    val minutos = TimeUnit.MILLISECONDS.toMinutes(diff)
                    "Há $minutos min"
                }
                diff < TimeUnit.DAYS.toMillis(1) -> {
                    val horas = TimeUnit.MILLISECONDS.toHours(diff)
                    "Há $horas h"
                }
                diff < TimeUnit.DAYS.toMillis(7) -> {
                    val dias = TimeUnit.MILLISECONDS.toDays(diff)
                    "Há $dias dias"
                }
                else -> {
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(data)
                }
            }
        }
    }

    class NotificacaoDiffCallback : DiffUtil.ItemCallback<Notificacao>() {
        override fun areItemsTheSame(oldItem: Notificacao, newItem: Notificacao): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Notificacao, newItem: Notificacao): Boolean {
            return oldItem == newItem
        }
    }
}