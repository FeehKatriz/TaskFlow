package com.example.taskflow.data.repository

import com.example.taskflow.data.model.Equipe
import com.example.taskflow.data.model.Projeto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
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
                salvarProjetoNoFirestore(nome, cor, usuarioId, codigo, callback)
            } else {
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
            admins = emptyList(), // ✅ NOVO: inicializar lista vazia de admins
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

                if (projeto.membros.contains(currentUserId)) {
                    callback(Result.failure(Exception("Você já faz parte deste projeto")))
                    return@addOnSuccessListener
                }

                if (projeto.membros.size >= 25) {
                    callback(Result.failure(Exception("Este projeto já atingiu o limite máximo de 25 membros")))
                    return@addOnSuccessListener
                }

                db.runTransaction { transaction ->
                    val freshSnapshot = transaction.get(projetoDoc.reference)
                    val membrosAtuais = freshSnapshot.get("membros") as? List<String> ?: emptyList()

                    if (membrosAtuais.size >= 25) {
                        throw Exception("Este projeto já atingiu o limite máximo de 25 membros")
                    }

                    if (membrosAtuais.contains(currentUserId)) {
                        throw Exception("Você já faz parte deste projeto")
                    }

                    val novosMembros = membrosAtuais.toMutableList()
                    novosMembros.add(currentUserId)
                    transaction.update(projetoDoc.reference, "membros", novosMembros)

                    projeto.nome
                }.addOnSuccessListener { nomeProjeto ->
                    callback(Result.success("Você entrou no projeto: $nomeProjeto"))
                }.addOnFailureListener { e ->
                    callback(Result.failure(e))
                }
            }
            .addOnFailureListener { e ->
                callback(Result.failure(e))
            }
    }

    // ✅ MODIFICADO: retornar 4 valores (incluindo isAdmin)
    fun carregarInfoProjeto(
        projetoId: String,
        callback: (Result<Quadruple<String, String, Boolean, Boolean>>) -> Unit
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
                    val admins = document.get("admins") as? List<String> ?: emptyList()

                    val isCreator = criadorId == usuarioAtualId
                    val isAdmin = admins.contains(usuarioAtualId)

                    callback(Result.success(Quadruple(nomeProjeto, codigoProjeto, isCreator, isAdmin)))
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

    // ✅ MODIFICADO: retornar também criadorId e adminsIds
    fun carregarMembros(
        projetoId: String,
        callback: (Result<Triple<List<Map<String, String>>, String, List<String>>>) -> Unit
    ) {
        val usuarioAtualId = auth.currentUser?.uid ?: return

        db.collection("projetos")
            .document(projetoId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val membrosIds = document.get("membros") as? List<String> ?: emptyList()
                    val criadorId = document.getString("criador") ?: ""
                    val adminsIds = document.get("admins") as? List<String> ?: emptyList()

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
                                        // ✅ MODIFICADO: determinar tipo baseado em criador/admin
                                        val tipoMembro = when {
                                            userId == criadorId -> "Criador"
                                            adminsIds.contains(userId) -> "Admin"
                                            else -> "Membro"
                                        }

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
                                        // ✅ MODIFICADO: ordenação incluindo admins
                                        val membrosOrdenados = membros.sortedWith(compareBy<Map<String, String>> { membro ->
                                            when {
                                                membro["nome"] == "Você" -> 0
                                                membro["tipo"] == "Criador" -> 1
                                                membro["tipo"] == "Admin" -> 2
                                                else -> 3
                                            }
                                        }.thenBy { it["nome"] })

                                        callback(Result.success(Triple(membrosOrdenados, criadorId, adminsIds)))
                                    }
                                }
                                .addOnFailureListener { e ->
                                    processedCount++

                                    if (processedCount == membrosIds.size) {
                                        val membrosOrdenados = membros.sortedWith(compareBy<Map<String, String>> { membro ->
                                            when {
                                                membro["nome"] == "Você" -> 0
                                                membro["tipo"] == "Criador" -> 1
                                                membro["tipo"] == "Admin" -> 2
                                                else -> 3
                                            }
                                        }.thenBy { it["nome"] })
                                        callback(Result.success(Triple(membrosOrdenados, criadorId, adminsIds)))
                                    }
                                }
                        }
                    } else {
                        callback(Result.success(Triple(emptyList(), criadorId, adminsIds)))
                    }
                } else {
                    callback(Result.success(Triple(emptyList(), "", emptyList())))
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

    fun atualizarNomeProjeto(
        projetoId: String,
        novoNome: String,
        callback: (Result<Unit>) -> Unit
    ) {
        db.collection("projetos")
            .document(projetoId)
            .update("nome", novoNome)
            .addOnSuccessListener {
                callback(Result.success(Unit))
            }
            .addOnFailureListener { e ->
                callback(Result.failure(e))
            }
    }

    fun atualizarCorProjeto(
        projetoId: String,
        novaCor: String,
        callback: (Result<Unit>) -> Unit
    ) {
        db.collection("projetos")
            .document(projetoId)
            .update("cor", novaCor)
            .addOnSuccessListener {
                callback(Result.success(Unit))
            }
            .addOnFailureListener { e ->
                callback(Result.failure(e))
            }
    }

    // ==================== MÉTODOS DE GERENCIAMENTO DE ADMINS (NOVO) ====================

    /**
     * Promove um membro a administrador (apenas criador pode fazer)
     */
    fun promoverParaAdmin(
        projetoId: String,
        userId: String,
        callback: (Result<Unit>) -> Unit
    ) {
        val currentUserId = auth.currentUser?.uid ?: return

        db.collection("projetos")
            .document(projetoId)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    callback(Result.failure(Exception("Projeto não encontrado")))
                    return@addOnSuccessListener
                }

                val criadorId = document.getString("criador") ?: ""

                // Verificar se quem está promovendo é o criador
                if (criadorId != currentUserId) {
                    callback(Result.failure(Exception("Apenas o criador pode promover administradores")))
                    return@addOnSuccessListener
                }

                // Verificar se o usuário já é admin
                val admins = document.get("admins") as? List<String> ?: emptyList()
                if (admins.contains(userId)) {
                    callback(Result.failure(Exception("Usuário já é administrador")))
                    return@addOnSuccessListener
                }

                // Adicionar à lista de admins
                db.collection("projetos")
                    .document(projetoId)
                    .update("admins", FieldValue.arrayUnion(userId))
                    .addOnSuccessListener {
                        callback(Result.success(Unit))
                    }
                    .addOnFailureListener { e ->
                        callback(Result.failure(e))
                    }
            }
            .addOnFailureListener { e ->
                callback(Result.failure(e))
            }
    }

    /**
     * Remove privilégios de administrador (apenas criador pode fazer)
     */
    fun removerAdmin(
        projetoId: String,
        userId: String,
        callback: (Result<Unit>) -> Unit
    ) {
        val currentUserId = auth.currentUser?.uid ?: return

        db.collection("projetos")
            .document(projetoId)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    callback(Result.failure(Exception("Projeto não encontrado")))
                    return@addOnSuccessListener
                }

                val criadorId = document.getString("criador") ?: ""

                // Verificar se quem está removendo é o criador
                if (criadorId != currentUserId) {
                    callback(Result.failure(Exception("Apenas o criador pode remover administradores")))
                    return@addOnSuccessListener
                }

                // Remover da lista de admins
                db.collection("projetos")
                    .document(projetoId)
                    .update("admins", FieldValue.arrayRemove(userId))
                    .addOnSuccessListener {
                        callback(Result.success(Unit))
                    }
                    .addOnFailureListener { e ->
                        callback(Result.failure(e))
                    }
            }
            .addOnFailureListener { e ->
                callback(Result.failure(e))
            }
    }

    // ✅ NOVO: Classe auxiliar para retornar 4 valores
    data class Quadruple<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )
}

