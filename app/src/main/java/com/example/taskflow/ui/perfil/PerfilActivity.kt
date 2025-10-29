package com.example.taskflow.ui.perfil

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.taskflow.R
import com.example.taskflow.databinding.ActivityPerfilBinding

class PerfilActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPerfilBinding
    private val viewModel: PerfilViewModel by viewModels()

    // Nova forma de abrir galeria (substitui startActivityForResult)
    private val selecionarImagemLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.setNovaImagem(it)
        }
    }

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

        binding.btnCancelar.setOnClickListener {
            viewModel.desativarModoEdicao()
            viewModel.carregarDadosUsuario() // Recarrega dados originais
        }

        binding.imageView.setOnClickListener {
            abrirGaleria()
        }

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
                binding.btnCancelar.visibility = android.view.View.VISIBLE
                binding.btnEntrarLogin.backgroundTintList =
                    getColorStateList(R.color.Secundaria)

                // Mudar cor dos textos para preto quando em modo edição
                binding.txtnome.setTextColor(getColor(android.R.color.black))
                binding.txtnick.setTextColor(getColor(android.R.color.black))
            } else {
                binding.txtnome.isEnabled = false
                binding.txtnick.isEnabled = false
                binding.btnEntrarLogin.text = "EDITAR"
                binding.btnCancelar.visibility = android.view.View.GONE
                binding.btnEntrarLogin.backgroundTintList =
                    getColorStateList(R.color.Secundaria)

                // Voltar cor cinza quando desabilitado
                binding.txtnome.setTextColor(getColor(android.R.color.darker_gray))
                binding.txtnick.setTextColor(getColor(android.R.color.darker_gray))
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
        selecionarImagemLauncher.launch("image/*")
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        if (viewModel.modoEdicao.value == true) {
            viewModel.desativarModoEdicao()
        }
    }
}