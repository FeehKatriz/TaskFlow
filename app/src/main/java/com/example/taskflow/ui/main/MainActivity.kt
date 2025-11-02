package com.example.taskflow.ui.main

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.example.taskflow.R
import com.example.taskflow.databinding.ActivityMainBinding
import com.example.taskflow.services.MyFirebaseMessagingService
import com.example.taskflow.ui.entrar.EntrarActivity
import com.example.taskflow.ui.entrar.EntrarViewModel
import com.example.taskflow.ui.notificacoes.NotificacoesActivity
import com.example.taskflow.ui.perfil.PerfilActivity
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private lateinit var navController: NavController
    private val viewModel: MainViewModel by viewModels()
    private val loginViewModel: EntrarViewModel by viewModels()

    // Launcher para abrir PerfilActivity e receber resultado
    private val perfilLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Foto foi atualizada, recarregar
            viewModel.recarregarFotoPerfil()
        }
    }

    // Launcher para solicitar permissão de notificação (Android 13+)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("MainActivity", "✅ Permissão de notificação concedida")
            obterERegistrarTokenFCM()
        } else {
            Log.d("MainActivity", "❌ Permissão de notificação negada")
            Toast.makeText(
                this,
                "Você não receberá notificações sobre atualizações de tarefas",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        navController = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment)!!
            .findNavController()

        // Liga o BottomNavigationView com o NavController
        binding.bottomNavegation.setupWithNavController(navController)

        // Configurar a toolbar única
        setupToolbars()

        // Observar ViewModel
        observarViewModel()

        // Listener para controlar a toolbar baseado na navegação
        navController.addOnDestinationChangedListener { _, destination, _ ->
            updateToolbarForDestination(destination)
        }

        // Configurar notificações FCM
        solicitarPermissaoNotificacao()

        // Verificar se há token pendente após login
        MyFirebaseMessagingService.verificarESalvarTokenPendente(this)

        // Processar intent se vier de notificação
        processarIntentNotificacao(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        processarIntentNotificacao(intent)
    }

    /**
     * Observa o ViewModel
     */
    private fun observarViewModel() {
        // Observar estado
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is MainState.Loading -> {
                        // Pode mostrar loading se necessário
                    }
                    is MainState.Success -> {
                        // Atualizar UI
                        binding.includeToolbar.tvUsuario.text = "Olá, ${state.nomeUsuario}"

                        // Atualizar foto
                        state.fotoPerfilUrl?.let { url ->
                            Glide.with(this@MainActivity)
                                .load(url)
                                .placeholder(R.drawable.usertype)
                                .circleCrop()
                                .into(binding.includeToolbar.btnPerfil)
                        } ?: run {
                            Glide.with(this@MainActivity)
                                .load(R.drawable.usertype)
                                .circleCrop()
                                .into(binding.includeToolbar.btnPerfil)
                        }
                    }
                    is MainState.Error -> {
                        Toast.makeText(this@MainActivity, state.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Observar notificações não lidas
        lifecycleScope.launch {
            viewModel.notificacoesNaoLidas.collect { count ->
                atualizarBadgeNotificacoes(count)
            }
        }

        // Observar eventos
        lifecycleScope.launch {
            viewModel.events.collect { event ->
                when (event) {
                    is MainEvent.ShowMessage -> {
                        Toast.makeText(this@MainActivity, event.message, Toast.LENGTH_SHORT).show()
                    }
                    is MainEvent.NavigateToTarefa -> {
                        // Navegar para tarefa
                        Log.d("MainActivity", "📋 Navegar para tarefa: ${event.tarefaId}")
                    }
                    is MainEvent.NavigateToEquipe -> {
                        // Navegar para equipe
                        Log.d("MainActivity", "👥 Navegar para equipe: ${event.equipeId}")
                    }
                }
            }
        }
    }

    /**
     * Atualiza o badge de notificações
     */
    private fun atualizarBadgeNotificacoes(count: Int) {
        val badge = binding.includeToolbar.tvBadgeNotificacoes

        if (count > 0) {
            badge.isVisible = true
            badge.text = if (count > 99) "99+" else count.toString()
        } else {
            badge.isVisible = false
        }
    }

    /**
     * Processa intent que veio de uma notificação
     */
    private fun processarIntentNotificacao(intent: Intent?) {
        intent?.let {
            val tipo = it.getStringExtra("NOTIFICATION_TYPE")
            val tarefaId = it.getStringExtra("TAREFA_ID")
            val equipeId = it.getStringExtra("EQUIPE_ID")

            viewModel.processarNotificacao(tipo, tarefaId, equipeId)
        }
    }

    /**
     * Solicita permissão de notificação (Android 13+)
     */
    private fun solicitarPermissaoNotificacao() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d("MainActivity", "✅ Permissão já concedida")
                    obterERegistrarTokenFCM()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    Toast.makeText(
                        this,
                        "Precisamos de permissão para notificar sobre suas tarefas",
                        Toast.LENGTH_LONG
                    ).show()
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            obterERegistrarTokenFCM()
        }
    }

    /**
     * Obtém token FCM e salva no Firestore
     */
    private fun obterERegistrarTokenFCM() {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                Log.d("FCM", "🔑 Token obtido: $token")
                viewModel.salvarTokenFCM(token)
            }
            .addOnFailureListener { e ->
                Log.e("FCM", "❌ Erro ao obter token", e)
            }
    }

    private fun setupToolbars() {
        // Configurar botão voltar
        binding.includeToolbar.btnVoltar.setOnClickListener {
            navController.navigateUp()
        }

        // Configurar botão notificação
        binding.includeToolbar.btnnotificacao.setOnClickListener {
            val intent = Intent(this, NotificacoesActivity::class.java)
            startActivity(intent)
        }

        // Mostrar menu ao clicar na foto do perfil
        binding.includeToolbar.btnPerfil.setOnClickListener { view ->
            mostrarMenuPerfil(view)
        }
    }

    /**
     * Mostra um menu popup com opções de Editar Perfil e Deslogar
     */
    private fun mostrarMenuPerfil(view: View) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.menuInflater.inflate(R.menu.menu_perfil, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_editar_perfil -> {
                    val intent = Intent(this, PerfilActivity::class.java)
                    perfilLauncher.launch(intent)
                    true
                }
                R.id.menu_deslogar -> {
                    deslogarUsuario()
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
    }

    /**
     * Desloga o usuário e volta para tela de login
     */
    private fun deslogarUsuario() {
        lifecycleScope.launch {
            // Remove token antes de deslogar
            viewModel.removerTokenFCM()

            loginViewModel.deslogar()
            Toast.makeText(this@MainActivity, "Você saiu da conta", Toast.LENGTH_SHORT).show()

            val intent = Intent(this@MainActivity, EntrarActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun updateToolbarForDestination(destination: NavDestination) {
        when (destination.id) {
            R.id.fragment_home,
            R.id.projetosFragment,
            R.id.fragment_equipe -> {
                binding.includeToolbar.tvUsuario.visibility = View.VISIBLE
                binding.includeToolbar.btnVoltar.visibility = View.GONE
            }
            else -> {
                binding.includeToolbar.tvUsuario.visibility = View.GONE
                binding.includeToolbar.btnVoltar.visibility = View.VISIBLE
            }
        }
    }
}