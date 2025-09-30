package com.example.taskflow.ui.cadastrar

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.taskflow.R
import com.example.taskflow.databinding.ActivityCadastrarBinding
import com.example.taskflow.ui.intro.IntroActivity

class CadastrarActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityCadastrarBinding.inflate(layoutInflater)
    }

    private val viewModel: CadastrarViewModel by viewModels()

    private var imageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            imageUri = it
            Glide.with(this)
                .load(it)
                .placeholder(R.drawable.usertype)
                .circleCrop()
                .into(binding.imageView)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.teste) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.imageView.setOnClickListener {
            abrirGaleria()
        }

        binding.btnCadastrar.setOnClickListener {
            val nome = binding.editNome.text.toString().trim()
            val email = binding.editEmail.text.toString().trim()
            val nickname = binding.editNick.text.toString().trim()
            val senha = binding.editSenha.text.toString().trim()
            val confirmaSenha = binding.editConfirmarSenha.text.toString().trim()

            viewModel.cadastrarUsuario(nome, email, nickname, senha, confirmaSenha, imageUri)
        }

        observarEstado()
    }

    private fun abrirGaleria() {
        pickImageLauncher.launch("image/*")
    }

    private fun observarEstado() {
        viewModel.state.observe(this) { state ->
            when (state) {
                is CadastrarState.Loading -> binding.btnCadastrar.isEnabled = false
                is CadastrarState.Success -> {
                    binding.btnCadastrar.isEnabled = true
                    Toast.makeText(this, "Cadastro realizado com sucesso!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, IntroActivity::class.java))
                    finish()
                }
                is CadastrarState.Error -> {
                    binding.btnCadastrar.isEnabled = true
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
                else -> Unit
            }
        }
    }
}