package com.example.taskflow.ui.perfil

import android.R
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.taskflow.databinding.FragmentPerfilBinding
import com.example.taskflow.data.model.Usuario
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage

class PerfilFragment : Fragment() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!

    private var modoEdicao = false
    private var dadosOriginais: Usuario? = null
    private var imageUri: Uri? = null

    private val PICK_IMAGE_REQUEST = 1001

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPerfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        carregarDadosUsuario()
        configurarBotoes()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun configurarBotoes() {
        binding.btnEntrarLogin.setOnClickListener {
            if (!modoEdicao) ativarModoEdicao()
            else salvarAlteracoes()
        }

        binding.textView3.setOnClickListener { abrirGaleria() }
    }

    private fun ativarModoEdicao() {
        modoEdicao = true
        binding.txtnome.isEnabled = true
        binding.txtnick.isEnabled = true
        binding.btnEntrarLogin.text = "SALVAR"
        binding.btnEntrarLogin.backgroundTintList =
            context?.getColorStateList(R.color.holo_green_dark)
        Toast.makeText(context, "Modo de edição ativado", Toast.LENGTH_SHORT).show()
    }

    private fun desativarModoEdicao() {
        modoEdicao = false
        binding.txtnome.isEnabled = false
        binding.txtnick.isEnabled = false
        binding.btnEntrarLogin.text = "EDITAR"
        binding.btnEntrarLogin.backgroundTintList =
            context?.getColorStateList(com.example.taskflow.R.color.Secundaria)
        binding.btnEntrarLogin.isEnabled = true
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
            // Mostrar a imagem selecionada redondinha
            Glide.with(this)
                .load(imageUri)
                .placeholder(com.example.taskflow.R.drawable.usertype)
                .circleCrop()
                .into(binding.imageView)
        }
    }

    private fun carregarDadosUsuario() {
        val user = auth.currentUser ?: return
        db.collection("usuarios")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val usuario = document.toObject(Usuario::class.java)
                    usuario?.let {
                        dadosOriginais = it
                        preencherCampos(it)
                    }
                } else {
                    Toast.makeText(context, "Erro ao carregar dados do perfil", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Erro ao carregar perfil", Toast.LENGTH_SHORT).show()
            }
    }

    private fun preencherCampos(usuario: Usuario) {
        binding.txtnome.setText(usuario.nome)
        binding.txtemail.setText(usuario.email)
        binding.txtnick.setText(usuario.nickname)

        binding.txtnome.isEnabled = false
        binding.txtemail.isEnabled = false
        binding.txtnick.isEnabled = false

        // Carregar foto redonda
        if (!usuario.fotoUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(usuario.fotoUrl)
                .placeholder(com.example.taskflow.R.drawable.usertype)
                .circleCrop() // <-- imagem redonda
                .into(binding.imageView)
        } else {
            Glide.with(this)
                .load(com.example.taskflow.R.drawable.usertype)
                .circleCrop()
                .into(binding.imageView)
        }
    }

    private fun salvarAlteracoes() {
        val user = auth.currentUser ?: run {
            Toast.makeText(context, "Usuário não autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        val novoNome = binding.txtnome.text.toString().trim()
        val novoNick = binding.txtnick.text.toString().trim()

        if (novoNome.isEmpty() || novoNick.isEmpty()) {
            Toast.makeText(context, "Nome e nickname são obrigatórios", Toast.LENGTH_SHORT).show()
            return
        }

        val dadosAtuais = dadosOriginais ?: run {
            Toast.makeText(context, "Erro: dados originais não encontrados", Toast.LENGTH_SHORT).show()
            return
        }

        val nomeAlterado = novoNome != dadosAtuais.nome
        val nickAlterado = novoNick != dadosAtuais.nickname

        if (!nomeAlterado && !nickAlterado && imageUri == null) {
            Toast.makeText(context, "Nenhuma alteração detectada", Toast.LENGTH_SHORT).show()
            desativarModoEdicao()
            return
        }

        binding.btnEntrarLogin.isEnabled = false
        binding.btnEntrarLogin.text = "SALVANDO..."

        // Se houver imagem nova, faz upload primeiro
        if (imageUri != null) {
            val storageRef = FirebaseStorage.getInstance().reference
            val fotoRef = storageRef.child("usuarios/${user.uid}/fotoPerfil.jpg")
            fotoRef.putFile(imageUri!!)
                .addOnSuccessListener {
                    fotoRef.downloadUrl.addOnSuccessListener { uri ->
                        atualizarFirestore(user.uid, novoNome, novoNick, nomeAlterado, nickAlterado, uri.toString())
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Erro ao enviar foto: ${e.message}", Toast.LENGTH_LONG).show()
                    binding.btnEntrarLogin.isEnabled = true
                    binding.btnEntrarLogin.text = "SALVAR"
                }
        } else {
            atualizarFirestore(user.uid, novoNome, novoNick, nomeAlterado, nickAlterado, null)
        }
    }

    private fun atualizarFirestore(
        uid: String,
        nome: String,
        nickname: String,
        nomeAlterado: Boolean,
        nickAlterado: Boolean,
        fotoUrl: String?
    ) {
        val dadosAtualizados = mutableMapOf<String, Any>()
        if (nomeAlterado) dadosAtualizados["nome"] = nome
        if (nickAlterado) dadosAtualizados["nickname"] = nickname
        if (fotoUrl != null) dadosAtualizados["fotoUrl"] = fotoUrl

        db.collection("usuarios")
            .document(uid)
            .update(dadosAtualizados)
            .addOnSuccessListener {
                dadosOriginais?.apply {
                    if (nomeAlterado) this.nome = nome
                    if (nickAlterado) this.nickname = nickname
                    if (fotoUrl != null) this.fotoUrl = fotoUrl
                }
                Toast.makeText(context, "Perfil atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                desativarModoEdicao()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Erro ao salvar dados: ${e.message}", Toast.LENGTH_LONG).show()
                desativarModoEdicao()
            }
    }
}