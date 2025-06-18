package com.example.taskflow

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.taskflow.databinding.ActivityTelaEntrarBinding
import com.google.firebase.auth.FirebaseAuth

class TelaEntrar : AppCompatActivity() {

    private val binding by lazy {
        ActivityTelaEntrarBinding.inflate(layoutInflater)
    }

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.btnEntrarLogin.setOnClickListener {
            val email = binding.txtnome.text.toString().trim()
            val senha = binding.txtSenha.text.toString().trim()

            if (email.isNotEmpty() && senha.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, senha)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            startActivity(Intent(this, TelaInicial::class.java))
                            finish()
                        } else {
                            val error = task.exception?.message ?: "Erro desconhecido"
                            Toast.makeText(this, "Erro: $error", Toast.LENGTH_LONG).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSemConta.setOnClickListener {
            startActivity(Intent(this, TelaCadastro::class.java))
        }

        binding.btnEsqueceuSenha.setOnClickListener {
            startActivity(Intent(this, tela_esqueci_minha_senha::class.java))
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.teste)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}