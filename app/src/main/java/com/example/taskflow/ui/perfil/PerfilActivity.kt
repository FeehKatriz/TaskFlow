package com.example.taskflow.ui.perfil

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.taskflow.R
import com.example.taskflow.databinding.ActivityPerfilBinding

class PerfilActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPerfilBinding
    private val viewModel: PerfilViewModel by viewModels()
    private val PICK_IMAGE_REQUEST = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPerfilBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.carregarDadosUsuario()
        configurarBotoes()
        observarEstado()
        observarDados()
    }

    private fun configurarBotoes() {
        binding.btnVoltar.setOnClickListener {
            finish()
        }

        binding.btnEntrarLogin.setOnClickListener {
            if (viewModel.modoEdicao.value == true) {
                val nome = binding.txtnome.text.toString()
                val nick = binding.txtnick.text.toString()
                viewModel.salvarAlteracoes(nome, nick)
            } else {
                viewModel.ativarModoEdicao()
            }
        }

        binding.textView3.setOnClickListener { abrirGaleria() }

        // Adicionar botão de voltar no título (opcional)
        binding.textView2.setOnClickListener {
            finish()
        }
    }

    private fun observarEstado() {
        viewModel.state.observe(this) { state ->
            when (state) {
                is PerfilState.Idle -> {}
                is PerfilState.Loading -> {}
                is PerfilState.DadosCarregados -> {
                    preencherCampos(state.usuario)
                }
                is PerfilState.Salvando -> {
                    binding.btnEntrarLogin.isEnabled = false
                    binding.btnEntrarLogin.text = "SALVANDO..."
                }
                is PerfilState.Success -> {
                    binding.btnEntrarLogin.isEnabled = true
                    Toast.makeText(this, "Perfil atualizado com sucesso!", Toast.LENGTH_SHORT).show()

                    // ✅ Sinalizar que houve mudança para atualizar foto na MainActivity
                    setResult(Activity.RESULT_OK)

                    viewModel.limparEstado()
                }
                is PerfilState.Error -> {
                    binding.btnEntrarLogin.isEnabled = true
                    binding.btnEntrarLogin.text = if (viewModel.modoEdicao.value == true) "SALVAR" else "EDITAR"
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                    viewModel.limparEstado()
                }
            }
        }
    }

    private fun observarDados() {
        viewModel.modoEdicao.observe(this) { ativo ->
            if (ativo) {
                binding.txtnome.isEnabled = true
                binding.txtnick.isEnabled = true
                binding.btnEntrarLogin.text = "SALVAR"
                binding.btnEntrarLogin.backgroundTintList =
                    getColorStateList(android.R.color.holo_green_dark)
                Toast.makeText(this, "Modo de edição ativado", Toast.LENGTH_SHORT).show()
            } else {
                binding.txtnome.isEnabled = false
                binding.txtnick.isEnabled = false
                binding.btnEntrarLogin.text = "EDITAR"
                binding.btnEntrarLogin.backgroundTintList =
                    getColorStateList(R.color.Secundaria)
            }
        }

        viewModel.novaImageUri.observe(this) { uri ->
            uri?.let {
                Glide.with(this)
                    .load(it)
                    .placeholder(R.drawable.usertype)
                    .circleCrop()
                    .into(binding.imageView)
            }
        }
    }

    private fun preencherCampos(usuario: com.example.taskflow.data.model.Usuario) {
        binding.txtnome.setText(usuario.nome)
        binding.txtemail.setText(usuario.email)
        binding.txtnick.setText(usuario.nickname)

        binding.txtnome.isEnabled = false
        binding.txtemail.isEnabled = false
        binding.txtnick.isEnabled = false

        if (!usuario.fotoUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(usuario.fotoUrl)
                .placeholder(R.drawable.usertype)
                .circleCrop()
                .into(binding.imageView)
        } else {
            Glide.with(this)
                .load(R.drawable.usertype)
                .circleCrop()
                .into(binding.imageView)
        }
    }

    private fun abrirGaleria() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            data.data?.let { uri ->
                viewModel.setNovaImagem(uri)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        // Se estava em modo edição, cancelar
        if (viewModel.modoEdicao.value == true) {
            viewModel.desativarModoEdicao()
        }
    }
}