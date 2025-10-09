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

    private var dadosOriginais: Usuario? = null

    fun carregarDadosUsuario() {
        val uid = auth.currentUser?.uid ?: run {
            _state.value = PerfilState.Error("Usuário não autenticado")
            return
        }

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

    fun salvarAlteracoes(novoNome: String, novoNick: String) {
        val uid = auth.currentUser?.uid ?: run {
            _state.value = PerfilState.Error("Usuário não autenticado")
            return
        }

        val novoNomeTrim = novoNome.trim()
        val novoNickTrim = novoNick.trim()

        if (novoNomeTrim.isEmpty() || novoNickTrim.isEmpty()) {
            _state.value = PerfilState.Error("Nome e nickname são obrigatórios")
            return
        }

        val dadosAtuais = dadosOriginais ?: run {
            _state.value = PerfilState.Error("Erro: dados originais não encontrados")
            return
        }

        val nomeAlterado = novoNomeTrim != dadosAtuais.nome
        val nickAlterado = novoNickTrim != dadosAtuais.nickname
        val temImagemNova = _novaImageUri.value != null

        if (!nomeAlterado && !nickAlterado && !temImagemNova) {
            _state.value = PerfilState.Error("Nenhuma alteração detectada")
            return
        }

        _state.value = PerfilState.Salvando

        // Se houver imagem nova, fazer upload primeiro
        if (temImagemNova) {
            repository.uploadFotoPerfil(uid, _novaImageUri.value!!) { resultadoUpload ->
                resultadoUpload.onSuccess { fotoUrl ->
                    atualizarFirestore(
                        uid,
                        if (nomeAlterado) novoNomeTrim else null,
                        if (nickAlterado) novoNickTrim else null,
                        fotoUrl
                    )
                }.onFailure { e ->
                    _state.value = PerfilState.Error("Erro ao enviar foto: ${e.message}")
                }
            }
        } else {
            atualizarFirestore(
                uid,
                if (nomeAlterado) novoNomeTrim else null,
                if (nickAlterado) novoNickTrim else null,
                null
            )
        }
    }

    private fun atualizarFirestore(
        uid: String,
        nome: String?,
        nickname: String?,
        fotoUrl: String?
    ) {
        repository.salvarAlteracoesPerfil(uid, nome, nickname, fotoUrl) { resultado ->
            resultado.onSuccess {
                // Atualizar dados originais
                dadosOriginais?.apply {
                    if (nome != null) this.nome = nome
                    if (nickname != null) this.nickname = nickname
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

    fun limparEstado() {
        _state.value = PerfilState.Idle
    }
}