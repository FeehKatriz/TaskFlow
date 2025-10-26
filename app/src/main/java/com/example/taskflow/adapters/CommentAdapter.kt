package com.example.taskflow.ui.tarefa

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.taskflow.R
import com.example.taskflow.data.model.Comment
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class CommentAdapter(
    private val onDeleteClick: (Comment) -> Unit
) : ListAdapter<Comment, CommentAdapter.CommentViewHolder>(CommentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view, onDeleteClick)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CommentViewHolder(
        itemView: View,
        private val onDeleteClick: (Comment) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val userPhoto: ImageView = itemView.findViewById(R.id.ivUserPhoto)
        private val userName: TextView = itemView.findViewById(R.id.tvUserName)
        private val commentMessage: TextView = itemView.findViewById(R.id.tvCommentMessage)
        private val commentTime: TextView = itemView.findViewById(R.id.tvCommentTime)
        private val deleteButton: ImageView = itemView.findViewById(R.id.ivDeleteComment)

        fun bind(comment: Comment) {
            userName.text = comment.userName
            commentMessage.text = comment.message
            commentTime.text = formatTimestamp(comment.timestamp)

            // Carregar foto do usuário
            if (comment.userPhotoUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(comment.userPhotoUrl)
                    .transform(CircleCrop())
                    .placeholder(R.drawable.usertype)  // ✅ MUDADO AQUI
                    .error(R.drawable.usertype)        // ✅ MUDADO AQUI
                    .into(userPhoto)
            } else {
                userPhoto.setImageResource(R.drawable.usertype)  // ✅ MUDADO AQUI
            }

            // Mostrar botão de deletar apenas para o próprio usuário
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            deleteButton.visibility = if (currentUserId == comment.userId) {
                View.VISIBLE
            } else {
                View.GONE
            }

            deleteButton.setOnClickListener {
                onDeleteClick(comment)
            }
        }

        private fun formatTimestamp(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < 60_000 -> "Agora"
                diff < 3600_000 -> "${diff / 60_000}m"
                diff < 86400_000 -> "${diff / 3600_000}h"
                diff < 604800_000 -> "${diff / 86400_000}d"
                else -> {
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    sdf.format(Date(timestamp))
                }
            }
        }
    }

    class CommentDiffCallback : DiffUtil.ItemCallback<Comment>() {
        override fun areItemsTheSame(oldItem: Comment, newItem: Comment): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Comment, newItem: Comment): Boolean {
            return oldItem == newItem
        }
    }
}