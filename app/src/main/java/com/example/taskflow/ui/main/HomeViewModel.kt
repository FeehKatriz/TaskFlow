package com.example.taskflow.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.taskflow.data.model.Tarefa
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class HomeViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault())

    private val _tarefasUrgentes = MutableLiveData<List<Tarefa>>()
    val tarefasUrgentes: LiveData<List<Tarefa>> = _tarefasUrgentes

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _erro = MutableLiveData<String?>()
    val erro: LiveData<String?> = _erro

    fun carregarTarefasUrgentes() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _erro.value = "Usuário não autenticado"
            return
        }

        _isLoading.value = true

        // Buscar tarefas onde o usuário é responsável
        firestore.collection("tarefas")
            .whereArrayContains("responsaveis", userId)
            .whereIn("status", listOf("pendente", "em_andamento"))
            .addSnapshotListener { snapshot, error ->
                _isLoading.value = false

                if (error != null) {
                    _erro.value = "Erro ao carregar tarefas: ${error.message}"
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val todasTarefas = snapshot.toObjects(Tarefa::class.java)

                    // Filtrar tarefas urgentes (vencimento em até 7 dias)
                    val tarefasUrgentes = filtrarTarefasUrgentes(todasTarefas)

                    // Ordenar por prazo (mais urgente primeiro)
                    val tarefasOrdenadas = tarefasUrgentes.sortedBy { tarefa ->
                        calcularDiasRestantes(tarefa.dataVencimento)
                    }

                    _tarefasUrgentes.value = tarefasOrdenadas
                } else {
                    _tarefasUrgentes.value = emptyList()
                }
            }
    }

    private fun filtrarTarefasUrgentes(tarefas: List<Tarefa>): List<Tarefa> {
        val hoje = Calendar.getInstance()
        val limiteUrgencia = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 7) // Próximos 7 dias
        }

        return tarefas.filter { tarefa ->
            if (tarefa.dataVencimento.isNullOrEmpty()) {
                false // Ignora tarefas sem prazo
            } else {
                try {
                    val dataPrazo = dateFormat.parse(tarefa.dataVencimento)
                    if (dataPrazo != null) {
                        val calendarPrazo = Calendar.getInstance().apply {
                            time = dataPrazo
                        }
                        // Tarefa está entre hoje e daqui 7 dias
                        calendarPrazo.after(hoje) && calendarPrazo.before(limiteUrgencia) ||
                                calendarPrazo.get(Calendar.DAY_OF_YEAR) <= limiteUrgencia.get(Calendar.DAY_OF_YEAR)
                    } else {
                        false
                    }
                } catch (e: Exception) {
                    false
                }
            }
        }
    }

    private fun calcularDiasRestantes(dataVencimento: String?): Int {
        if (dataVencimento.isNullOrEmpty()) return Int.MAX_VALUE

        return try {
            val dataPrazo = dateFormat.parse(dataVencimento)
            if (dataPrazo != null) {
                val hoje = Calendar.getInstance().time
                val diffMillis = dataPrazo.time - hoje.time
                (diffMillis / (1000 * 60 * 60 * 24)).toInt()
            } else {
                Int.MAX_VALUE
            }
        } catch (e: Exception) {
            Int.MAX_VALUE
        }
    }

    fun limparErro() {
        _erro.value = null
    }
}