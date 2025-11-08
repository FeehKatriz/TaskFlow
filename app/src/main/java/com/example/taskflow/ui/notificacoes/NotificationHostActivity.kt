package com.example.taskflow.ui.notificacoes

import android.os.Bundle
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.taskflow.R
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class NotificationHostActivity : AppCompatActivity() {

    private val viewModel: NotificationHostViewModel by viewModels()

    private lateinit var btnVoltar: ImageButton
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_host)

        setupWindowInsets()
        inicializarViews()
        setupListeners()
        observarViewModel()

        if (savedInstanceState == null) {
            viewModel.processarIntent(intent)
        }
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
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupListeners() {
        btnVoltar.setOnClickListener {
            finish()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

    private fun observarViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is NotificationHostState.Loading -> {
                        progressBar.isVisible = true
                    }

                    is NotificationHostState.Success -> {
                        progressBar.isVisible = false
                        carregarFragment(state)
                    }

                    is NotificationHostState.Error -> {
                        progressBar.isVisible = false
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.events.collect { event ->
                when (event) {
                    is NotificationHostEvent.ShowMessage -> {
                        mostrarMensagem(event.message)
                    }

                    is NotificationHostEvent.NavigateBack -> {
                        finish()
                    }
                }
            }
        }
    }

    private fun carregarFragment(state: NotificationHostState.Success) {
        if (supportFragmentManager.findFragmentById(R.id.fragmentContainer) != null) {
            return
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, state.fragment)
            .commit()
    }

    private fun mostrarMensagem(mensagem: String) {
        Snackbar.make(findViewById(R.id.rootLayout), mensagem, Snackbar.LENGTH_SHORT).show()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.processarIntent(intent)
    }
}