package com.example.taskflow.ui.notificacoes

import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.taskflow.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class NotificacoesActivity : AppCompatActivity() {

    private val viewModel: NotificacoesViewModel by viewModels()

    private lateinit var btnVoltar: ImageButton
    private lateinit var btnMarcarTodasLidas: ImageButton
    private lateinit var btnDeletarTodas: ImageButton
    private lateinit var layoutContador: LinearLayout
    private lateinit var tvContadorNaoLidas: TextView
    private lateinit var scrollView: NestedScrollView
    private lateinit var tvHeaderNaoLidas: TextView
    private lateinit var tvHeaderLidas: TextView
    private lateinit var rvNotificacoesNaoLidas: RecyclerView
    private lateinit var rvNotificacoesLidas: RecyclerView
    private lateinit var layoutEstadoVazio: LinearLayout
    private lateinit var progressBar: ProgressBar

    private val adapterNaoLidas by lazy {
        NotificacaoAdapter(
            onItemClick = { notificacao ->
                viewModel.onNotificacaoClick(notificacao)
            },
            onDeleteClick = { notificacao ->
                mostrarDialogConfirmacaoDeletar(notificacao)
            }
        )
    }

    private val adapterLidas by lazy {
        NotificacaoAdapter(
            onItemClick = { notificacao ->
                viewModel.onNotificacaoClick(notificacao)
            },
            onDeleteClick = { notificacao ->
                mostrarDialogConfirmacaoDeletar(notificacao)
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notificacoes)

        setupWindowInsets()
        inicializarViews()
        setupRecyclerViews()
        setupListeners()
        observarViewModel()
    }

    override fun onResume() {
        super.onResume()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun inicializarViews() {
        btnVoltar = findViewById(R.id.btnVoltar)
        btnMarcarTodasLidas = findViewById(R.id.btnMarcarTodasLidas)
        btnDeletarTodas = findViewById(R.id.btnDeletarTodas)
        layoutContador = findViewById(R.id.layoutContador)
        tvContadorNaoLidas = findViewById(R.id.tvContadorNaoLidas)
        scrollView = findViewById(R.id.scrollView)
        tvHeaderNaoLidas = findViewById(R.id.tvHeaderNaoLidas)
        tvHeaderLidas = findViewById(R.id.tvHeaderLidas)
        rvNotificacoesNaoLidas = findViewById(R.id.rvNotificacoesNaoLidas)
        rvNotificacoesLidas = findViewById(R.id.rvNotificacoesLidas)
        layoutEstadoVazio = findViewById(R.id.layoutEstadoVazio)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupRecyclerViews() {
        rvNotificacoesNaoLidas.layoutManager = LinearLayoutManager(this)
        rvNotificacoesNaoLidas.adapter = adapterNaoLidas

        rvNotificacoesLidas.layoutManager = LinearLayoutManager(this)
        rvNotificacoesLidas.adapter = adapterLidas
    }

    private fun setupListeners() {
        btnVoltar.setOnClickListener {
            finish()
        }

        btnMarcarTodasLidas.setOnClickListener {
            mostrarDialogMarcarTodasLidas()
        }

        btnDeletarTodas.setOnClickListener {
            mostrarDialogDeletarTodas()
        }
    }

    private fun observarViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is NotificacoesState.Loading -> {
                        progressBar.isVisible = true
                        scrollView.isVisible = false
                        layoutEstadoVazio.isVisible = false
                        layoutContador.isVisible = false
                    }

                    is NotificacoesState.Success -> {
                        progressBar.isVisible = false
                        scrollView.isVisible = true
                        layoutEstadoVazio.isVisible = false

                        // Atualizar RecyclerViews
                        adapterNaoLidas.submitList(state.notificacoesNaoLidas)
                        adapterLidas.submitList(state.notificacoesLidas)

                        // Mostrar/ocultar headers
                        tvHeaderNaoLidas.isVisible = state.notificacoesNaoLidas.isNotEmpty()
                        tvHeaderLidas.isVisible = state.notificacoesLidas.isNotEmpty()

                        // Atualizar contador
                        if (state.totalNaoLidas > 0) {
                            layoutContador.isVisible = true
                            val texto = if (state.totalNaoLidas == 1) {
                                "1 notificação não lida"
                            } else {
                                "${state.totalNaoLidas} notificações não lidas"
                            }
                            tvContadorNaoLidas.text = texto
                        } else {
                            layoutContador.isVisible = false
                        }
                    }

                    is NotificacoesState.Empty -> {
                        progressBar.isVisible = false
                        scrollView.isVisible = false
                        layoutEstadoVazio.isVisible = true
                        layoutContador.isVisible = false
                    }

                    is NotificacoesState.Error -> {
                        progressBar.isVisible = false
                        mostrarMensagem(state.message)
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.events.collect { event ->
                when (event) {
                    is NotificacoesEvent.ShowMessage -> {
                        mostrarMensagem(event.message)
                    }

                    is NotificacoesEvent.NavigateToTarefa -> {
                        navegarParaTarefa(event.tarefaId)
                    }

                    is NotificacoesEvent.NavigateToTarefaWithComentario -> {
                        navegarParaTarefaComComentario(
                            event.tarefaId,
                            event.comentarioId,
                            event.scrollToComentario
                        )
                    }

                    is NotificacoesEvent.NavigateToEquipe -> {
                        navegarParaEquipe(event.equipeId)
                    }

                    is NotificacoesEvent.NavigateToProjeto -> {
                        navegarParaProjeto(event.projetoId)
                    }
                }
            }
        }
    }

    private fun mostrarDialogMarcarTodasLidas() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Marcar todas como lidas")
            .setMessage("Deseja marcar todas as notificações como lidas?")
            .setPositiveButton("Sim") { _, _ ->
                viewModel.marcarTodasComoLidas()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogDeletarTodas() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Deletar todas")
            .setMessage("Deseja remover TODAS as notificações? Esta ação não pode ser desfeita.")
            .setPositiveButton("Deletar") { _, _ ->
                viewModel.deletarTodasNotificacoes()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogConfirmacaoDeletar(notificacao: com.example.taskflow.data.model.Notificacao) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Deletar notificação")
            .setMessage("Deseja remover esta notificação?")
            .setPositiveButton("Deletar") { _, _ ->
                viewModel.deletarNotificacao(notificacao)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarMensagem(mensagem: String) {
        Snackbar.make(findViewById(R.id.rootLayout), mensagem, Snackbar.LENGTH_SHORT).show()
    }

    private fun navegarParaTarefa(tarefaId: String) {
        val intent = NotificationHostViewModel.createTarefaIntent(
            context = this,
            tarefaId = tarefaId
        )
        startActivity(intent)
        // Removido finish() - agora volta para NotificacoesActivity
    }

    private fun navegarParaTarefaComComentario(
        tarefaId: String,
        comentarioId: String?,
        scrollToComentario: Boolean
    ) {
        val intent = NotificationHostViewModel.createTarefaIntent(
            context = this,
            tarefaId = tarefaId,
            comentarioId = comentarioId,
            scrollToComentario = scrollToComentario
        )
        startActivity(intent)
    }


    private fun navegarParaEquipe(equipeId: String) {
        val intent = NotificationHostViewModel.createEquipeIntent(
            context = this,
            equipeId = equipeId
        )
        startActivity(intent)
    }


    private fun navegarParaProjeto(projetoId: String) {
        val intent = NotificationHostViewModel.createProjetoIntent(
            context = this,
            projetoId = projetoId
        )
        startActivity(intent)
    }
}