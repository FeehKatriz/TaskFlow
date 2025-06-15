package com.example.taskflow.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.taskflow.R
import com.example.taskflow.databinding.FragmentPerfilBinding
import com.example.taskflow.models.Usuario
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class PerfilFragment : Fragment() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!

    private var modoEdicao = false
    private var dadosOriginais: Usuario? = null

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
            if (!modoEdicao) {
                ativarModoEdicao()
            } else {
                salvarAlteracoes()
            }
        }

        binding.textView3.setOnClickListener {
            Toast.makeText(context, "Funcionalidade de foto em desenvolvimento", Toast.LENGTH_SHORT).show()
        }
    }

    private fun ativarModoEdicao() {
        modoEdicao = true

        binding.txtnome.isEnabled = true
        binding.txtnick.isEnabled = true

        binding.btnEntrarLogin.text = "SALVAR"
        binding.btnEntrarLogin.backgroundTintList = context?.getColorStateList(android.R.color.holo_green_dark)

        Toast.makeText(context, "Modo de edição ativado", Toast.LENGTH_SHORT).show()
    }

    private fun desativarModoEdicao() {
        modoEdicao = false

        binding.txtnome.isEnabled = false
        binding.txtnick.isEnabled = false

        binding.btnEntrarLogin.text = "EDITAR"
        binding.btnEntrarLogin.backgroundTintList = context?.getColorStateList(R.color.Secundaria)
        binding.btnEntrarLogin.isEnabled = true
    }

    private fun salvarAlteracoes() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(context, "Usuário não autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        val novoNome = binding.txtnome.text.toString().trim()
        val novoNick = binding.txtnick.text.toString().trim()

        // Validar campos obrigatórios
        if (novoNome.isEmpty() || novoNick.isEmpty()) {
            Toast.makeText(context, "Nome e nickname são obrigatórios", Toast.LENGTH_SHORT).show()
            return
        }

        // Verificar se houve mudanças
        val dadosAtuais = dadosOriginais
        if (dadosAtuais == null) {
            Toast.makeText(context, "Erro: dados originais não encontrados", Toast.LENGTH_SHORT).show()
            return
        }

        val nomeAlterado = novoNome != dadosAtuais.nome
        val nickAlterado = novoNick != dadosAtuais.nickname

        // Se nada foi alterado
        if (!nomeAlterado && !nickAlterado) {
            Toast.makeText(context, "Nenhuma alteração detectada", Toast.LENGTH_SHORT).show()
            desativarModoEdicao()
            return
        }

        // Mostrar loading
        binding.btnEntrarLogin.isEnabled = false
        binding.btnEntrarLogin.text = "SALVANDO..."

        atualizarFirestore(user.uid, novoNome, novoNick, nomeAlterado, nickAlterado)
    }

    private fun atualizarFirestore(
        uid: String,
        nome: String,
        nickname: String,
        nomeAlterado: Boolean,
        nickAlterado: Boolean
    ) {
        // Criar mapa apenas com os campos alterados
        val dadosAtualizados = mutableMapOf<String, Any>()

        if (nomeAlterado) dadosAtualizados["nome"] = nome
        if (nickAlterado) dadosAtualizados["nickname"] = nickname

        db.collection("usuarios")
            .document(uid)
            .update(dadosAtualizados)
            .addOnSuccessListener {
                Log.d("PerfilFragment", "Dados atualizados no Firestore: $dadosAtualizados")

                // Atualizar dados originais
                dadosOriginais?.apply {
                    if (nomeAlterado) this.nome = nome
                    if (nickAlterado) this.nickname = nickname
                }

                Toast.makeText(context, "Perfil atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                desativarModoEdicao()
            }
            .addOnFailureListener { exception ->
                Log.e("PerfilFragment", "Erro ao atualizar Firestore: ", exception)
                Toast.makeText(context, "Erro ao salvar dados: ${exception.message}", Toast.LENGTH_LONG).show()
                desativarModoEdicao()
            }
    }

    private fun carregarDadosUsuario() {
        val user = auth.currentUser
        if (user != null) {
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
                        Log.e("PerfilFragment", "Usuário não encontrado no Firestore")
                        Toast.makeText(context, "Erro ao carregar dados do perfil", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("PerfilFragment", "Erro ao carregar dados: ", exception)
                    Toast.makeText(context, "Erro ao carregar perfil", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun preencherCampos(usuario: Usuario) {
        binding.txtnome.setText(usuario.nome)
        binding.txtemail.setText(usuario.email)
        binding.txtnick.setText(usuario.nickname)

        // Campos desabilitados inicialmente (email sempre desabilitado)
        binding.txtnome.isEnabled = false
        binding.txtemail.isEnabled = false
        binding.txtnick.isEnabled = false
    }
}