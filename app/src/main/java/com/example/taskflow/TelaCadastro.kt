package com.example.taskflow

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.taskflow.databinding.ActivityTelaCadastroBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TelaCadastro : AppCompatActivity() {

    private lateinit var binding: ActivityTelaCadastroBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityTelaCadastroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.teste) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        binding.btnEntrarLogin.setOnClickListener {
            cadastrarUsuario()
        }
    }

    private fun cadastrarUsuario() {
        // Capturar dados dos campos
        val nome = binding.txtnome.text.toString().trim()
        val email = binding.txtemail.text.toString().trim()
        val nickname = binding.tilSenha.text.toString().trim()
        val senha = binding.txtsenha.text.toString()
        val confirmaSenha = binding.txtconfirmasenha.text.toString()

        // Validar campos
        if (!validarCampos(nome, email, nickname, senha, confirmaSenha)) {
            return
        }

        // Desabilitar botão para evitar múltiplos cliques
        binding.btnEntrarLogin.isEnabled = false

        // Criar usuário no Firebase Auth
        auth.createUserWithEmailAndPassword(email, senha)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Usuário criado com sucesso, agora salvar dados no Firestore
                    val user = auth.currentUser
                    user?.let {
                        salvarDadosFirestore(it.uid, nome, email, nickname)
                    }
                } else {
                    // Erro ao criar usuário
                    binding.btnEntrarLogin.isEnabled = true
                    val errorMessage = task.exception?.message ?: "Erro desconhecido"
                    Toast.makeText(this, "Erro no cadastro: $errorMessage", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun validarCampos(nome: String, email: String, nickname: String, senha: String, confirmaSenha: String): Boolean {

        if (nome.isEmpty() || email.isEmpty() || nickname.isEmpty() || senha.isEmpty() || confirmaSenha.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Email inválido", Toast.LENGTH_SHORT).show()
            return false
        }

        if (nickname.length < 3) {
            Toast.makeText(this, "Nickname muito curto", Toast.LENGTH_SHORT).show()
            return false
        }

        if (senha.length < 6) {
            Toast.makeText(this, "Senha muito curta", Toast.LENGTH_SHORT).show()
            return false
        }

        if (senha != confirmaSenha) {
            Toast.makeText(this, "Senhas não coincidem", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun salvarDadosFirestore(uid: String, nome: String, email: String, nickname: String) {
        // Criar mapa com os dados do usuário
        val userData = hashMapOf(
            "nome" to nome,
            "email" to email,
            "nickname" to nickname,
            "fotoUrl" to "", // Vazio por enquanto, será preenchido quando adicionar foto
            "dataCriacao" to com.google.firebase.Timestamp.now()
        )

        // Salvar no Firestore
        firestore.collection("usuarios")
            .document(uid)
            .set(userData)
            .addOnSuccessListener {
                // Dados salvos com sucesso
                binding.btnEntrarLogin.isEnabled = true
                Toast.makeText(this, "Cadastro realizado com sucesso!", Toast.LENGTH_SHORT).show()
                finish() // Voltar para a tela anterior
            }
            .addOnFailureListener { exception ->
                // Erro ao salvar dados
                binding.btnEntrarLogin.isEnabled = true
                Toast.makeText(this, "Erro ao salvar dados: ${exception.message}", Toast.LENGTH_LONG).show()

                // Opcional: deletar usuário do Auth se falhar ao salvar no Firestore
                auth.currentUser?.delete()
            }
    }
}