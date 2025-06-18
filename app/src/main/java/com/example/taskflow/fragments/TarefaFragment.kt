package com.example.taskflow.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.taskflow.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.firebase.firestore.FirebaseFirestore

class TarefaFragment : Fragment() {

    private var tarefaId: String? = null
    private var tarefaTitulo: String? = null
    private var tarefaDescricao: String? = null
    private var tarefaStatus: String? = null

    private lateinit var textViewTaskName: TextView
    private lateinit var toggleGroup: MaterialButtonToggleGroup
    private lateinit var btnDescricao: MaterialButton
    private lateinit var btnArquivos: MaterialButton
    private lateinit var descricaoContainer: ScrollView
    private lateinit var arquivosContainer: LinearLayout
    private lateinit var textViewDescricao: TextView
    private lateinit var textViewStatusAtual: TextView
    private lateinit var btnStatusPendente: Button
    private lateinit var btnStatusProgresso: Button
    private lateinit var btnStatusConcluida: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { bundle ->
            tarefaId = bundle.getString("tarefaId")
            tarefaTitulo = bundle.getString("tarefaTitulo")
            tarefaDescricao = bundle.getString("tarefaDescricao")
            tarefaStatus = bundle.getString("tarefaStatus")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_tarefa, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        configurarBotaoVoltar(view)
        configurarToggleButtons()
        configurarDadosTarefa()
        configurarBotoesStatus()
    }

    private fun initViews(view: View) {
        textViewTaskName = view.findViewById(R.id.textViewTaskName)
        toggleGroup = view.findViewById(R.id.toggleGroup)
        btnDescricao = view.findViewById(R.id.btnDescricao)
        btnArquivos = view.findViewById(R.id.btnArquivos)
        descricaoContainer = view.findViewById(R.id.descricaoContainer)
        arquivosContainer = view.findViewById(R.id.arquivosContainer)
        textViewDescricao = view.findViewById(R.id.textViewDescricao)
        textViewStatusAtual = view.findViewById(R.id.textViewStatusAtual)
        btnStatusPendente = view.findViewById(R.id.btnStatusPendente)
        btnStatusProgresso = view.findViewById(R.id.btnStatusProgresso)
        btnStatusConcluida = view.findViewById(R.id.btnStatusConcluida)
    }

    private fun configurarBotaoVoltar(view: View) {
        view.findViewById<View>(R.id.btnVoltar).setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun configurarToggleButtons() {
        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnDescricao -> mostrarDescricao()
                    R.id.btnArquivos -> mostrarArquivos()
                }
            }
        }
        mostrarDescricao()
    }

    private fun mostrarDescricao() {
        descricaoContainer.visibility = View.VISIBLE
        arquivosContainer.visibility = View.GONE
    }

    private fun mostrarArquivos() {
        descricaoContainer.visibility = View.GONE
        arquivosContainer.visibility = View.VISIBLE
    }

    private fun configurarDadosTarefa() {
        textViewTaskName.text = tarefaTitulo ?: "Tarefa"
        textViewDescricao.text = if (tarefaDescricao.isNullOrBlank()) {
            "Nenhuma descrição disponível para esta tarefa."
        } else {
            tarefaDescricao
        }
        atualizarStatusDisplay()
    }

    private fun configurarBotoesStatus() {
        btnStatusPendente.setOnClickListener {
            alterarStatus("pendente")
        }

        btnStatusProgresso.setOnClickListener {
            alterarStatus("em_andamento")
        }

        btnStatusConcluida.setOnClickListener {
            alterarStatus("concluida")
        }
    }

    private fun alterarStatus(novoStatus: String) {
        tarefaStatus = novoStatus
        atualizarStatusDisplay()

        val db = FirebaseFirestore.getInstance()
        val id = tarefaId

        if (id != null) {
            db.collection("tarefas")
                .document(id)
                .update("status", novoStatus)
                .addOnSuccessListener {
                    Toast.makeText(context, "Status alterado para: ${traduzirStatusParaUsuario(novoStatus)}", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Erro ao atualizar status no Firebase.", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(context, "ID da tarefa não encontrado.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun atualizarStatusDisplay() {
        val statusAtual = tarefaStatus ?: "não_definido"
        textViewStatusAtual.text = "Status Atual: ${traduzirStatusParaUsuario(statusAtual)}"

        btnStatusPendente.isEnabled = statusAtual != "pendente"
        btnStatusProgresso.isEnabled = statusAtual != "em_andamento"
        btnStatusConcluida.isEnabled = statusAtual != "concluida"
    }

    private fun traduzirStatusParaUsuario(status: String?): String {
        return when (status) {
            "pendente" -> "Pendente"
            "em_andamento" -> "Em andamento"
            "concluida" -> "Concluída"
            else -> "Status desconhecido"
        }
    }
}
