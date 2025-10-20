package com.example.taskflow.ui.main

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.example.taskflow.R
import com.example.taskflow.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class MainActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private lateinit var navController: NavController
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

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
            // Navegar para o perfil usando o ID correto
            navController.navigate(R.id.perfilFragment)
        }

        // Configurar botões da toolbar com voltar
        binding.includeToolbarVoltar.btnnotificacao.setOnClickListener {
            // TODO: Implementar ação da notificação
        }

        binding.includeToolbarVoltar.btnPerfil.setOnClickListener {
            // Navegar para o perfil usando o ID correto
            navController.navigate(R.id.perfilFragment)
        }
    }

    private fun updateToolbarForDestination(destination: NavDestination) {
        when (destination.id) {
            // Telas principais (do bottom navigation) - SEMPRE mostram toolbar inicial
            R.id.navHome,
            R.id.projetosFragment,
            R.id.navEquipe -> {
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