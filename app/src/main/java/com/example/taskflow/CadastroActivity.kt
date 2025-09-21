package com.example.taskflow

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.taskflow.databinding.ActivityCadastroBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class CadastroActivity : AppCompatActivity() {
    //chamada do Binding para manipulação de objetos
    private val binding by lazy {
       ActivityCadastroBinding.inflate(layoutInflater)
    }

    //chamada para variáveis
    private lateinit var nome: String
    private lateinit var email: String
    private lateinit var nickname: String
    private lateinit var senha: String
    private lateinit var confirmaSenha: String

    //Chamada dos bancos
    private val firebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }
    private val fireStore by lazy {
        FirebaseFirestore.getInstance()
    }

    //Chamadas para Storage imagens
    private var imageUri: Uri? = null
    private val PICK_IMAGE_REQUEST = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.teste) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnCadastrar.setOnClickListener {
            cadastrarUsuario()
        }

        // Clicar na imagem grande para escolher foto
        binding.imageView.setOnClickListener {
            abrirGaleria()
        }
    }

    private fun abrirGaleria() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            imageUri = data.data
            Glide.with(this)
                .load(imageUri)
                .placeholder(R.drawable.usertype)
                .circleCrop() // Deixa a imagem redonda
                .into(binding.imageView)
        }
    }

    private fun cadastrarUsuario() {
        nome = binding.editNome.text.toString().trim() // Supondo que você tenha um EditText com id editTextNome
        email = binding.editEmail.text.toString().trim() // Supondo que você tenha um EditText com id editTextEmail
        nickname = binding.editNick.text.toString().trim() // Supondo que você tenha um EditText com id editTextNick
        senha = binding.editSenha.text.toString().trim() // Supondo que você tenha um EditText com id editTextSenha
        confirmaSenha = binding.editConfirmarSenha.text.toString().trim() // Supondo que você tenha um EditText com id editTextConfirmarSenha
        if (!validarCampos(nome, email, nickname, senha, confirmaSenha)) return

        binding.btnCadastrar.isEnabled = false

        firebaseAuth.createUserWithEmailAndPassword(email, senha)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    user?.let {
                        if (imageUri != null) {
                            val storageRef = FirebaseStorage.getInstance().reference
                            val fotoRef = storageRef.child("usuarios/${user.uid}/fotoPerfil.jpg")
                            fotoRef.putFile(imageUri!!)
                                .addOnSuccessListener {
                                    fotoRef.downloadUrl.addOnSuccessListener { uri ->
                                        salvarDadosFirestore(user.uid, nome, email, nickname, uri.toString())
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Erro ao enviar foto: ${e.message}", Toast.LENGTH_LONG).show()
                                    binding.btnCadastrar.isEnabled = true
                                }
                        } else {
                            salvarDadosFirestore(user.uid, nome, email, nickname, null)
                        }
                    }
                } else {
                    binding.btnCadastrar.isEnabled = true
                    val errorMessage = task.exception?.message ?: "Erro desconhecido"
                    Toast.makeText(this, "Erro no cadastro: $errorMessage", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun validarCampos(
        nome: String,
        email: String,
        nickname: String,
        senha: String,
        confirmaSenha: String
    ): Boolean {
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

    private fun salvarDadosFirestore(
        uid: String,
        nome: String,
        email: String,
        nickname: String,
        fotoUrl: String?
    ) {
        val userData = hashMapOf(
            "nome" to nome,
            "email" to email,
            "nickname" to nickname,
            "fotoUrl" to (fotoUrl ?: ""),
            "dataCriacao" to com.google.firebase.Timestamp.now()
        )

        fireStore.collection("usuarios")
            .document(uid)
            .set(userData)
            .addOnSuccessListener {
                binding.btnCadastrar.isEnabled = true
                Toast.makeText(this, "Cadastro realizado com sucesso!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, IntroActivity::class.java))
                finish()
            }
            .addOnFailureListener { exception ->
                binding.btnCadastrar.isEnabled = true
                Toast.makeText(this, "Erro ao salvar dados: ${exception.message}", Toast.LENGTH_LONG).show()
                firebaseAuth.currentUser?.delete()
            }
    }
}