package com.example.taskflow.data.repository

import android.util.Log
import com.example.taskflow.data.model.Tarefa
import com.example.taskflow.ui.equipe.tarefa.EquipeTarefasOrganizadas
import com.example.taskflow.ui.tarefa.Arquivo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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
     * Cria uma tarefa completa com todos os campos (NOVO)
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
                    "criadoEm" to com.google.firebase.Timestamp.now(),
                    "atualizadoEm" to com.google.firebase.Timestamp.now(),
                    "anexos" to emptyList<String>()
                )

                tarefaRef.set(tarefaData)
                    .addOnSuccessListener {
                        atualizarContadorTarefasProjeto(projetoId)
                        callback(Result.success(tarefaId))
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
     * Método original mantido para compatibilidade
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

        val tarefa = Tarefa(
            titulo = titulo,
            descricao = descricao,
            projetoId = projetoId,
            equipeId = equipeId,
            criadoPor = userId,
            anexos = emptyList()
        )

        firestore.collection("tarefas")
            .add(tarefa)
            .addOnSuccessListener { documentReference ->
                documentReference.update("id", documentReference.id)
                    .addOnSuccessListener {
                        atualizarContadorTarefasProjeto(projetoId)
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

    private fun atualizarContadorTarefasProjeto(projetoId: String) {
        firestore.collection("tarefas")
            .whereEqualTo("projetoId", projetoId)
            .get()
            .addOnSuccessListener { snapshot ->
                val totalTarefas = snapshot.size()
                firestore.collection("projetos")
                    .document(projetoId)
                    .update("totalTarefas", totalTarefas)
            }
    }

    // ==================== BUSCAR TAREFA POR ID (NOVO) ====================

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

    fun alterarStatus(tarefaId: String, novoStatus: String, callback: (Result<Unit>) -> Unit) {
        firestore.collection("tarefas")
            .document(tarefaId)
            .update("status", novoStatus)
            .addOnSuccessListener {
                callback(Result.success(Unit))
            }
            .addOnFailureListener { e ->
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
}