package com.example.taskflow.adapters

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.taskflow.R
import com.example.taskflow.databinding.ItemMembroBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class MembroAdapter(
    private val projetoId: String,
    private val onMembroRemovido: (() -> Unit)? = null,
    private val onPromoverAdmin: ((String) -> Unit)? = null,
    private val onRemoverAdmin: ((String) -> Unit)? = null
) : RecyclerView.Adapter<MembroAdapter.ViewHolder>() {

    private var membros = mutableListOf<Map<String, String>>()
    private var criadorId: String = ""
    private var adminsIds: List<String> = emptyList()
    private var isCreator: Boolean = false
    private var isAdmin: Boolean = false

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    fun atualizarMembros(
        novosMembros: List<Map<String, String>>,
        criador: String,
        admins: List<String>,
        userIsCreator: Boolean,
        userIsAdmin: Boolean
    ) {
        membros.clear()
        membros.addAll(novosMembros)
        criadorId = criador
        adminsIds = admins
        isCreator = userIsCreator
        isAdmin = userIsAdmin
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMembroBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val membro = membros[position]
        holder.bind(membro)
    }

    override fun getItemCount(): Int = membros.size

    inner class ViewHolder(private val binding: ItemMembroBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(membro: Map<String, String>) {
            val membroId = membro["uid"] ?: membro["id"] ?: ""

            binding.apply {
                // Nome do membro
                nomeMembro.text = membro["nome"] ?: "Usuário"

                //  Mostrar texto de cargo + drawable de coroa
                cargoMembro.visibility = View.VISIBLE
                cargoMembro.visibility = View.VISIBLE
                when {
                    membroId == criadorId -> {
                        cargoMembro.text = "Criador"
                        cargoMembro.setTextColor(
                            ContextCompat.getColor(binding.root.context, android.R.color.holo_orange_dark)
                        )
                        // Define a coroa de ouro à DIREITA do texto
                        cargoMembro.setCompoundDrawablesWithIntrinsicBounds(
                            0, // left
                            0, // top
                            R.drawable.ic_crown_gold, // right (AQUI!)
                            0  // bottom
                        )
                        // Espaçamento entre o texto e o drawable
                        cargoMembro.compoundDrawablePadding = 8.dpToPx(binding.root.context)
                    }
                    adminsIds.contains(membroId) -> {
                        cargoMembro.text = "Administrador"
                        cargoMembro.setTextColor(
                            ContextCompat.getColor(binding.root.context, android.R.color.holo_blue_dark)
                        )
                        // Define a coroa de prata à DIREITA do texto
                        cargoMembro.setCompoundDrawablesWithIntrinsicBounds(
                            0, // left
                            0, // top
                            R.drawable.ic_crown_silver, // right (AQUI!)
                            0  // bottom
                        )
                        cargoMembro.compoundDrawablePadding = 8.dpToPx(binding.root.context)
                    }
                    else -> {
                        cargoMembro.text = "Membro"
                        cargoMembro.setTextColor(
                            ContextCompat.getColor(binding.root.context, android.R.color.darker_gray)
                        )
                        // Remove qualquer drawable
                        cargoMembro.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                    }
                }

                // Carregar foto do perfil
                if (membroId.isNotEmpty()) {
                    carregarFotoPerfil(membroId)
                }

                // Configurar clique no botão de opções
                configurarBotaoOpcoes(membro)
            }
        }

        private fun carregarFotoPerfil(userId: String) {
            val ref = storage.getReference("usuarios/$userId/fotoPerfil.jpg")
            ref.downloadUrl.addOnSuccessListener { uri ->
                Glide.with(binding.root.context)
                    .load(uri)
                    .placeholder(R.drawable.usertype)
                    .circleCrop()
                    .into(binding.imgUsuario)
            }.addOnFailureListener {
                Glide.with(binding.root.context)
                    .load(R.drawable.usertype)
                    .circleCrop()
                    .into(binding.imgUsuario)
            }
        }

        private fun configurarBotaoOpcoes(membro: Map<String, String>) {
            val usuarioAtualId = auth.currentUser?.uid ?: ""
            val membroId = membro["uid"] ?: membro["id"] ?: ""
            val nomeMembro = membro["nome"] ?: "Usuário"

            binding.opcoes.visibility = when {
                membroId == usuarioAtualId -> View.VISIBLE
                membroId == criadorId -> View.GONE
                isCreator -> View.VISIBLE
                isAdmin && !adminsIds.contains(membroId) -> View.VISIBLE
                else -> View.GONE
            }

            binding.opcoes.setOnClickListener { view ->
                if (membroId == usuarioAtualId) {
                    mostrarMenuUsuarioAtual(view, nomeMembro)
                } else {
                    mostrarMenuGerenciar(view, membro)
                }
            }
        }

        private fun mostrarMenuUsuarioAtual(view: View, nomeMembro: String) {
            val popup = PopupMenu(binding.root.context, view)
            popup.menuInflater.inflate(R.menu.menu_membro_proprio, popup.menu)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.sair_equipe -> {
                        mostrarDialogoSairProjeto(nomeMembro)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        private fun mostrarMenuGerenciar(view: View, membro: Map<String, String>) {
            val membroId = membro["uid"] ?: membro["id"] ?: ""
            val isMembroAdmin = adminsIds.contains(membroId)

            val popup = PopupMenu(binding.root.context, view)

            when {
                isCreator -> {
                    if (isMembroAdmin) {
                        popup.menu.add("Remover privilégios de admin")
                    } else {
                        popup.menu.add("Promover a administrador")
                    }
                    popup.menu.add("Remover do projeto")
                }
                isAdmin && !isMembroAdmin -> {
                    popup.menu.add("Remover do projeto")
                }
            }

            popup.setOnMenuItemClickListener { item ->
                when (item.title.toString()) {
                    "Promover a administrador" -> {
                        onPromoverAdmin?.invoke(membroId)
                        true
                    }
                    "Remover privilégios de admin" -> {
                        onRemoverAdmin?.invoke(membroId)
                        true
                    }
                    "Remover do projeto" -> {
                        mostrarDialogoRemoverMembro(membro, membro["nome"] ?: "Usuário")
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        private fun mostrarDialogoSairProjeto(nomeMembro: String) {
            AlertDialog.Builder(binding.root.context)
                .setTitle("Sair do Projeto")
                .setMessage("Tem certeza que deseja sair deste projeto? Você será removido de todas as equipes e tarefas relacionadas.")
                .setPositiveButton("Sair") { _, _ ->
                    val usuarioAtualId = auth.currentUser?.uid ?: ""
                    removerMembroDoProjeto(usuarioAtualId, nomeMembro, true)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        private fun mostrarDialogoRemoverMembro(membro: Map<String, String>, nomeMembro: String) {
            AlertDialog.Builder(binding.root.context)
                .setTitle("Remover Membro")
                .setMessage("Tem certeza que deseja remover '$nomeMembro' do projeto? O membro será removido de todas as equipes e tarefas relacionadas.")
                .setPositiveButton("Remover") { _, _ ->
                    val membroId = membro["uid"] ?: membro["id"] ?: ""
                    removerMembroDoProjeto(membroId, nomeMembro, false)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        private fun removerMembroDoProjeto(membroId: String, nomeMembro: String, isSaindoPorConta: Boolean) {
            if (membroId.isEmpty()) {
                Toast.makeText(
                    binding.root.context,
                    "Erro: ID do membro não encontrado",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            removerDoProjeto(membroId, nomeMembro, isSaindoPorConta)
        }

        private fun removerDoProjeto(membroId: String, nomeMembro: String, isSaindoPorConta: Boolean) {
            firestore.collection("projetos")
                .document(projetoId)
                .update(
                    mapOf(
                        "membros" to FieldValue.arrayRemove(membroId),
                        "admins" to FieldValue.arrayRemove(membroId)
                    )
                )
                .addOnSuccessListener {
                    removerDasEquipes(membroId, nomeMembro, isSaindoPorConta)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        binding.root.context,
                        "Erro ao remover do projeto: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }

        private fun removerDasEquipes(membroId: String, nomeMembro: String, isSaindoPorConta: Boolean) {
            firestore.collection("equipes")
                .whereEqualTo("projetoId", projetoId)
                .get()
                .addOnSuccessListener { equipesSnapshot ->
                    val equipesParaAtualizar = mutableListOf<String>()

                    equipesSnapshot.documents.forEach { equipeDoc ->
                        val membrosEquipeIds = equipeDoc.get("membros") as? List<String> ?: emptyList()
                        if (membrosEquipeIds.contains(membroId)) {
                            equipesParaAtualizar.add(equipeDoc.id)
                        }
                    }

                    if (equipesParaAtualizar.isEmpty()) {
                        removerDasTarefas(membroId, nomeMembro, isSaindoPorConta)
                        return@addOnSuccessListener
                    }

                    var equipesProcessadas = 0
                    equipesParaAtualizar.forEach { equipeId ->
                        firestore.collection("equipes")
                            .document(equipeId)
                            .update("membros", FieldValue.arrayRemove(membroId))
                            .addOnSuccessListener {
                                equipesProcessadas++
                                if (equipesProcessadas == equipesParaAtualizar.size) {
                                    removerDasTarefas(membroId, nomeMembro, isSaindoPorConta)
                                }
                            }
                            .addOnFailureListener { e ->
                                equipesProcessadas++
                                Log.e("MembroAdapter", "Erro ao remover da equipe $equipeId: ${e.message}")
                                if (equipesProcessadas == equipesParaAtualizar.size) {
                                    removerDasTarefas(membroId, nomeMembro, isSaindoPorConta)
                                }
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MembroAdapter", "Erro ao buscar equipes: ${e.message}")
                    removerDasTarefas(membroId, nomeMembro, isSaindoPorConta)
                }
        }

        private fun removerDasTarefas(membroId: String, nomeMembro: String, isSaindoPorConta: Boolean) {
            firestore.collection("tarefas")
                .whereEqualTo("projetoId", projetoId)
                .get()
                .addOnSuccessListener { tarefasSnapshot ->
                    val tarefasParaRemoverDeMembros = mutableListOf<String>()
                    val tarefasParaRemoverDeResponsaveis = mutableListOf<Pair<String, List<String>>>()

                    tarefasSnapshot.documents.forEach { tarefaDoc ->
                        val membrosTarefaIds = tarefaDoc.get("membros") as? List<String> ?: emptyList()
                        val responsaveisIds = tarefaDoc.get("responsaveis") as? List<String> ?: emptyList()
                        if (membrosTarefaIds.contains(membroId)) {
                            tarefasParaRemoverDeMembros.add(tarefaDoc.id)
                        }
                        if (responsaveisIds.contains(membroId)) {
                            val novosResponsaveis = responsaveisIds.filter { it != membroId }
                            tarefasParaRemoverDeResponsaveis.add(Pair(tarefaDoc.id, novosResponsaveis))
                        }
                    }

                    val totalTarefas = tarefasParaRemoverDeMembros.size + tarefasParaRemoverDeResponsaveis.size
                    if (totalTarefas == 0) {
                        finalizarRemocao(nomeMembro, isSaindoPorConta)
                        return@addOnSuccessListener
                    }

                    var tarefasProcessadas = 0
                    // Remover membro da lista de membros
                    tarefasParaRemoverDeMembros.forEach { tarefaId ->
                        firestore.collection("tarefas")
                            .document(tarefaId)
                            .update("membros", FieldValue.arrayRemove(membroId))
                            .addOnSuccessListener {
                                tarefasProcessadas++
                                if (tarefasProcessadas == totalTarefas) {
                                    finalizarRemocao(nomeMembro, isSaindoPorConta)
                                }
                            }
                            .addOnFailureListener { e ->
                                tarefasProcessadas++
                                Log.e("MembroAdapter", "Erro ao remover da tarefa $tarefaId: ${e.message}")
                                if (tarefasProcessadas == totalTarefas) {
                                    finalizarRemocao(nomeMembro, isSaindoPorConta)
                                }
                            }
                    }
                    // Remover membro do array de responsáveis
                    tarefasParaRemoverDeResponsaveis.forEach { (tarefaId, novosResponsaveis) ->
                        // Aqui seria ideal usar o TarefaRepository, mas como estamos no Adapter, vamos atualizar direto
                        firestore.collection("tarefas")
                            .document(tarefaId)
                            .update("responsaveis", novosResponsaveis)
                            .addOnSuccessListener {
                                tarefasProcessadas++
                                if (tarefasProcessadas == totalTarefas) {
                                    finalizarRemocao(nomeMembro, isSaindoPorConta)
                                }
                            }
                            .addOnFailureListener { e ->
                                tarefasProcessadas++
                                Log.e("MembroAdapter", "Erro ao remover dos responsáveis da tarefa $tarefaId: ${e.message}")
                                if (tarefasProcessadas == totalTarefas) {
                                    finalizarRemocao(nomeMembro, isSaindoPorConta)
                                }
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MembroAdapter", "Erro ao buscar tarefas: ${e.message}")
                    finalizarRemocao(nomeMembro, isSaindoPorConta)
                }
        }

        private fun finalizarRemocao(nomeMembro: String, isSaindoPorConta: Boolean) {
            onMembroRemovido?.invoke()
        }
    }

    // auxiliar para converter dp para px
    private fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }
}