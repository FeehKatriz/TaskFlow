package com.example.taskflow

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2

class TelaInicial : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tela_inicial)
        viewPager = findViewById(R.id.viewPager)
        val fragments = listOf(
            InicialFragment1(),
            InicialFragment2()
        )

        viewPager.adapter = OnboardingAdapter(this, fragments)

        findViewById<Button>(R.id.btnEntrar).setOnClickListener {
            startActivity(Intent(this, TelaEntrar::class.java))
        }

        findViewById<Button>(R.id.btnCadastrar).setOnClickListener {
            startActivity(Intent(this, TelaCadastro::class.java))
        }
    }
}