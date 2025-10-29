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
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
import com.example.taskflow.ui.perfil.PerfilActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
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
    private val viewModel: EntrarViewModel by viewModels()

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

        // Configurar a toolbar única
        setupToolbars()

        // Carregar dados do usuário
        carregarDadosUsuario()

        // Listener para controlar a toolbar baseado na navegação
        navController.addOnDestinationChangedListener { _, destination, _ ->
            updateToolbarForDestination(destination)
        }

        // Configurar notificações FCM
        solicitarPermissaoNotificacao()

        // ✅ Verificar se há token pendente após login
        MyFirebaseMessagingService.verificarESalvarTokenPendente(this)

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

                when (tipo) {
                    "TAREFA_ATRIBUIDA", "TAREFA_ATUALIZADA", "NOVO_COMENTARIO" -> {
                        tarefaId?.let { id ->
                            Log.d("MainActivity", "📋 Abrir tarefa: $id")
                            // navController.navigate(...)
                        }
                    }
                    "CONVITE_EQUIPE" -> {
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
                salvarTokenNoFirestore(token)
            }
            .addOnFailureListener { e ->
                Log.e("FCM", "❌ Erro ao obter token", e)
            }
    }

    /**
     * Salva token no documento do usuário (em array, sem duplicatas)
     */
    private fun salvarTokenNoFirestore(token: String) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w("FCM", "⚠️ Usuário não autenticado")
            return
        }

        firestore.collection("usuarios")
            .document(userId)
            .set(
                mapOf("fcmTokens" to FieldValue.arrayUnion(token)),
                com.google.firebase.firestore.SetOptions.merge()
            )
            .addOnSuccessListener {
                Log.d("FCM", "✅ Token adicionado ao array no Firestore!")
            }
            .addOnFailureListener { e ->
                Log.e("FCM", "❌ Erro ao salvar token: ${e.message}", e)
            }
    }

    /**
     * Remove o token FCM ao deslogar
     */
    private fun removerTokenAoDeslogar() {
        val userId = auth.currentUser?.uid
        if (userId == null) return

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                firestore.collection("usuarios")
                    .document(userId)
                    .update("fcmTokens", FieldValue.arrayRemove(token))
                    .addOnSuccessListener {
                        Log.d("FCM", "✅ Token removido ao deslogar")
                    }
                    .addOnFailureListener { e ->
                        Log.e("FCM", "❌ Erro ao remover token: ${e.message}")
                    }
            }
    }

    private fun carregarDadosUsuario() {
        val userId = auth.currentUser?.uid ?: return

        // Buscar nome do usuário com listener em tempo real
        firestore.collection("usuarios")
            .document(userId)
            .addSnapshotListener { document, error ->
                if (error != null) {
                    binding.includeToolbar.tvUsuario.text = "Olá, Usuário"
                    return@addSnapshotListener
                }

                if (document != null && document.exists()) {
                    val nome = document.getString("nome") ?: "Usuário"
                    binding.includeToolbar.tvUsuario.text = "Olá, $nome"
                } else {
                    binding.includeToolbar.tvUsuario.text = "Olá, Usuário"
                }
            }

        // Carregar foto do usuário na toolbar única
        carregarFotoUsuario(userId, binding.includeToolbar.btnPerfil)
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
                Glide.with(this)
                    .load(R.drawable.usertype)
                    .circleCrop()
                    .into(imageView)
            }
    }

    private fun setupToolbars() {
        // Configurar botão voltar
        binding.includeToolbar.btnVoltar.setOnClickListener {
            navController.navigateUp()
        }

        // Configurar botão notificação
        binding.includeToolbar.btnnotificacao.setOnClickListener {
            // TODO: Implementar ação da notificação
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
        removerTokenAoDeslogar()
        viewModel.deslogar()
        Toast.makeText(this, "Você saiu da conta", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, EntrarActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun updateToolbarForDestination(destination: NavDestination) {
        when (destination.id) {
            // Telas principais - mostrar "Olá, usuário"
            R.id.fragment_home,
            R.id.projetosFragment,
            R.id.fragment_equipe -> {
                binding.includeToolbar.tvUsuario.visibility = View.VISIBLE
                binding.includeToolbar.btnVoltar.visibility = View.GONE
            }
            // Outras telas - mostrar botão voltar
            else -> {
                binding.includeToolbar.tvUsuario.visibility = View.GONE
                binding.includeToolbar.btnVoltar.visibility = View.VISIBLE
            }
        }
    }

    /**
     * Método público para fragments controlarem a toolbar manualmente se necessário
     */
    fun setToolbarMode(showBackButton: Boolean, userName: String? = null) {
        if (showBackButton) {
            binding.includeToolbar.tvUsuario.visibility = View.GONE
            binding.includeToolbar.btnVoltar.visibility = View.VISIBLE
        } else {
            binding.includeToolbar.tvUsuario.visibility = View.VISIBLE
            binding.includeToolbar.btnVoltar.visibility = View.GONE
            userName?.let {
                binding.includeToolbar.tvUsuario.text = "Olá, $it"
            }
        }
    }

    /**
     * Método público para atualizar a foto do usuário (caso seja alterada)
     */
    fun atualizarFotoUsuario() {
        val userId = auth.currentUser?.uid ?: return
        carregarFotoUsuario(userId, binding.includeToolbar.btnPerfil)
    }
}