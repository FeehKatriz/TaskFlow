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
    private val equipeId: String,
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
                    // Verificar se o usuário atual é o criador da equipe
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
                        mostrarDialogoSairEquipe(nomeMembro)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        private fun verificarPermissaoEMostrarMenu(view: View, membro: Map<String, String>) {
            val usuarioAtualId = auth.currentUser?.uid ?: ""

            // Verificar se o usuário atual é o criador da equipe
            firestore.collection("equipes")
                .document(equipeId)
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
                            "Apenas o criador da equipe pode remover membros",
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
                    "O criador da equipe não pode ser removido",
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

        private fun mostrarDialogoSairEquipe(nomeMembro: String) {
            AlertDialog.Builder(binding.root.context)
                .setTitle("Sair da Equipe")
                .setMessage("Tem certeza que deseja sair desta equipe? Você será removido de todos os projetos e tarefas relacionados.")
                .setPositiveButton("Sair") { _, _ ->
                    val usuarioAtualId = auth.currentUser?.uid ?: ""
                    removerMembroDaEquipe(usuarioAtualId, nomeMembro, true)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        private fun mostrarDialogoRemoverMembro(membro: Map<String, String>, nomeMembro: String) {
            AlertDialog.Builder(binding.root.context)
                .setTitle("Remover Membro")
                .setMessage("Tem certeza que deseja remover '$nomeMembro' da equipe? O membro será removido de todos os projetos e tarefas relacionados.")
                .setPositiveButton("Remover") { _, _ ->
                    val membroId = membro["uid"] ?: membro["id"] ?: ""
                    removerMembroDaEquipe(membroId, nomeMembro, false)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        private fun removerMembroDaEquipe(membroId: String, nomeMembro: String, isSaindoPorConta: Boolean) {
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
            removerDaEquipe(membroId, nomeMembro, isSaindoPorConta)
        }

        private fun removerDaEquipe(membroId: String, nomeMembro: String, isSaindoPorConta: Boolean) {
            // 1. Remover da equipe
            firestore.collection("equipes")
                .document(equipeId)
                .update("membros", FieldValue.arrayRemove(membroId))
                .addOnSuccessListener {
                    // 2. Após remover da equipe, remover dos projetos
                    removerDosProjetos(membroId, nomeMembro, isSaindoPorConta)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        binding.root.context,
                        "Erro ao remover da equipe: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }

        private fun removerDosProjetos(membroId: String, nomeMembro: String, isSaindoPorConta: Boolean) {
            // Buscar todos os projetos da equipe
            firestore.collection("projetos")
                .whereEqualTo("equipeId", equipeId)
                .get()
                .addOnSuccessListener { projetosSnapshot ->
                    val projetosParaAtualizar = mutableListOf<String>()

                    // Identificar quais projetos contêm o membro
                    projetosSnapshot.documents.forEach { projetoDoc ->
                        val membrosProjetIds = projetoDoc.get("membros") as? List<String> ?: emptyList()
                        if (membrosProjetIds.contains(membroId)) {
                            projetosParaAtualizar.add(projetoDoc.id)
                        }
                    }

                    // Se não há projetos para atualizar, pular para tarefas
                    if (projetosParaAtualizar.isEmpty()) {
                        removerDasTarefas(membroId, nomeMembro, isSaindoPorConta)
                        return@addOnSuccessListener
                    }

                    // Remover dos projetos encontrados
                    var projetosProcessados = 0
                    projetosParaAtualizar.forEach { projetoId ->
                        firestore.collection("projetos")
                            .document(projetoId)
                            .update("membros", FieldValue.arrayRemove(membroId))
                            .addOnSuccessListener {
                                projetosProcessados++
                                if (projetosProcessados == projetosParaAtualizar.size) {
                                    // Todos os projetos foram processados, agora processar tarefas
                                    removerDasTarefas(membroId, nomeMembro, isSaindoPorConta)
                                }
                            }
                            .addOnFailureListener { e ->
                                projetosProcessados++
                                Log.e("MembroAdapter", "Erro ao remover do projeto $projetoId: ${e.message}")
                                if (projetosProcessados == projetosParaAtualizar.size) {
                                    // Mesmo com erros, continuar para tarefas
                                    removerDasTarefas(membroId, nomeMembro, isSaindoPorConta)
                                }
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MembroAdapter", "Erro ao buscar projetos: ${e.message}")
                    // Mesmo com erro, tentar remover das tarefas
                    removerDasTarefas(membroId, nomeMembro, isSaindoPorConta)
                }
        }

        private fun removerDasTarefas(membroId: String, nomeMembro: String, isSaindoPorConta: Boolean) {
            // Buscar todas as tarefas da equipe
            firestore.collection("tarefas")
                .whereEqualTo("equipeId", equipeId)
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
                "Você saiu da equipe com sucesso"
            } else {
                "'$nomeMembro' foi removido da equipe e de todos os projetos/tarefas relacionados"
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