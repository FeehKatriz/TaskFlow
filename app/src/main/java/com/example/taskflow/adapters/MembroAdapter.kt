package com.example.taskflow.adapters

import android.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
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
    private val onMembroRemovido: (() -> Unit)? = null
) : RecyclerView.Adapter<MembroAdapter.ViewHolder>() {

    private var membros = mutableListOf<Map<String, String>>()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    fun atualizarMembros(novosMembros: List<Map<String, String>>) {
        membros.clear()
        membros.addAll(novosMembros)
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
            binding.apply {
                // Nome do membro
                nomeMembro.text = membro["nome"] ?: "Usuário"

                // Cargo do membro
                cargoMembro.text = membro["tipo"] ?: "Membro"

                // Configurar cor do cargo baseado no tipo
                when (membro["tipo"]) {
                    "Criador" -> {
                        cargoMembro.setTextColor(
                            binding.root.context.getColor(android.R.color.holo_orange_dark)
                        )
                    }
                    "Você" -> {
                        cargoMembro.setTextColor(
                            binding.root.context.getColor(android.R.color.holo_blue_dark)
                        )
                    }
                    else -> {
                        cargoMembro.setTextColor(
                            binding.root.context.getColor(android.R.color.darker_gray)
                        )
                    }
                }

                // Carregar foto do perfil
                val userId = membro["uid"] ?: membro["id"] ?: ""
                if (userId.isNotEmpty()) {
                    carregarFotoPerfil(userId)
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
                // Usar imagem padrão
                Glide.with(binding.root.context)
                    .load(R.drawable.usertype)
                    .circleCrop()
                    .into(binding.imgUsuario)
            }
        }

        private fun configurarBotaoOpcoes(membro: Map<String, String>) {
            val usuarioAtualId = auth.currentUser?.uid ?: ""
            val membroId = membro["uid"] ?: membro["id"] ?: ""
            val tipoMembro = membro["tipo"] ?: ""
            val nomeMembro = membro["nome"] ?: "Usuário"

            binding.opcoes.setOnClickListener { view ->
                // Verificar se é o próprio usuário
                if (membroId == usuarioAtualId) {
                    mostrarMenuUsuarioAtual(view, nomeMembro)
                } else {
                    // Verificar se o usuário atual é o criador do projeto
                    verificarPermissaoEMostrarMenu(view, membro)
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

        private fun verificarPermissaoEMostrarMenu(view: View, membro: Map<String, String>) {
            val usuarioAtualId = auth.currentUser?.uid ?: ""

            // Verificar se o usuário atual é o criador do projeto
            firestore.collection("projetos")
                .document(projetoId)
                .get()
                .addOnSuccessListener { document ->
                    val criadorId = document.getString("criador")

                    if (usuarioAtualId == criadorId) {
                        // É o criador, pode remover outros membros
                        mostrarMenuCriador(view, membro)
                    } else {
                        // Não é o criador, não pode remover outros
                        Toast.makeText(
                            binding.root.context,
                            "Apenas o criador do projeto pode remover membros",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(
                        binding.root.context,
                        "Erro ao verificar permissões",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }

        private fun mostrarMenuCriador(view: View, membro: Map<String, String>) {
            val tipoMembro = membro["tipo"] ?: ""

            // Se for o criador tentando se remover, não permitir
            if (tipoMembro == "Criador") {
                Toast.makeText(
                    binding.root.context,
                    "O criador do projeto não pode ser removido",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            val popup = PopupMenu(binding.root.context, view)
            popup.menuInflater.inflate(R.menu.menu_membro_admin, popup.menu)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.remover_membro -> {
                        val nomeMembro = membro["nome"] ?: "Usuário"
                        mostrarDialogoRemoverMembro(membro, nomeMembro)
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

            // Mostrar loading
            Toast.makeText(
                binding.root.context,
                "Removendo membro...",
                Toast.LENGTH_SHORT
            ).show()

            // Processo sequencial para garantir que todas as operações sejam concluídas
            removerDoProjeto(membroId, nomeMembro, isSaindoPorConta)
        }

        private fun removerDoProjeto(membroId: String, nomeMembro: String, isSaindoPorConta: Boolean) {
            // 1. Remover do projeto
            firestore.collection("projetos")
                .document(projetoId)
                .update("membros", FieldValue.arrayRemove(membroId))
                .addOnSuccessListener {
                    // 2. Após remover do projeto, remover das equipes
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
            // Buscar todas as equipes do projeto
            firestore.collection("equipes")
                .whereEqualTo("projetoId", projetoId)
                .get()
                .addOnSuccessListener { equipesSnapshot ->
                    val equipesParaAtualizar = mutableListOf<String>()

                    // Identificar quais equipes contêm o membro
                    equipesSnapshot.documents.forEach { equipeDoc ->
                        val membrosEquipeIds = equipeDoc.get("membros") as? List<String> ?: emptyList()
                        if (membrosEquipeIds.contains(membroId)) {
                            equipesParaAtualizar.add(equipeDoc.id)
                        }
                    }

                    // Se não há equipes para atualizar, pular para tarefas
                    if (equipesParaAtualizar.isEmpty()) {
                        removerDasTarefas(membroId, nomeMembro, isSaindoPorConta)
                        return@addOnSuccessListener
                    }

                    // Remover das equipes encontradas
                    var equipesProcessadas = 0
                    equipesParaAtualizar.forEach { equipeId ->
                        firestore.collection("equipes")
                            .document(equipeId)
                            .update("membros", FieldValue.arrayRemove(membroId))
                            .addOnSuccessListener {
                                equipesProcessadas++
                                if (equipesProcessadas == equipesParaAtualizar.size) {
                                    // Todas as equipes foram processadas, agora processar tarefas
                                    removerDasTarefas(membroId, nomeMembro, isSaindoPorConta)
                                }
                            }
                            .addOnFailureListener { e ->
                                equipesProcessadas++
                                Log.e("MembroAdapter", "Erro ao remover da equipe $equipeId: ${e.message}")
                                if (equipesProcessadas == equipesParaAtualizar.size) {
                                    // Mesmo com erros, continuar para tarefas
                                    removerDasTarefas(membroId, nomeMembro, isSaindoPorConta)
                                }
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MembroAdapter", "Erro ao buscar equipes: ${e.message}")
                    // Mesmo com erro, tentar remover das tarefas
                    removerDasTarefas(membroId, nomeMembro, isSaindoPorConta)
                }
        }

        private fun removerDasTarefas(membroId: String, nomeMembro: String, isSaindoPorConta: Boolean) {
            // Buscar todas as tarefas do projeto
            firestore.collection("tarefas")
                .whereEqualTo("projetoId", projetoId)
                .get()
                .addOnSuccessListener { tarefasSnapshot ->
                    val tarefasParaAtualizar = mutableListOf<String>()

                    // Identificar quais tarefas contêm o membro
                    tarefasSnapshot.documents.forEach { tarefaDoc ->
                        val membrosTarefaIds = tarefaDoc.get("membros") as? List<String> ?: emptyList()
                        if (membrosTarefaIds.contains(membroId)) {
                            tarefasParaAtualizar.add(tarefaDoc.id)
                        }
                    }

                    // Se não há tarefas para atualizar, finalizar processo
                    if (tarefasParaAtualizar.isEmpty()) {
                        finalizarRemocao(nomeMembro, isSaindoPorConta)
                        return@addOnSuccessListener
                    }

                    // Remover das tarefas encontradas
                    var tarefasProcessadas = 0
                    tarefasParaAtualizar.forEach { tarefaId ->
                        firestore.collection("tarefas")
                            .document(tarefaId)
                            .update("membros", FieldValue.arrayRemove(membroId))
                            .addOnSuccessListener {
                                tarefasProcessadas++
                                if (tarefasProcessadas == tarefasParaAtualizar.size) {
                                    // Todas as tarefas foram processadas
                                    finalizarRemocao(nomeMembro, isSaindoPorConta)
                                }
                            }
                            .addOnFailureListener { e ->
                                tarefasProcessadas++
                                Log.e("MembroAdapter", "Erro ao remover da tarefa $tarefaId: ${e.message}")
                                if (tarefasProcessadas == tarefasParaAtualizar.size) {
                                    // Mesmo com erros, finalizar
                                    finalizarRemocao(nomeMembro, isSaindoPorConta)
                                }
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MembroAdapter", "Erro ao buscar tarefas: ${e.message}")
                    // Mesmo com erro, finalizar o processo
                    finalizarRemocao(nomeMembro, isSaindoPorConta)
                }
        }

        private fun finalizarRemocao(nomeMembro: String, isSaindoPorConta: Boolean) {
            val mensagem = if (isSaindoPorConta) {
                "Você saiu do projeto com sucesso"
            } else {
                "'$nomeMembro' foi removido do projeto e de todas as equipes/tarefas relacionadas"
            }

            Toast.makeText(
                binding.root.context,
                mensagem,
                Toast.LENGTH_LONG
            ).show()

            // Recarregar a lista de membros
            onMembroRemovido?.invoke()
        }
    }
}