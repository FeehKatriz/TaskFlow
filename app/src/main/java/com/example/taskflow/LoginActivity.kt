package com.example.taskflow

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.taskflow.databinding.ActivityLoginBinding
import com.example.taskflow.utils.exibirMensagem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class LoginActivity : AppCompatActivity() {

    //Declaração de variáveis que serão usadas Globalmente.

    private lateinit var email: String
    private lateinit var senha: String

    //chamadas de funções

    //função binding para interação dos elementos.
    private val binding by lazy {
        ActivityLoginBinding.inflate(layoutInflater)
    }

    //Coloquei a funcção de chamada do Firebase FORA da OVERRIDE.
    private val firebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        // Ajusta o layout para não ficar escondido atrás da status bar / nav bar
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.teste)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        inicializarEventosClique()

        firebaseAuth.signOut()


    }

    //ciclo de vida para verificar se usuario está logado.
    override fun onStart() {
        super.onStart()
        verificarUsuarioLogado()
    }


    //MÉTODOS PARA FICAR MAIS FÁCIL DE ENTENDER O CÓDIGO E NA oRIENTAÇÃO DE OBJETOS

    //METODO VALIDAR CAMPOS (Boolean) -
    private fun LoginActivity.validarCampos(): Boolean {
        email = binding.editLoginEmail.text.toString().trim()
        senha = binding.editLoginSenha.text.toString().trim()
        if (email.isNotEmpty()) {
            binding.TextInputLayoutLoginEmail.error = null
            if (senha.isNotEmpty()) {
                binding.TextInputLayoutLoginSenha.error = null
                return true
            } else {
                binding.TextInputLayoutLoginSenha.error = "Preencha sua senha"
                return false
            }
        } else {
            binding.TextInputLayoutLoginEmail.error = "Preencha o e-mail"
            return false
        }
    }

    //METODO VERIFICA USUARIO LOGADO
    private fun verificarUsuarioLogado() {
        val usuarioAtual = firebaseAuth.currentUser
        if (usuarioAtual != null) {
            startActivity(
                Intent(this, MainActivity::class.java)
            )
        }
    }

    //METODO DE INICIALIZAR EVENTOS, aqui é onde terá o controle dos eventos.
    private fun inicializarEventosClique() {
        binding.btnSemConta.setOnClickListener {
            startActivity(
                Intent(this, CadastroActivity::class.java)
            )
        }
        binding.btnLogar.setOnClickListener {
            if (validarCampos()) {
                logarUsuario()
            }
        }

        binding.btnEsqueceuSenha.setOnClickListener {
            startActivity(Intent(this, tela_esqueci_minha_senha::class.java))
        }

        //Colocar um biding para os btns


    }

    //METODO DE LOGAR O USUÁRIO
    private fun LoginActivity.logarUsuario() {
        firebaseAuth.signInWithEmailAndPassword(
            email, senha
        ).addOnSuccessListener {
            exibirMensagem("Logado com sucesso!")
            startActivity(
                Intent(this, TelaInicial::class.java)
            )
            finish() //Aqui ele impede de voltar para tela de login.
        }.addOnFailureListener { erro ->
            try {
                throw erro
            } catch (erroUsuaruiInvalido: FirebaseAuthInvalidUserException) {
                erroUsuaruiInvalido.printStackTrace()
                exibirMensagem("e-mail não cadastrado")
            } catch (ErroCredenciaisInvalidas: FirebaseAuthInvalidCredentialsException) {
                ErroCredenciaisInvalidas.printStackTrace()
                exibirMensagem("e-mail ou senha estão incorretos!.")

            }
        }
    }


}


