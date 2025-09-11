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
import com.example.taskflow.databinding.ActivityTelaCadastroBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class TelaCadastro : AppCompatActivity() {

    private lateinit var binding: ActivityTelaCadastroBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private var imageUri: Uri? = null
    private val PICK_IMAGE_REQUEST = 1001

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

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        binding.btnEntrarLogin.setOnClickListener {
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
        val nome = binding.txtnome.text.toString().trim()
        val email = binding.txtemail.text.toString().trim()
        val nickname = binding.tilSenha.text.toString().trim()
        val senha = binding.txtsenha.text.toString()
        val confirmaSenha = binding.txtconfirmasenha.text.toString()

        if (!validarCampos(nome, email, nickname, senha, confirmaSenha)) return

        binding.btnEntrarLogin.isEnabled = false

        auth.createUserWithEmailAndPassword(email, senha)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
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
                                    binding.btnEntrarLogin.isEnabled = true
                                }
                        } else {
                            salvarDadosFirestore(user.uid, nome, email, nickname, null)
                        }
                    }
                } else {
                    binding.btnEntrarLogin.isEnabled = true
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

        firestore.collection("usuarios")
            .document(uid)
            .set(userData)
            .addOnSuccessListener {
                binding.btnEntrarLogin.isEnabled = true
                Toast.makeText(this, "Cadastro realizado com sucesso!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { exception ->
                binding.btnEntrarLogin.isEnabled = true
                Toast.makeText(this, "Erro ao salvar dados: ${exception.message}", Toast.LENGTH_LONG).show()
                auth.currentUser?.delete()
            }
    }
}