package com.example.taskflow

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.taskflow.databinding.ActivityTelaInicialBinding
import com.example.taskflow.fragments.EquipesFragment
import com.example.taskflow.fragments.HomeFragment
import com.example.taskflow.fragments.PerfilFragment
import com.example.taskflow.fragments.ProjetosFragment
import com.example.taskflow.fragments.TarefasFragment

class TelaInicial : AppCompatActivity() {

    //private lateinit var binding : ActivityTelaPricipalBinding
    private val binding by lazy {
        ActivityTelaInicialBinding.inflate(layoutInflater)
    }

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        //binding = ActivityTelaPricipalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        /*replaceFragment(HomeFragment()) // mostra um fragmento inicial

        binding.bottomNavegation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.bottom_equipes -> {
                    replaceFragment(EquipesFragment())
                    true
                }
                R.id.bottom_perfil -> {
                    replaceFragment(PerfilFragment())
                    true
                }
                R.id.bottom_tarefas -> {
                    replaceFragment(TarefasFragment())
                    true
                }
                R.id.bottom_home -> {
                    replaceFragment(HomeFragment())
                    true
                }
                R.id.bottom_projetos -> {
                    replaceFragment(ProjetosFragment())
                    true
                }
                else -> false
            }
        }

        replaceFragment(HomeFragment())*/

        navController = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment)!!
            .findNavController()

        // Liga o BottomNavigationView com o NavController
        binding.bottomNavegation.setupWithNavController(navController)

        // Configurar as toolbars
        setupToolbars()

        // Listener para controlar as toolbars baseado na navegação
        navController.addOnDestinationChangedListener { _, destination, _ ->
            updateToolbarForDestination(destination)
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
            //navController.navigate(R.id.perfilFragment)
        }

        // Configurar botões da toolbar com voltar
        binding.includeToolbarVoltar.btnnotificacao.setOnClickListener {
            // TODO: Implementar ação da notificação
        }

        binding.includeToolbarVoltar.btnPerfil.setOnClickListener {
            // Navegar para o perfil usando o ID correto
            //navController.navigate(R.id.perfilFragment)
        }
    }

    private fun updateToolbarForDestination(destination: NavDestination) {
        when (destination.id) {
            // Telas principais (do bottom navigation) - SEMPRE mostram toolbar inicial
            R.id.homeFragment,
            R.id.projetosFragment,
            R.id.equipesFragment -> {
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

    /*private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.frame_container, fragment)
            .commit()
    }*/
}