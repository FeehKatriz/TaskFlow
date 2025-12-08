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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.*

class CommentAdapter(
    private val onDeleteClick: (Comment) -> Unit,
    private val isAdminOuCriador: () -> Boolean = { false }
) : ListAdapter<Comment, CommentAdapter.CommentViewHolder>(CommentDiffCallback()) {

    private val storage = FirebaseStorage.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view, onDeleteClick, isAdminOuCriador, storage, firestore)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CommentViewHolder(
        itemView: View,
        private val onDeleteClick: (Comment) -> Unit,
        private val isAdminOuCriador: () -> Boolean,
        private val storage: FirebaseStorage,
        private val firestore: FirebaseFirestore
    ) : RecyclerView.ViewHolder(itemView) {

        private val userPhoto: ImageView = itemView.findViewById(R.id.ivUserPhoto)
        private val userName: TextView = itemView.findViewById(R.id.tvUserName)
        private val commentMessage: TextView = itemView.findViewById(R.id.tvCommentMessage)
        private val commentTime: TextView = itemView.findViewById(R.id.tvCommentTime)
        private val deleteButton: ImageView = itemView.findViewById(R.id.ivDeleteComment)

        fun bind(comment: Comment) {
            // BUSCAR NOME EM TEMPO REAL
            carregarNomeUsuario(comment.userId)

            commentMessage.text = comment.message
            commentTime.text = formatTimestamp(comment.timestamp)

            // Buscar foto em tempo real
            carregarFotoUsuario(comment.userId)

            // Mostrar botão de deletar para:
            // 1. O próprio autor do comentário
            // 2. Admin/Criador do projeto (pode excluir qualquer comentário)
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            val isAutor = currentUserId == comment.userId
            val podeExcluir = isAutor || isAdminOuCriador()
            
            deleteButton.visibility = if (podeExcluir) View.VISIBLE else View.GONE

            deleteButton.setOnClickListener {
                onDeleteClick(comment)
            }
        }

        private fun carregarNomeUsuario(userId: String) {
            firestore.collection("usuarios")
                .document(userId)
                .get()
                .addOnSuccessListener { document ->
                    val nome = document.getString("nome") ?: "Usuário"
                    userName.text = nome
                }
                .addOnFailureListener {
                    userName.text = "Usuário"
                }
        }

        private fun carregarFotoUsuario(userId: String) {
            val ref = storage.getReference("usuarios/$userId/fotoPerfil.jpg")

            ref.downloadUrl
                .addOnSuccessListener { uri ->
                    Glide.with(itemView.context)
                        .load(uri)
                        .transform(CircleCrop())
                        .placeholder(R.drawable.usertype)
                        .error(R.drawable.usertype)
                        .into(userPhoto)
                }
                .addOnFailureListener {
                    // Se não existir no Storage, tentar buscar a URL salva no Firestore
                    firestore.collection("usuarios")
                        .document(userId)
                        .get()
                        .addOnSuccessListener { doc ->
                            val foto = doc?.getString("fotoUrl")
                            if (!foto.isNullOrEmpty()) {
                                Glide.with(itemView.context)
                                    .load(foto)
                                    .transform(CircleCrop())
                                    .placeholder(R.drawable.usertype)
                                    .error(R.drawable.usertype)
                                    .into(userPhoto)
                            } else {
                                Glide.with(itemView.context)
                                    .load(R.drawable.usertype)
                                    .transform(CircleCrop())
                                    .into(userPhoto)
                            }
                        }
                        .addOnFailureListener {
                            Glide.with(itemView.context)
                                .load(R.drawable.usertype)
                                .transform(CircleCrop())
                                .into(userPhoto)
                        }
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