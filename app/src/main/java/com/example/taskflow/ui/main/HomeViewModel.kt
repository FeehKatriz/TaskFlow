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

    // FORÇAR TIMEZONE DO BRASIL
    private val timeZoneBrasil = TimeZone.getTimeZone("America/Sao_Paulo")
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault()).apply {
        timeZone = timeZoneBrasil
    }

    private val _tarefasVencidas = MutableLiveData<List<Tarefa>>()
    val tarefasVencidas: LiveData<List<Tarefa>> = _tarefasVencidas

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

                    // Separar tarefas vencidas e urgentes
                    val (vencidas, urgentes) = separarTarefas(todasTarefas)

                    // Ordenar vencidas (mais antiga primeiro - mais atrasada aparece no topo)
                    _tarefasVencidas.value = vencidas.sortedBy { tarefa ->
                        calcularDiasRestantes(tarefa.dataVencimento)
                    }

                    // Ordenar urgentes (mais próxima do vencimento primeiro)
                    _tarefasUrgentes.value = urgentes.sortedBy { tarefa ->
                        calcularDiasRestantes(tarefa.dataVencimento)
                    }
                } else {
                    _tarefasVencidas.value = emptyList()
                    _tarefasUrgentes.value = emptyList()
                }
            }
    }

    private fun separarTarefas(tarefas: List<Tarefa>): Pair<List<Tarefa>, List<Tarefa>> {
        // OBTER HORÁRIO ATUAL NO TIMEZONE DO BRASIL
        val agora = Calendar.getInstance(timeZoneBrasil)

        val vencidas = mutableListOf<Tarefa>()
        val urgentes = mutableListOf<Tarefa>()

        tarefas.forEach { tarefa ->
            // Verificar se tem prazo definido
            if (tarefa.dataVencimento.isNullOrEmpty() ||
                tarefa.dataVencimento == "Sem prazo definido") {
                return@forEach
            }

            try {
                // PARSEAR DATA NO TIMEZONE DO BRASIL
                val dataPrazo = dateFormat.parse(tarefa.dataVencimento)
                if (dataPrazo != null) {
                    val calendarPrazo = Calendar.getInstance(timeZoneBrasil).apply {
                        time = dataPrazo
                    }

                    // Calcular diferença em milissegundos
                    val diffMillis = calendarPrazo.timeInMillis - agora.timeInMillis

                    val diasRestantes = Math.floor(diffMillis / (1000.0 * 60 * 60 * 24)).toInt()

                    when {
                        // Tarefa VENCIDA (qualquer tempo negativo)
                        diffMillis < 0 -> {
                            vencidas.add(tarefa)
                        }

                        // Tarefa URGENTE (0 a 7 dias restantes)
                        diasRestantes in 0..7 -> {
                            urgentes.add(tarefa)
                        }

                        // Tarefas com mais de 7 dias não aparecem aqui
                    }
                }
            } catch (e: Exception) {
                // Ignora tarefas com data inválida
                e.printStackTrace()
            }
        }

        return Pair(vencidas, urgentes)
    }

    private fun calcularDiasRestantes(dataVencimento: String?): Int {
        //  Retorna valor alto para tarefas sem prazo (vai pro final da lista)
        if (dataVencimento.isNullOrEmpty() ||
            dataVencimento == "Sem prazo definido") {
            return Int.MAX_VALUE
        }

        return try {
            // PARSEAR DATA NO TIMEZONE DO BRASIL
            val dataPrazo = dateFormat.parse(dataVencimento)
            if (dataPrazo != null) {
                // OBTER HORÁRIO ATUAL NO TIMEZONE DO BRASIL
                val agora = Calendar.getInstance(timeZoneBrasil)
                val calendarPrazo = Calendar.getInstance(timeZoneBrasil).apply {
                    time = dataPrazo
                }

                val diffMillis = calendarPrazo.timeInMillis - agora.timeInMillis

                Math.floor(diffMillis / (1000.0 * 60 * 60 * 24)).toInt()
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