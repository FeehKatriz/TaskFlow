package com.example.taskflow.data.repository

import android.util.Log
import com.example.taskflow.data.model.Tarefa
import com.example.taskflow.ui.equipe.tarefa.EquipeTarefasOrganizadas
import com.example.taskflow.ui.tarefa.Arquivo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp

class TarefaRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // ==================== MÉTODOS EXISTENTES ====================

    // Buscar projeto da equipe
    fun buscarProjetoDaEquipe(equipeId: String, callback: (Result<String>) -> Unit) {
        firestore.collection("equipes")
            .document(equipeId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val projetoId = document.getString("projetoId") ?: ""
                    callback(Result.success(projetoId))
                } else {
                    callback(Result.failure(Exception("Equipe não encontrada")))
                }
            }
            .addOnFailureListener { e ->
                Log.e("TarefaRepository", "Erro ao buscar projeto da equipe", e)
                callback(Result.failure(e))
            }
    }

    // Carregador de tarefas com listener em tempo real
    fun carregarTarefasComListener(
        equipeId: String,
        callback: (Result<EquipeTarefasOrganizadas>) -> Unit
    ) {
        firestore.collection("tarefas")
            .whereEqualTo("equipeId", equipeId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("TarefaRepository", "Erro ao carregar tarefas", error)
                    callback(Result.failure(error))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val todasTarefas = snapshot.toObjects(Tarefa::class.java)

                    // Organizar tarefas por status
                    val tarefasPendentes = todasTarefas.filter { it.status == "pendente" }
                    val tarefasAndamento = todasTarefas.filter { it.status == "em_andamento" }
                    val tarefasConcluidas = todasTarefas.filter { it.status == "concluida" }

                    val tarefasOrganizadas = EquipeTarefasOrganizadas(
                        tarefasPendentes = tarefasPendentes,
                        tarefasAndamento = tarefasAndamento,
                        tarefasConcluidas = tarefasConcluidas
                    )

                    Log.d("TarefaRepository", "Tarefas carregadas - Pendentes: ${tarefasPendentes.size}, Em andamento: ${tarefasAndamento.size}, Concluídas: ${tarefasConcluidas.size}")
                    callback(Result.success(tarefasOrganizadas))
                }
            }
    }

    // ==================== CRIAR TAREFA ====================

    fun carregarProjetosDoUsuario(callback: (Result<Pair<String, String>>) -> Unit) {
        val userId = auth.currentUser?.uid ?: run {
            callback(Result.failure(Exception("Usuário não autenticado")))
            return
        }

        firestore.collection("equipes")
            .whereArrayContains("membros", userId)
            .get()
            .addOnSuccessListener { equipesSnapshot ->
                if (!equipesSnapshot.isEmpty) {
                    val equipeIds = equipesSnapshot.documents.map { it.id }

                    firestore.collection("projetos")
                        .whereIn("equipeId", equipeIds)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { projetosSnapshot ->
                            if (!projetosSnapshot.isEmpty) {
                                val projeto = projetosSnapshot.documents[0]
                                val projetoId = projeto.id
                                val equipeId = projeto.getString("equipeId") ?: ""

                                if (equipeId.isNotEmpty()) {
                                    callback(Result.success(Pair(projetoId, equipeId)))
                                } else {
                                    callback(Result.failure(Exception("Erro: Projeto sem equipe associada")))
                                }
                            } else {
                                callback(Result.failure(Exception("Nenhum projeto encontrado. Crie um projeto primeiro.")))
                            }
                        }
                        .addOnFailureListener { e ->
                            callback(Result.failure(e))
                        }
                } else {
                    callback(Result.failure(Exception("Você precisa fazer parte de uma equipe primeiro")))
                }
            }
            .addOnFailureListener { e ->
                callback(Result.failure(e))
            }
    }

    fun buscarEquipeDoProjeto(projetoId: String, callback: (Result<String>) -> Unit) {
        firestore.collection("projetos")
            .document(projetoId)
            .get()
            .addOnSuccessListener { documento ->
                if (documento.exists()) {
                    val equipeId = documento.getString("equipeId") ?: ""
                    if (equipeId.isNotEmpty()) {
                        callback(Result.success(equipeId))
                    } else {
                        callback(Result.failure(Exception("Projeto sem equipe associada")))
                    }
                } else {
                    callback(Result.failure(Exception("Projeto não encontrado")))
                }
            }
            .addOnFailureListener { e ->
                callback(Result.failure(e))
            }
    }

    /**
     * Carrega os membros da equipe para seleção de responsáveis
     */
    fun carregarMembrosEquipe(
        equipeId: String,
        callback: (Result<List<Pair<String, String>>>) -> Unit
    ) {
        firestore.collection("equipes")
            .document(equipeId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val membrosIds = document.get("membros") as? List<String> ?: emptyList()

                    if (membrosIds.isEmpty()) {
                        callback(Result.success(emptyList()))
                        return@addOnSuccessListener
                    }

                    // Buscar informações dos membros
                    val membros = mutableListOf<Pair<String, String>>()
                    var processados = 0

                    membrosIds.forEach { memberId ->
                        firestore.collection("usuarios")
                            .document(memberId)
                            .get()
                            .addOnSuccessListener { userDoc ->
                                if (userDoc.exists()) {
                                    val nome = userDoc.getString("nome") ?: "Sem nome"
                                    membros.add(Pair(memberId, nome))
                                }
                                processados++

                                if (processados == membrosIds.size) {
                                    callback(Result.success(membros))
                                }
                            }
                            .addOnFailureListener { e ->
                                processados++
                                if (processados == membrosIds.size) {
                                    callback(Result.success(membros))
                                }
                            }
                    }
                } else {
                    callback(Result.failure(Exception("Equipe não encontrada")))
                }
            }
            .addOnFailureListener { e ->
                callback(Result.failure(e))
            }
    }

    /**
     * Cria uma tarefa completa com todos os campos
     * ✅ ATUALIZADO PARA NOTIFICAÇÕES
     */
    fun criarTarefaCompleta(
        titulo: String,
        descricao: String,
        projetoId: String,
        equipeId: String,
        prioridade: String,
        dataVencimento: String?,
        responsaveis: List<String>,
        callback: (Result<String>) -> Unit
    ) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            callback(Result.failure(Exception("Usuário não autenticado")))
            return
        }

        // Buscar o nome da equipe
        firestore.collection("equipes")
            .document(equipeId)
            .get()
            .addOnSuccessListener { equipeDoc ->
                val equipeNome = equipeDoc.getString("nome") ?: "Equipe Desconhecida"

                // Criar a tarefa
                val tarefaRef = firestore.collection("tarefas").document()
                val tarefaId = tarefaRef.id

                val tarefaData = hashMapOf(
                    "id" to tarefaId,
                    "titulo" to titulo,
                    "descricao" to descricao,
                    "status" to "pendente",
                    "projetoId" to projetoId,
                    "equipeId" to equipeId,
                    "equipeNome" to equipeNome,
                    "prioridade" to prioridade,
                    "dataVencimento" to (dataVencimento ?: ""),
                    "responsaveis" to responsaveis,
                    "criadoPor" to userId,
                    "atualizadoPor" to userId, // ✅ ADICIONADO para notificações
                    "dataCriacao" to System.currentTimeMillis(),
                    "atualizadoEm" to Timestamp.now(),
                    "anexos" to emptyList<String>()
                )

                tarefaRef.set(tarefaData)
                    .addOnSuccessListener {
                        atualizarContadorTarefasEquipe(equipeId)
                        Log.d("TarefaRepository", "✅ Tarefa criada - ID: $tarefaId, Criador: $userId, Responsáveis: $responsaveis")
                        callback(Result.success(tarefaId))
                    }
                    .addOnFailureListener { e ->
                        Log.e("TarefaRepository", "❌ Erro ao criar tarefa", e)
                        callback(Result.failure(e))
                    }
            }
            .addOnFailureListener { e ->
                callback(Result.failure(e))
            }
    }

    /**
     * Método original mantido para compatibilidade
     * ✅ ATUALIZADO PARA NOTIFICAÇÕES
     */
    fun criarTarefa(
        titulo: String,
        descricao: String,
        projetoId: String,
        equipeId: String,
        callback: (Result<Unit>) -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: run {
            callback(Result.failure(Exception("Usuário não autenticado")))
            return
        }

        // Buscar nome da equipe
        firestore.collection("equipes")
            .document(equipeId)
            .get()
            .addOnSuccessListener { equipeDoc ->
                val equipeNome = equipeDoc.getString("nome") ?: "Equipe Desconhecida"

                val tarefaData = hashMapOf(
                    "titulo" to titulo,
                    "descricao" to descricao,
                    "projetoId" to projetoId,
                    "equipeId" to equipeId,
                    "equipeNome" to equipeNome,
                    "criadoPor" to userId,
                    "atualizadoPor" to userId, // ✅ ADICIONADO
                    "status" to "pendente",
                    "prioridade" to "media",
                    "dataVencimento" to "",
                    "dataCriacao" to System.currentTimeMillis(),
                    "atualizadoEm" to Timestamp.now(),
                    "anexos" to emptyList<String>(),
                    "responsaveis" to emptyList<String>()
                )

                firestore.collection("tarefas")
                    .add(tarefaData)
                    .addOnSuccessListener { documentReference ->
                        documentReference.update("id", documentReference.id)
                            .addOnSuccessListener {
                                atualizarContadorTarefasEquipe(equipeId)
                                callback(Result.success(Unit))
                            }
                            .addOnFailureListener { e ->
                                callback(Result.success(Unit))
                            }
                    }
                    .addOnFailureListener { e ->
                        callback(Result.failure(e))
                    }
            }
            .addOnFailureListener { e ->
                callback(Result.failure(e))
            }
    }

    private fun atualizarContadorTarefasEquipe(equipeId: String) {
        firestore.collection("tarefas")
            .whereEqualTo("equipeId", equipeId)
            .get()
            .addOnSuccessListener { snapshot ->
                val totalTarefas = snapshot.size()
                firestore.collection("equipes")
                    .document(equipeId)
                    .update("totalTarefas", totalTarefas)
                    .addOnSuccessListener {
                        Log.d("TarefaRepository", "Contador de tarefas da equipe $equipeId atualizado: $totalTarefas")
                    }
                    .addOnFailureListener { e ->
                        Log.e("TarefaRepository", "Erro ao atualizar contador da equipe", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("TarefaRepository", "Erro ao buscar tarefas da equipe", e)
            }
    }

    // ==================== BUSCAR TAREFA POR ID ====================

    /**
     * Busca uma tarefa específica pelo ID no Firestore
     */
    fun buscarTarefaPorId(tarefaId: String, callback: (Result<Tarefa>) -> Unit) {
        firestore.collection("tarefas")
            .document(tarefaId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val tarefa = document.toObject(Tarefa::class.java)
                    if (tarefa != null) {
                        callback(Result.success(tarefa))
                    } else {
                        callback(Result.failure(Exception("Erro ao converter dados da tarefa")))
                    }
                } else {
                    callback(Result.failure(Exception("Tarefa não encontrada")))
                }
            }
            .addOnFailureListener { e ->
                Log.e("TarefaRepository", "Erro ao buscar tarefa por ID", e)
                callback(Result.failure(e))
            }
    }

    /**
     * Busca nomes de usuários pelos IDs (para exibir responsáveis)
     */
    fun buscarNomesUsuarios(userIds: List<String>, callback: (Result<List<String>>) -> Unit) {
        if (userIds.isEmpty()) {
            callback(Result.success(emptyList()))
            return
        }

        val nomes = mutableListOf<String>()
        var contadorProcessados = 0

        userIds.forEach { userId ->
            firestore.collection("usuarios")
                .document(userId)
                .get()
                .addOnSuccessListener { document ->
                    val nome = document.getString("nome") ?: "Usuário"
                    nomes.add(nome)
                    contadorProcessados++

                    if (contadorProcessados == userIds.size) {
                        callback(Result.success(nomes))
                    }
                }
                .addOnFailureListener { e ->
                    contadorProcessados++
                    nomes.add("Usuário")

                    if (contadorProcessados == userIds.size) {
                        callback(Result.success(nomes))
                    }
                }
        }
    }

    /**
     * Busca o nome de uma equipe pelo ID
     */
    fun buscarNomeEquipe(equipeId: String, callback: (Result<String>) -> Unit) {
        firestore.collection("equipes")
            .document(equipeId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val nome = document.getString("nome") ?: "Equipe Desconhecida"
                    callback(Result.success(nome))
                } else {
                    callback(Result.failure(Exception("Equipe não encontrada")))
                }
            }
            .addOnFailureListener { e ->
                Log.e("TarefaRepository", "Erro ao buscar nome da equipe", e)
                callback(Result.failure(e))
            }
    }

    // ==================== GERENCIAR TAREFA ====================

    /**
     * Altera o status da tarefa
     * ✅ OTIMIZADO PARA NOTIFICAÇÕES
     */
    fun alterarStatus(tarefaId: String, novoStatus: String, callback: (Result<Unit>) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            callback(Result.failure(Exception("Usuário não autenticado")))
            return
        }

        // Salvar quem fez a alteração e quando
        val updates = hashMapOf<String, Any>(
            "status" to novoStatus,
            "atualizadoPor" to userId, // ✅ Cloud Function usa isso
            "atualizadoEm" to Timestamp.now()
        )

        firestore.collection("tarefas")
            .document(tarefaId)
            .update(updates)
            .addOnSuccessListener {
                Log.d("TarefaRepository", "✅ Status atualizado - Tarefa: $tarefaId, Novo status: $novoStatus, Por: $userId")
                callback(Result.success(Unit))
            }
            .addOnFailureListener { e ->
                Log.e("TarefaRepository", "❌ Erro ao atualizar status", e)
                callback(Result.failure(e))
            }
    }

    // ==================== ARQUIVOS ====================

    fun carregarArquivos(
        tarefaId: String,
        callback: (Result<List<Arquivo>>) -> Unit
    ) {
        val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference
        val tarefaStorageRef = storageRef.child("tarefas/$tarefaId")

        tarefaStorageRef.listAll()
            .addOnSuccessListener { listResult ->
                if (listResult.items.isEmpty()) {
                    callback(Result.success(emptyList()))
                } else {
                    val arquivos = mutableListOf<Arquivo>()
                    var processedCount = 0

                    for (itemRef in listResult.items) {
                        itemRef.metadata
                            .addOnSuccessListener { metadata ->
                                processedCount++
                                val mimeType = metadata.contentType ?: ""
                                val arquivo = Arquivo(
                                    nome = itemRef.name,
                                    mimeType = mimeType,
                                    ref = itemRef
                                )
                                arquivos.add(arquivo)

                                if (processedCount == listResult.items.size) {
                                    callback(Result.success(arquivos))
                                }
                            }
                            .addOnFailureListener { e ->
                                processedCount++
                                val arquivo = Arquivo(
                                    nome = itemRef.name,
                                    mimeType = "",
                                    ref = itemRef
                                )
                                arquivos.add(arquivo)

                                if (processedCount == listResult.items.size) {
                                    callback(Result.success(arquivos))
                                }
                            }
                    }
                }
            }
            .addOnFailureListener { e ->
                callback(Result.failure(e))
            }
    }

    fun uploadArquivo(
        tarefaId: String,
        fileUri: android.net.Uri,
        callback: (Result<Unit>) -> Unit
    ) {
        val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference
        val fileName = System.currentTimeMillis().toString() + "_" + (fileUri.lastPathSegment ?: "arquivo")
        val fileRef = storageRef.child("tarefas/$tarefaId/$fileName")

        fileRef.putFile(fileUri)
            .addOnSuccessListener {
                callback(Result.success(Unit))
            }
            .addOnFailureListener { e ->
                callback(Result.failure(e))
            }
    }

    fun obterDownloadUrlArquivo(
        fileRef: com.google.firebase.storage.StorageReference,
        callback: (Result<android.net.Uri>) -> Unit
    ) {
        fileRef.downloadUrl
            .addOnSuccessListener { uri ->
                callback(Result.success(uri))
            }
            .addOnFailureListener { e ->
                callback(Result.failure(e))
            }
    }

    // ==================== COMENTÁRIOS ====================

    /**
     * Carrega comentários de uma tarefa em tempo real
     */
    fun carregarComentarios(
        tarefaId: String,
        callback: (Result<List<com.example.taskflow.data.model.Comment>>) -> Unit
    ) {
        firestore.collection("tarefas")
            .document(tarefaId)
            .collection("comentarios")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("TarefaRepository", "Erro ao carregar comentários", error)
                    callback(Result.failure(error))
                    return@addSnapshotListener
                }

                val comentarios = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val comment = doc.toObject(com.example.taskflow.data.model.Comment::class.java)
                        comment?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e("TarefaRepository", "Erro ao converter comentário", e)
                        null
                    }
                } ?: emptyList()

                Log.d("TarefaRepository", "Comentários carregados: ${comentarios.size}")
                callback(Result.success(comentarios))
            }
    }

    /**
     * Adiciona um novo comentário
     * ✅ FUNCIONAL PARA NOTIFICAÇÕES
     */
    fun adicionarComentario(
        tarefaId: String,
        mensagem: String,
        callback: (Result<Unit>) -> Unit
    ) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            callback(Result.failure(Exception("Usuário não autenticado")))
            return
        }

        // Buscar nome do usuário no Firestore
        firestore.collection("usuarios")
            .document(userId)
            .get()
            .addOnSuccessListener { userDoc ->
                val userName = userDoc.getString("nome") ?: "Usuário"
                var userPhotoUrl = ""

                // Buscar URL da foto no Storage
                val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance()
                    .getReference("usuarios/$userId/fotoPerfil.jpg")

                storageRef.downloadUrl
                    .addOnSuccessListener { uri ->
                        userPhotoUrl = uri.toString()
                    }
                    .addOnFailureListener {
                        userPhotoUrl = ""
                    }
                    .addOnCompleteListener {
                        // Salvar comentário
                        val comment = hashMapOf(
                            "userId" to userId, // ✅ Cloud Function usa isso
                            "userName" to userName,
                            "userPhotoUrl" to userPhotoUrl,
                            "message" to mensagem.trim(), // ✅ Cloud Function usa isso
                            "timestamp" to System.currentTimeMillis()
                        )

                        firestore.collection("tarefas")
                            .document(tarefaId)
                            .collection("comentarios")
                            .add(comment)
                            .addOnSuccessListener {
                                Log.d("TarefaRepository", "✅ Comentário adicionado - Tarefa: $tarefaId, Autor: $userId")
                                callback(Result.success(Unit))
                            }
                            .addOnFailureListener { e ->
                                Log.e("TarefaRepository", "❌ Erro ao adicionar comentário", e)
                                callback(Result.failure(e))
                            }
                    }
            }
            .addOnFailureListener { e ->
                Log.e("TarefaRepository", "Erro ao buscar dados do usuário", e)
                callback(Result.failure(e))
            }
    }

    /**
     * Deleta um comentário
     */
    fun deletarComentario(
        tarefaId: String,
        commentId: String,
        callback: (Result<Unit>) -> Unit
    ) {
        firestore.collection("tarefas")
            .document(tarefaId)
            .collection("comentarios")
            .document(commentId)
            .delete()
            .addOnSuccessListener {
                Log.d("TarefaRepository", "Comentário deletado com sucesso")
                callback(Result.success(Unit))
            }
            .addOnFailureListener { e ->
                Log.e("TarefaRepository", "Erro ao deletar comentário", e)
                callback(Result.failure(e))
            }
    }

    fun verificarSeUsuarioEstaEquipe(
        equipeId: String,
        callback: (Result<Boolean>) -> Unit
    ) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            callback(Result.failure(Exception("Usuário não autenticado")))
            return
        }

        firestore.collection("equipes")
            .document(equipeId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val membros = document.get("membros") as? List<String> ?: emptyList()
                    val isMembro = membros.contains(userId)

                    Log.d("TarefaRepository", "Verificação de membro - UserId: $userId, EquipeId: $equipeId, É membro: $isMembro")
                    callback(Result.success(isMembro))
                } else {
                    callback(Result.failure(Exception("Equipe não encontrada")))
                }
            }
            .addOnFailureListener { e ->
                Log.e("TarefaRepository", "Erro ao verificar membro da equipe", e)
                callback(Result.failure(e))
            }
    }
}