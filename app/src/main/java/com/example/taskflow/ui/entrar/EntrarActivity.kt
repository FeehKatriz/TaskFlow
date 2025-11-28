package com.example.taskflow.ui.entrar

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.taskflow.R
import com.example.taskflow.databinding.ActivityLoginBinding
import com.example.taskflow.ui.cadastrar.CadastrarActivity
import com.example.taskflow.ui.entrar.esquecersenha.EsquecerSenhaActivity
import com.example.taskflow.ui.main.MainActivity
import com.example.taskflow.utils.exibirMensagem
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task

class EntrarActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityLoginBinding.inflate(layoutInflater)
    }

    private val viewModel: EntrarViewModel by viewModels()
    private var loginEmailVerificationDialog: LoginEmailVerificationDialog? = null

    private lateinit var googleSignInClient: GoogleSignInClient

    // Launcher para o resultado do Google Sign-In
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        handleGoogleSignInResult(task)
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

        configurarGoogleSignIn()
        configurarListeners()
        observarEstado()
        observarErros()
    }

    private fun configurarGoogleSignIn() {
        // Configurar o Google Sign-In com o Web Client ID em res/values/strings.xml
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun configurarListeners() {
        binding.btnSemConta.setOnClickListener {
            startActivity(Intent(this, CadastrarActivity::class.java))
        }

        binding.btnLogar.setOnClickListener {
            val email = binding.editLoginEmail.text.toString()
            val senha = binding.editLoginSenha.text.toString()
            viewModel.entrar(email, senha)
        }

        binding.btnEsqueceuSenha.setOnClickListener {
            startActivity(Intent(this, EsquecerSenhaActivity::class.java))
        }

        // Listener para o botão do Google
        binding.btnGoogleSignIn.setOnClickListener {
            iniciarLoginGoogle()
        }
    }

    private fun iniciarLoginGoogle() {
        // Fazer sign out primeiro para sempre mostrar a escolha de conta
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }

    private fun handleGoogleSignInResult(task: Task<GoogleSignInAccount>) {
        try {
            val account = task.getResult(ApiException::class.java)
            // Autenticar com Firebase usando o ID token
            account.idToken?.let { idToken ->
                viewModel.entrarComGoogle(idToken)
            } ?: run {
                exibirMensagem("Erro ao obter token do Google")
            }
        } catch (e: ApiException) {
            Log.e("EntrarActivity", "Google sign in failed", e)
            exibirMensagem("Erro ao fazer login com Google: ${e.message}")
        }
    }

    private fun observarEstado() {
        viewModel.state.observe(this) { state ->
            when (state) {
                is EntrarState.Idle -> {
                    habilitarBotao()
                }
                is EntrarState.Loading -> {
                    desabilitarBotao()
                }
                is EntrarState.Success -> {
                    habilitarBotao()
                    exibirMensagem("Logado com sucesso!")
                    navegarParaHome()
                }
                is EntrarState.EmailNotVerified -> {
                    habilitarBotao()
                    mostrarDialogVerificacao(state.email)
                }
                is EntrarState.EmailResent -> {
                    exibirMensagem(state.message)
                }
                is EntrarState.Error -> {
                    habilitarBotao()
                    exibirMensagem(state.message)
                    viewModel.limparEstado()
                }
            }
        }
    }

    private fun observarErros() {
        viewModel.emailErro.observe(this) { erro ->
            binding.TextInputLayoutLoginEmail.error = erro
        }

        viewModel.senhaErro.observe(this) { erro ->
            binding.TextInputLayoutLoginSenha.error = erro
        }
    }

    private fun mostrarDialogVerificacao(email: String) {
        loginEmailVerificationDialog = LoginEmailVerificationDialog(
            context = this,
            email = email,
            onReenviar = {
                viewModel.reenviarEmailVerificacao()
            },
            onFechar = {
                viewModel.limparEstado()
            }
        )
        loginEmailVerificationDialog?.show()
    }

    private fun desabilitarBotao() {
        binding.btnLogar.isEnabled = false
    }

    private fun habilitarBotao() {
        binding.btnLogar.isEnabled = true
    }

    private fun navegarParaHome() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        loginEmailVerificationDialog?.dismiss()
    }
}