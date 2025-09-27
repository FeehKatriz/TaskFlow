package com.example.taskflow.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taskflow.adapters.SelecionarMembrosProjetoAdapter
import com.example.taskflow.databinding.BottomsheetGerenciarMembrosBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class GerenciarMembrosBottomSheet(
    private val equipeId: String,
    private val projetoId: String,
    private val onMembrosAtualizados: () -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomsheetGerenciarMembrosBinding? = null
    private val binding get() = _binding!!

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adapter: SelecionarMembrosProjetoAdapter

    private var membrosProjeto = mutableListOf<Map<String, String>>()
    private var membrosEquipe = mutableSetOf<String>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomsheetGerenciarMembrosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configurarRecyclerView()
        carregarDados()
        configurarBotoes()
    }

    private fun configurarRecyclerView() {
        adapter = SelecionarMembrosProjetoAdapter { memberId, selecionado ->
            if (selecionado) {
                membrosEquipe.add(memberId)
            } else {
                membrosEquipe.remove(memberId)
            }
            atualizarContadorSelecionados()
        }

        binding.rvMembrosEquipe.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMembrosEquipe.adapter = adapter
    }

    private fun configurarBotoes() {
        binding.btnSalvar.setOnClickListener {
            salvarMembros()
        }

        binding.btnCancelar.setOnClickListener {
            dismiss()
        }

        binding.btnSelecionarTodos.setOnClickListener {
            selecionarTodos()
        }

        binding.btnLimparSelecao.setOnClickListener {
            limparSelecao()
        }
    }

    private fun carregarDados() {
        binding.progressBar.visibility = View.VISIBLE

        // Carregar membros atuais da equipe
        firestore.collection("equipes")
            .document(equipeId)
            .get()
            .addOnSuccessListener { equipeDoc ->
                if (equipeDoc.exists()) {
                    val membrosAtuais = equipeDoc.get("membros") as? List<String> ?: emptyList()
                    membrosEquipe.addAll(membrosAtuais)

                    // Carregar membros do projeto
                    carregarMembrosProjeto()
                } else {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Equipe não encontrada", Toast.LENGTH_SHORT).show()
                    dismiss()
                }
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Erro ao carregar equipe", Toast.LENGTH_SHORT).show()
                dismiss()
            }
    }

    private fun carregarMembrosProjeto() {
        firestore.collection("projetos")
            .document(projetoId)
            .get()
            .addOnSuccessListener { projetoDoc ->
                if (projetoDoc.exists()) {
                    val membrosIds = projetoDoc.get("membros") as? List<String> ?: emptyList()
                    val usuarioAtualId = auth.currentUser?.uid

                    var processedCount = 0
                    membrosProjeto.clear()

                    membrosIds.forEach { userId ->
                        firestore.collection("usuarios")
                            .document(userId)
                            .get()
                            .addOnSuccessListener { userDoc ->
                                processedCount++
                                if (userDoc.exists()) {
                                    val nomeUsuario = userDoc.getString("nome") ?: "Usuário"
                                    val nomeExibir = if (userId == usuarioAtualId) "Você" else nomeUsuario

                                    val membro = mapOf(
                                        "uid" to userDoc.id,
                                        "nome" to nomeExibir,
                                        "email" to (userDoc.getString("email") ?: ""),
                                        "fotoPerfil" to userId // Para carregar a foto
                                    )
                                    membrosProjeto.add(membro)
                                }

                                if (processedCount == membrosIds.size) {
                                    val membrosOrdenados = membrosProjeto.sortedBy {
                                        if (it["nome"] == "Você") 0 else 1
                                    }
                                    adapter.atualizarMembros(membrosOrdenados, membrosEquipe)
                                    atualizarContadorSelecionados()
                                    binding.progressBar.visibility = View.GONE
                                }
                            }
                            .addOnFailureListener {
                                processedCount++
                                if (processedCount == membrosIds.size) {
                                    adapter.atualizarMembros(membrosProjeto, membrosEquipe)
                                    atualizarContadorSelecionados()
                                    binding.progressBar.visibility = View.GONE
                                }
                            }
                    }
                } else {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Projeto não encontrado", Toast.LENGTH_SHORT).show()
                    dismiss()
                }
            }
    }

    private fun atualizarContadorSelecionados() {
        binding.tvContador.text = "${membrosEquipe.size} de ${membrosProjeto.size} selecionados"
    }

    private fun selecionarTodos() {
        membrosEquipe.clear()
        membrosProjeto.forEach { membro ->
            membro["uid"]?.let { membrosEquipe.add(it) }
        }
        adapter.atualizarSelecao(membrosEquipe)
        atualizarContadorSelecionados()
    }

    private fun limparSelecao() {
        membrosEquipe.clear()
        adapter.atualizarSelecao(membrosEquipe)
        atualizarContadorSelecionados()
    }

    private fun salvarMembros() {
        binding.btnSalvar.isEnabled = false
        binding.btnSalvar.text = "Salvando..."

        firestore.collection("equipes")
            .document(equipeId)
            .update("membros", membrosEquipe.toList())
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Membros atualizados com sucesso!", Toast.LENGTH_SHORT).show()
                onMembrosAtualizados()
                dismiss()
            }
            .addOnFailureListener { e ->
                binding.btnSalvar.isEnabled = true
                binding.btnSalvar.text = "Salvar"
                Toast.makeText(requireContext(), "Erro ao salvar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(equipeId: String, projetoId: String, onMembrosAtualizados: () -> Unit) =
            GerenciarMembrosBottomSheet(equipeId, projetoId, onMembrosAtualizados)
    }
}