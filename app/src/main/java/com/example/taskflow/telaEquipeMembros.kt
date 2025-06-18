package com.example.taskflow

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taskflow.databinding.ActivityTelaEquipeMembrosBinding
import com.example.taskflow.adapters.MembroAdapter


class telaEquipeMembros : AppCompatActivity() {
    private val binding by lazy {
        ActivityTelaEquipeMembrosBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.teste)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.rvMembros.layoutManager = LinearLayoutManager(this)
        binding.rvMembros.adapter = MembroAdapter()
    }
}