package com.example.taskflow.data.repository

import android.util.Log
import com.example.taskflow.data.model.Tarefa
import com.example.taskflow.ui.equipe.tarefa.EquipeTarefasOrganizadas
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
}