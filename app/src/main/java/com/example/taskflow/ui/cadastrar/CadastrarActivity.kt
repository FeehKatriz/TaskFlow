package com.example.taskflow.ui.cadastrar

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.taskflow.R
import com.example.taskflow.databinding.ActivityCadastrarBinding
import com.example.taskflow.ui.entrar.EmailVerificationDialog
import com.google.firebase.auth.FirebaseAuth

class CadastrarActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCadastrarBinding
    private val viewModel: CadastrarViewModel by viewModels()
    private var imagemSelecionada: Uri? = null
    private var emailVerificationDialog: EmailVerificationDialog? = null

    private val selecionarImagemLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            imagemSelecionada = it
            Glide.with(this)
                .load(it)
                .placeholder(R.drawable.usertype)
                .circleCrop()
                .into(binding.imageView)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCadastrarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarBotoes()
        observarEstado()
    }

    private fun configurarBotoes() {
        binding.imageView.setOnClickListener {
            selecionarImagemLauncher.launch("image/*")
        }

        binding.btnCadastrar.setOnClickListener {
            val nome = binding.editNome.text.toString()
            val email = binding.editEmail.text.toString()
            val senha = binding.editSenha.text.toString()
            val confirmaSenha = binding.editConfirmarSenha.text.toString()

            viewModel.cadastrarUsuario(nome, email, senha, confirmaSenha, imagemSelecionada)
        }

        binding.btnVoltarLogin.setOnClickListener {
            finish()
        }
    }

    private fun observarEstado() {
        viewModel.state.observe(this) { state ->
            when (state) {
                is CadastrarState.Idle -> {
                    binding.btnCadastrar.isEnabled = true
                    binding.btnCadastrar.text = "CADASTRAR"
                }

                is CadastrarState.Loading -> {
                    binding.btnCadastrar.isEnabled = false
                    binding.btnCadastrar.text = "CADASTRANDO..."
                }

                is CadastrarState.EmailVerificationSent -> {
                    binding.btnCadastrar.isEnabled = true
                    binding.btnCadastrar.text = "CADASTRAR"
                    mostrarDialogVerificacao(state.email)
                }

                is CadastrarState.EmailResent -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }

                is CadastrarState.Success -> {
                    Toast.makeText(this, "Cadastro realizado com sucesso!", Toast.LENGTH_SHORT).show()
                    finish()
                }

                is CadastrarState.Error -> {
                    binding.btnCadastrar.isEnabled = true
                    binding.btnCadastrar.text = "CADASTRAR"
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun mostrarDialogVerificacao(email: String) {
        emailVerificationDialog = EmailVerificationDialog(
            context = this,
            email = email,
            onReenviar = {
                viewModel.reenviarEmailVerificacao()
            },
            onFechar = {
                // Fazer logout ao fechar o dialog
                FirebaseAuth.getInstance().signOut()
                viewModel.voltarParaIdle()
                // Voltar para a tela de login
                setResult(RESULT_OK)
                finish()
            }
        )
        emailVerificationDialog?.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        emailVerificationDialog?.dismiss()
    }
}