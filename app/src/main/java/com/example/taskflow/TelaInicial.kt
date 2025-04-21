package com.example.taskflow

import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        //binding = ActivityTelaPricipalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        replaceFragment(HomeFragment()) // mostra um fragmento inicial

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

        replaceFragment(HomeFragment())
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.frame_container, fragment)
            .commit()
    }
}