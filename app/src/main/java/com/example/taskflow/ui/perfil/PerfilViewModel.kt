package com.example.taskflow.ui.perfil

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.taskflow.data.model.Usuario
import com.example.taskflow.data.repository.UsuarioRepository
import com.google.firebase.auth.FirebaseAuth

class PerfilViewModel : ViewModel() {

    private val repository = UsuarioRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _state = MutableLiveData<PerfilState>(PerfilState.Idle)
    val state: LiveData<PerfilState> = _state

    private val _modoEdicao = MutableLiveData<Boolean>(false)
    val modoEdicao: LiveData<Boolean> = _modoEdicao

    private val _usuario = MutableLiveData<Usuario?>()
    val usuario: LiveData<Usuario?> = _usuario

    private val _novaImageUri = MutableLiveData<Uri?>()
    val novaImageUri: LiveData<Uri?> = _novaImageUri

    private val _isGoogleUser = MutableLiveData<Boolean>(false)
    val isGoogleUser: LiveData<Boolean> = _isGoogleUser

    private var dadosOriginais: Usuario? = null

    fun carregarDadosUsuario() {
        val uid = auth.currentUser?.uid ?: run {
            _state.value = PerfilState.Error("Usuário não autenticado")
            return
        }

        // Verificar se é usuário do Google
        val currentUser = auth.currentUser
        val isGoogle = currentUser?.providerData?.any {
            it.providerId == "google.com"
        } ?: false
        _isGoogleUser.value = isGoogle

        _state.value = PerfilState.Loading

        repository.carregarDadosUsuario(uid) { resultado ->
            resultado.onSuccess { usuario ->
                dadosOriginais = usuario
                _usuario.value = usuario
                _state.value = PerfilState.DadosCarregados(usuario)
            }.onFailure { e ->
                _state.value = PerfilState.Error(e.message ?: "Erro ao carregar dados")
            }
        }
    }

    fun ativarModoEdicao() {
        _modoEdicao.value = true
    }

    fun desativarModoEdicao() {
        _modoEdicao.value = false
        _novaImageUri.value = null
    }

    fun setNovaImagem(uri: Uri) {
        _novaImageUri.value = uri
    }

    fun salvarAlteracoes(
        novoNome: String,
        senhaAtual: String,
        novaSenha: String,
        confirmaSenha: String
    ) {
        val uid = auth.currentUser?.uid ?: run {
            _state.value = PerfilState.Error("Usuário não autenticado")
            return
        }

        val novoNomeTrim = novoNome.trim()

        if (novoNomeTrim.isEmpty()) {
            _state.value = PerfilState.Error("Nome é obrigatório")
            return
        }

        val dadosAtuais = dadosOriginais ?: run {
            _state.value = PerfilState.Error("Erro: dados originais não encontrados")
            return
        }

        val nomeAlterado = novoNomeTrim != dadosAtuais.nome
        val temImagemNova = _novaImageUri.value != null
        val querTrocarSenha = senhaAtual.isNotEmpty() || novaSenha.isNotEmpty() || confirmaSenha.isNotEmpty()

        if (!nomeAlterado && !temImagemNova && !querTrocarSenha) {
            _state.value = PerfilState.Error("Nenhuma alteração detectada")
            return
        }

        // Validar troca de senha apenas se não for usuário do Google
        if (querTrocarSenha) {
            if (_isGoogleUser.value == true) {
                _state.value = PerfilState.Error("Usuários com login Google não podem alterar senha")
                return
            }

            if (senhaAtual.isEmpty()) {
                _state.value = PerfilState.Error("Digite sua senha atual")
                return
            }
            if (novaSenha.isEmpty()) {
                _state.value = PerfilState.Error("Digite a nova senha")
                return
            }
            if (confirmaSenha.isEmpty()) {
                _state.value = PerfilState.Error("Confirme a nova senha")
                return
            }
            if (novaSenha != confirmaSenha) {
                _state.value = PerfilState.Error("As senhas não coincidem")
                return
            }
            if (novaSenha.length < 6) {
                _state.value = PerfilState.Error("A nova senha deve ter pelo menos 6 caracteres")
                return
            }
            if (!validarSenhaForte(novaSenha)) {
                _state.value = PerfilState.Error("A senha deve conter letra maiúscula, minúscula, número e caractere especial")
                return
            }
        }

        _state.value = PerfilState.Salvando

        // Se precisa trocar senha, fazer isso primeiro
        if (querTrocarSenha && _isGoogleUser.value == false) {
            repository.trocarSenha(senhaAtual, novaSenha) { resultado ->
                resultado.onSuccess {
                    // Senha trocada, agora atualizar outros dados
                    continuarSalvamento(uid, nomeAlterado, temImagemNova, novoNomeTrim)
                }.onFailure { e ->
                    _state.value = PerfilState.Error(e.message ?: "Erro ao trocar senha")
                }
            }
        } else {
            // Não precisa trocar senha, só atualizar dados
            continuarSalvamento(uid, nomeAlterado, temImagemNova, novoNomeTrim)
        }
    }

    private fun continuarSalvamento(
        uid: String,
        nomeAlterado: Boolean,
        temImagemNova: Boolean,
        novoNome: String
    ) {
        // Se houver imagem nova, fazer upload primeiro
        if (temImagemNova) {
            repository.uploadFotoPerfil(uid, _novaImageUri.value!!) { resultadoUpload ->
                resultadoUpload.onSuccess { fotoUrl ->
                    atualizarFirestore(
                        uid,
                        if (nomeAlterado) novoNome else null,
                        fotoUrl
                    )
                }.onFailure { e ->
                    _state.value = PerfilState.Error("Erro ao enviar foto: ${e.message}")
                }
            }
        } else {
            atualizarFirestore(
                uid,
                if (nomeAlterado) novoNome else null,
                null
            )
        }
    }

    private fun atualizarFirestore(
        uid: String,
        nome: String?,
        fotoUrl: String?
    ) {
        repository.salvarAlteracoesPerfil(uid, nome, fotoUrl) { resultado ->
            resultado.onSuccess {
                // Atualizar dados originais
                dadosOriginais?.apply {
                    if (nome != null) this.nome = nome
                    if (fotoUrl != null) this.fotoUrl = fotoUrl
                }

                // Atualizar estado
                _usuario.value = dadosOriginais
                _state.value = PerfilState.Success
                desativarModoEdicao()
            }.onFailure { e ->
                _state.value = PerfilState.Error("Erro ao salvar dados: ${e.message}")
                desativarModoEdicao()
            }
        }
    }

    private fun validarSenhaForte(senha: String): Boolean {
        val temMaiuscula = senha.any { it.isUpperCase() }
        val temMinuscula = senha.any { it.isLowerCase() }
        val temNumero = senha.any { it.isDigit() }
        val temEspecial = senha.any { !it.isLetterOrDigit() }
        return temMaiuscula && temMinuscula && temNumero && temEspecial
    }

    fun limparEstado() {
        _state.value = PerfilState.Idle
    }
}