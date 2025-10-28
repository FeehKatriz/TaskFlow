package com.example.taskflow.ui.main

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.example.taskflow.R
import com.example.taskflow.databinding.ActivityMainBinding
import com.example.taskflow.ui.perfil.PerfilActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage

class MainActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private lateinit var navController: NavController
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // Launcher para abrir PerfilActivity e receber resultado
    private val perfilLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Foto foi atualizada, recarregar
            atualizarFotoUsuario()
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

        // Configurar as toolbars
        setupToolbars()

        // Carregar dados do usuário
        carregarDadosUsuario()

        // Listener para controlar as toolbars baseado na navegação
        navController.addOnDestinationChangedListener { _, destination, _ ->
            updateToolbarForDestination(destination)
        }

        // Configurar notificações FCM
        solicitarPermissaoNotificacao()

        // Processar intent se vier de notificação
        processarIntentNotificacao(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        processarIntentNotificacao(intent)
    }

    /**
     * Processa intent que veio de uma notificação
     */
    private fun processarIntentNotificacao(intent: Intent?) {
        intent?.let {
            val tipo = it.getStringExtra("NOTIFICATION_TYPE")
            val tarefaId = it.getStringExtra("TAREFA_ID")
            val equipeId = it.getStringExtra("EQUIPE_ID")

            if (tipo != null) {
                Log.d("MainActivity", "🔔 App aberto via notificação: $tipo")

                // Aqui você pode navegar para tela específica baseado no tipo
                when (tipo) {
                    "TAREFA_ATRIBUIDA", "TAREFA_ATUALIZADA" -> {
                        // TODO: Navegar para detalhes da tarefa
                        tarefaId?.let { id ->
                            Log.d("MainActivity", "📋 Abrir tarefa: $id")
                            // navController.navigate(...)
                        }
                    }
                    "CONVITE_EQUIPE" -> {
                        // TODO: Navegar para equipe
                        equipeId?.let { id ->
                            Log.d("MainActivity", "👥 Abrir equipe: $id")
                            // navController.navigate(...)
                        }
                    }
                }
            }
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
                    // Permissão já concedida
                    Log.d("MainActivity", "✅ Permissão já concedida")
                    obterERegistrarTokenFCM()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Mostrar explicação
                    Toast.makeText(
                        this,
                        "Precisamos de permissão para notificar sobre suas tarefas",
                        Toast.LENGTH_LONG
                    ).show()
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    // Solicitar permissão
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // Android < 13, não precisa solicitar
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
                salvarTokenNoFirestore(token)
            }
            .addOnFailureListener { e ->
                Log.e("FCM", "❌ Erro ao obter token", e)
            }
    }

    /**
     * Salva token no documento do usuário
     */
    private fun salvarTokenNoFirestore(token: String) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w("FCM", "⚠️ Usuário não autenticado")
            return
        }

        firestore.collection("usuarios")
            .document(userId)
            .update("fcmToken", token)
            .addOnSuccessListener {
                Log.d("FCM", "✅ Token salvo no Firestore!")
            }
            .addOnFailureListener { e ->
                Log.e("FCM", "❌ Erro ao salvar token: ${e.message}", e)

                // Tentar criar campo se não existir
                firestore.collection("usuarios")
                    .document(userId)
                    .set(
                        mapOf("fcmToken" to token),
                        com.google.firebase.firestore.SetOptions.merge()
                    )
            }
    }

    private fun carregarDadosUsuario() {
        val userId = auth.currentUser?.uid ?: return

        // Buscar nome do usuário
        firestore.collection("usuarios")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                val nome = document.getString("nome") ?: "Usuário"
                binding.includeToolbarInicial.tvUsuario.text = "Olá, $nome"
            }
            .addOnFailureListener {
                binding.includeToolbarInicial.tvUsuario.text = "Olá, Usuário"
            }

        // Carregar foto do usuário nas duas toolbars
        carregarFotoUsuario(userId, binding.includeToolbarInicial.btnPerfil)
        carregarFotoUsuario(userId, binding.includeToolbarVoltar.btnPerfil)
    }

    private fun carregarFotoUsuario(userId: String, imageView: ImageView) {
        val ref = storage.getReference("usuarios/$userId/fotoPerfil.jpg")

        ref.downloadUrl
            .addOnSuccessListener { uri ->
                Glide.with(this)
                    .load(uri)
                    .placeholder(R.drawable.usertype)
                    .circleCrop()
                    .skipMemoryCache(true)
                    .into(imageView)
            }
            .addOnFailureListener {
                // Se não encontrar a foto, usar imagem padrão
                Glide.with(this)
                    .load(R.drawable.usertype)
                    .circleCrop()
                    .into(imageView)
            }
    }

    private fun setupToolbars() {
        // Configurar botão voltar da toolbar com voltar
        binding.includeToolbarVoltar.btnVoltar.setOnClickListener {
            navController.navigateUp()
        }

        // Configurar botões da toolbar inicial
        binding.includeToolbarInicial.btnnotificacao.setOnClickListener {
            // TODO: Implementar ação da notificação
        }

        binding.includeToolbarInicial.btnPerfil.setOnClickListener {
            // Abrir PerfilActivity ao invés de navegar
            val intent = Intent(this, PerfilActivity::class.java)
            perfilLauncher.launch(intent)
        }

        // Configurar botões da toolbar com voltar
        binding.includeToolbarVoltar.btnnotificacao.setOnClickListener {
            // TODO: Implementar ação da notificação
        }

        binding.includeToolbarVoltar.btnPerfil.setOnClickListener {
            // Abrir PerfilActivity ao invés de navegar
            val intent = Intent(this, PerfilActivity::class.java)
            perfilLauncher.launch(intent)
        }
    }

    private fun updateToolbarForDestination(destination: NavDestination) {
        when (destination.id) {
            // Telas principais (do bottom navigation) - SEMPRE mostram toolbar inicial
            R.id.fragment_home,
            R.id.projetosFragment,
            R.id.fragment_equipe -> {
                // Telas principais - mostrar toolbar inicial (sem botão voltar)
                showHomeToolbar()
            }
            else -> {
                // Telas secundárias (navegadas a partir das principais) - mostrar toolbar com botão voltar
                showBackToolbar()
            }
        }
    }

    private fun showHomeToolbar() {
        binding.includeToolbarInicial.root.visibility = View.VISIBLE
        binding.includeToolbarVoltar.root.visibility = View.GONE
    }

    private fun showBackToolbar() {
        binding.includeToolbarInicial.root.visibility = View.GONE
        binding.includeToolbarVoltar.root.visibility = View.VISIBLE
    }

    // Método público para fragments controlarem a toolbar manualmente se necessário
    fun setToolbarMode(showBackButton: Boolean, userName: String? = null) {
        if (showBackButton) {
            showBackToolbar()
        } else {
            showHomeToolbar()
            // Atualizar nome do usuário se fornecido
            userName?.let {
                binding.includeToolbarInicial.tvUsuario.text = "Olá, $it"
            }
        }
    }

    // Método público para atualizar a foto do usuário (caso seja alterada)
    fun atualizarFotoUsuario() {
        val userId = auth.currentUser?.uid ?: return
        carregarFotoUsuario(userId, binding.includeToolbarInicial.btnPerfil)
        carregarFotoUsuario(userId, binding.includeToolbarVoltar.btnPerfil)
    }
}