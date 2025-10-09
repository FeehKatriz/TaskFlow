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
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.example.taskflow.databinding.FragmentPerfilBinding

class PerfilFragment : Fragment() {

    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PerfilViewModel by viewModels()

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

        viewModel.carregarDadosUsuario()
        configurarBotoes()
        observarEstado()
        observarDados()
    }

    private fun configurarBotoes() {
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
    }

    private fun observarEstado() {
        viewModel.state.observe(viewLifecycleOwner) { state ->
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
                    Toast.makeText(context, "Perfil atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                    viewModel.limparEstado()
                }
                is PerfilState.Error -> {
                    binding.btnEntrarLogin.isEnabled = true
                    binding.btnEntrarLogin.text = if (viewModel.modoEdicao.value == true) "SALVAR" else "EDITAR"
                    Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                    viewModel.limparEstado()
                }
            }
        }
    }

    private fun observarDados() {
        viewModel.modoEdicao.observe(viewLifecycleOwner) { ativo ->
            if (ativo) {
                binding.txtnome.isEnabled = true
                binding.txtnick.isEnabled = true
                binding.btnEntrarLogin.text = "SALVAR"
                binding.btnEntrarLogin.backgroundTintList =
                    context?.getColorStateList(R.color.holo_green_dark)
                Toast.makeText(context, "Modo de edição ativado", Toast.LENGTH_SHORT).show()
            } else {
                binding.txtnome.isEnabled = false
                binding.txtnick.isEnabled = false
                binding.btnEntrarLogin.text = "EDITAR"
                binding.btnEntrarLogin.backgroundTintList =
                    context?.getColorStateList(com.example.taskflow.R.color.Secundaria)
            }
        }

        viewModel.novaImageUri.observe(viewLifecycleOwner) { uri ->
            uri?.let {
                Glide.with(this)
                    .load(it)
                    .placeholder(com.example.taskflow.R.drawable.usertype)
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
                .placeholder(com.example.taskflow.R.drawable.usertype)
                .circleCrop()
                .into(binding.imageView)
        } else {
            Glide.with(this)
                .load(com.example.taskflow.R.drawable.usertype)
                .circleCrop()
                .into(binding.imageView)
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
            data.data?.let { uri ->
                viewModel.setNovaImagem(uri)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}