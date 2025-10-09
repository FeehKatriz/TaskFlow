package com.example.taskflow.data.repository

import com.example.taskflow.data.model.Equipe
import com.example.taskflow.data.model.Projeto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.random.Random

class ProjetoRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun criarProjeto(
        nome: String,
        cor: String,
        callback: (Result<String>) -> Unit
    ) {
        val usuarioAtual = auth.currentUser
        if (usuarioAtual == null) {
            callback(Result.failure(Exception("Usuário não autenticado")))
            return
        }

        criarProjetoComCodigoUnico(nome, cor, usuarioAtual.uid, callback)
    }

    private fun criarProjetoComCodigoUnico(
        nome: String,
        cor: String,
        usuarioId: String,
        callback: (Result<String>) -> Unit
    ) {
        val codigo = gerarCodigoProjeto()

        verificarCodigoUnico(codigo) { isUnico ->
            if (isUnico) {
                // Código é único, criar o projeto
                salvarProjetoNoFirestore(nome, cor, usuarioId, codigo, callback)
            } else {
                // Código já existe, tentar novamente
                criarProjetoComCodigoUnico(nome, cor, usuarioId, callback)
            }
        }
    }

    private fun salvarProjetoNoFirestore(
        nome: String,
        cor: String,
        usuarioId: String,
        codigo: String,
        callback: (Result<String>) -> Unit
    ) {
        val projetoRef = db.collection("projetos").document()
        val projetoId = projetoRef.id

        val projeto = Projeto(
            id = projetoId,
            nome = nome,
            criador = usuarioId,
            membros = listOf(usuarioId),
            cor = cor,
            codigo = codigo
        )

        projetoRef.set(projeto)
            .addOnSuccessListener {
                callback(Result.success("Projeto '$nome' criado!\nCódigo: $codigo"))
            }
            .addOnFailureListener { e ->
                callback(Result.failure(e))
            }
    }

    private fun gerarCodigoProjeto(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..10)
            .map { chars[Random.Default.nextInt(chars.length)] }
            .joinToString("")
    }

    fun verificarCodigoUnico(codigo: String, callback: (Boolean) -> Unit) {
        db.collection("projetos")
            .whereEqualTo("codigo", codigo)
            .get()
            .addOnSuccessListener { documents ->
                callback(documents.isEmpty)
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    //projetos

    fun carregarProjetosDoUsuario(callback: (Result<List<Projeto>>) -> Unit) {
        val currentUserId = auth.currentUser?.uid ?: run {
            callback(Result.failure(Exception("Usuário não autenticado")))
            return
        }

        db.collection("projetos")
            .whereArrayContains("membros", currentUserId)
            .get()
            .addOnSuccessListener { documents ->
                val projetos = documents.map { doc ->
                    val projeto = doc.toObject(Projeto::class.java)
                    if (projeto.id.isEmpty()) {
                        projeto.id = doc.id
                    }
                    projeto
                }
                callback(Result.success(projetos))
            }
            .addOnFailureListener { exception ->
                callback(Result.failure(exception))
            }
    }

    fun entrarNoProjeto(codigo: String, callback: (Result<String>) -> Unit) {
        val currentUserId = auth.currentUser?.uid ?: run {
            callback(Result.failure(Exception("Usuário não autenticado")))
            return
        }

        db.collection("projetos")
            .whereEqualTo("codigo", codigo)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    callback(Result.failure(Exception("Código do projeto não encontrado")))
                    return@addOnSuccessListener
                }

                val projetoDoc = documents.first()
                val projeto = projetoDoc.toObject(Projeto::class.java)

                // Verificar se o usuário já está no projeto
                if (projeto.membros.contains(currentUserId)) {
                    callback(Result.failure(Exception("Você já faz parte deste projeto")))
                    return@addOnSuccessListener
                }

                // Adicionar o usuário ao projeto
                val novosMembros = projeto.membros.toMutableList()
                novosMembros.add(currentUserId)

                projetoDoc.reference.update("membros", novosMembros)
                    .addOnSuccessListener {
                        callback(Result.success("Você entrou no projeto: ${projeto.nome}"))
                    }
                    .addOnFailureListener { e ->
                        callback(Result.failure(e))
                    }
            }
            .addOnFailureListener { e ->
                callback(Result.failure(e))
            }
    }

    //projeto


    fun carregarInfoProjeto(
        projetoId: String,
        callback: (Result<Triple<String, String, Boolean>>) -> Unit
    ) {
        val usuarioAtualId = auth.currentUser?.uid ?: ""

        db.collection("projetos")
            .document(projetoId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val nomeProjeto = document.getString("nome") ?: "Projeto"
                    val codigoProjeto = document.getString("codigo") ?: ""
                    val criadorId = document.getString("criador") ?: ""
                    val isCreator = criadorId == usuarioAtualId

                    callback(Result.success(Triple(nomeProjeto, codigoProjeto, isCreator)))
                } else {
                    callback(Result.failure(Exception("Projeto não encontrado")))
                }
            }
            .addOnFailureListener { e ->
                callback(Result.failure(e))
            }
    }

    fun carregarEquipes(
        projetoId: String,
        callback: (Result<List<Equipe>>) -> Unit
    ) {
        db.collection("equipes")
            .whereEqualTo("projetoId", projetoId)
            .get()
            .addOnSuccessListener { equipesSnapshot ->
                val equipes = equipesSnapshot.documents.mapNotNull { equipeDoc ->
                    equipeDoc.toObject(Equipe::class.java)?.copy(
                        id = equipeDoc.id
                    )
                }
                callback(Result.success(equipes))
            }
            .addOnFailureListener { e ->
                callback(Result.failure(e))
            }
    }

    fun carregarMembros(
        projetoId: String,
        callback: (Result<List<Map<String, String>>>) -> Unit
    ) {
        val usuarioAtualId = auth.currentUser?.uid ?: return

        db.collection("projetos")
            .document(projetoId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val membrosIds = document.get("membros") as? List<String> ?: emptyList()
                    val criadorId = document.getString("criador")

                    if (membrosIds.isNotEmpty()) {
                        val membros = mutableListOf<Map<String, String>>()
                        var processedCount = 0

                        membrosIds.forEach { userId ->
                            db.collection("usuarios")
                                .document(userId)
                                .get()
                                .addOnSuccessListener { userDoc ->
                                    processedCount++

                                    if (userDoc.exists()) {
                                        val tipoMembro = if (userId == criadorId) "Criador" else "Membro"
                                        val nomeUsuario = userDoc.getString("nome") ?: "Usuário"
                                        val nomeExibir = if (userId == usuarioAtualId) "Você" else nomeUsuario

                                        val membro = mapOf(
                                            "uid" to userDoc.id,
                                            "id" to userDoc.id,
                                            "nome" to nomeExibir,
                                            "email" to (userDoc.getString("email") ?: ""),
                                            "tipo" to tipoMembro
                                        )
                                        membros.add(membro)
                                    }

                                    if (processedCount == membrosIds.size) {
                                        val membrosOrdenados = membros.sortedWith(compareBy<Map<String, String>> { membro ->
                                            when {
                                                membro["nome"] == "Você" -> 0
                                                membro["tipo"] == "Criador" -> 1
                                                else -> 2
                                            }
                                        }.thenBy { it["nome"] })

                                        callback(Result.success(membrosOrdenados))
                                    }
                                }
                                .addOnFailureListener { e ->
                                    processedCount++

                                    if (processedCount == membrosIds.size) {
                                        val membrosOrdenados = membros.sortedWith(compareBy<Map<String, String>> { membro ->
                                            when {
                                                membro["nome"] == "Você" -> 0
                                                membro["tipo"] == "Criador" -> 1
                                                else -> 2
                                            }
                                        }.thenBy { it["nome"] })
                                        callback(Result.success(membrosOrdenados))
                                    }
                                }
                        }
                    } else {
                        callback(Result.success(emptyList()))
                    }
                } else {
                    callback(Result.success(emptyList()))
                }
            }
            .addOnFailureListener { e ->
                callback(Result.failure(e))
            }
    }

    fun verificarSeUsuarioEstaNoProjeto(
        projetoId: String,
        callback: (Result<Boolean>) -> Unit
    ) {
        val usuarioAtualId = auth.currentUser?.uid ?: return

        db.collection("projetos")
            .document(projetoId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val membrosIds = document.get("membros") as? List<String> ?: emptyList()
                    callback(Result.success(membrosIds.contains(usuarioAtualId)))
                } else {
                    callback(Result.success(false))
                }
            }
            .addOnFailureListener { e ->
                callback(Result.failure(e))
            }
    }

    fun atualizarCodigoProjeto(
        projetoId: String,
        novoCodigo: String,
        callback: (Result<String>) -> Unit
    ) {
        db.collection("projetos")
            .document(projetoId)
            .update("codigo", novoCodigo)
            .addOnSuccessListener {
                callback(Result.success(novoCodigo))
            }
            .addOnFailureListener { e ->
                callback(Result.failure(e))
            }
    }


}