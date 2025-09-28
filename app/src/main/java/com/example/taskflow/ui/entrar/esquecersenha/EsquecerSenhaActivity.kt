package com.example.taskflow.ui.entrar.esquecersenha

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.taskflow.R
import com.example.taskflow.databinding.ActivityTelaEsqueciMinhaSenhaBinding
import com.example.taskflow.ui.entrar.EntrarActivity
import com.google.firebase.auth.FirebaseAuth

class EsquecerSenhaActivity : AppCompatActivity() {

    //Declaração de variáveis que serão usadas Globalmente.
    private lateinit var email: String


    //função binding para interação dos elementos.
    private val binding by lazy {
        ActivityTelaEsqueciMinhaSenhaBinding.inflate(layoutInflater)
    }
    //chamada do banco de dados
    private val firebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        inicializarEventosClique()

    }

    private fun inicializarEventosClique() {
        binding.btnvoltarLogin.setOnClickListener{
            startActivity(Intent(this, EntrarActivity::class.java))
        }
        binding.btnRecupearSenha.setOnClickListener {
            if(validaCampoEmail()){
                enviarEmailRecuperacao()
            }
        }


    }

    private fun validaCampoEmail(): Boolean {
        email = binding.emailRecuperacao.text.toString().trim()
        return if (email.isNotEmpty()) {
            if (Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.textInputLayoutEmail.error = null
                true
            } else {
                binding.textInputLayoutEmail.error = "Formato de email inválido"
                false
            }
        } else {
            binding.textInputLayoutEmail.error = "Preencha o e-mail"
            false
        }
    }

    private fun enviarEmailRecuperacao() {
        firebaseAuth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                Toast.makeText(this, "Email de recuperação enviado!", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, EntrarActivity::class.java))
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao enviar email. Verifique se está cadastrado.", Toast.LENGTH_LONG).show()
            }
    }

}