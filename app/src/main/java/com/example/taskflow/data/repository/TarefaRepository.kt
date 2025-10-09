package com.example.taskflow.data.repository

import android.util.Log
import com.example.taskflow.data.model.Tarefa
import com.example.taskflow.ui.equipe.tarefa.EquipeTarefasOrganizadas
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TarefaRepository {
    private val firestore = FirebaseFirestore.getInstance()

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

    //criar tarefa

    fun carregarProjetosDoUsuario(callback: (Result<Pair<String, String>>) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
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

    fun criarTarefa(
        titulo: String,
        descricao: String,
        projetoId: String,
        equipeId: String,
        callback: (Result<Unit>) -> Unit
    ) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
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
}