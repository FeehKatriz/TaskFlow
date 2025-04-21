package com.example.taskflow

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.taskflow.databinding.ActivityTelaPricipalBinding
import com.example.taskflow.fragments.EquipeFragment
import com.example.taskflow.fragments.TelaInicialFragment

class TelaPrincipal : AppCompatActivity() {

    private lateinit var binding: ActivityTelaPricipalBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityTelaPricipalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        replaceFragment(TelaInicialFragment()) // mostra um fragmento inicial

        binding.bottomNavegation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.bottom_equipe -> {
                    replaceFragment(EquipeFragment())
                    true
                }
                R.id.bottom_perfil -> {
                    replaceFragment(TelaInicialFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.frame_container, fragment)
            .commit()
    }
}
