package com.example.taskflow.ui.equipe

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.taskflow.data.model.Equipe
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EquipeViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _minhasEquipes = MutableLiveData<List<Equipe>>()
    val minhasEquipes: LiveData<List<Equipe>> = _minhasEquipes

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _erro = MutableLiveData<String?>()
    val erro: LiveData<String?> = _erro

    fun carregarMinhasEquipes() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _erro.value = "Usuário não autenticado"
            return
        }

        _isLoading.value = true

        // Buscar equipes onde o usuário é membro
        firestore.collection("equipes")
            .whereArrayContains("membros", userId)
            .addSnapshotListener { snapshot, error ->
                _isLoading.value = false

                if (error != null) {
                    _erro.value = "Erro ao carregar equipes: ${error.message}"
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val equipes = snapshot.toObjects(Equipe::class.java)
                    _minhasEquipes.value = equipes
                } else {
                    _minhasEquipes.value = emptyList()
                }
            }
    }

    fun limparErro() {
        _erro.value = null
    }
}