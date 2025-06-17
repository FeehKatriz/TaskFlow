package com.example.taskflow.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.taskflow.databinding.ActivityIntroBinding

class IntroActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityIntroBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Ações dos botões usando somente o binding
        binding.btnEntrar.setOnClickListener {
            startActivity(Intent(this, TelaEntrar::class.java))
        }

        binding.btnCadastrar.setOnClickListener {
            startActivity(Intent(this, TelaCadastro::class.java))
        }

        // Configurações do ViewPager2 e do indicador de pontos
        val adapter = IntroAdapter(this)
        binding.viewPager.adapter = adapter
        binding.dotsIndicator.attachTo(binding.viewPager)

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
            }
        })
    }
}
