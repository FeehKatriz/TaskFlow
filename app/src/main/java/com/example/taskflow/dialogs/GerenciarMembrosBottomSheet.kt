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
                // RN07: Validar limite máximo antes de adicionar
                if (membrosEquipe.size >= MAX_MEMBROS) {
                    Toast.makeText(
                        requireContext(),
                        "Limite máximo de $MAX_MEMBROS membros atingido!",
                        Toast.LENGTH_SHORT
                    ).show()
                    // Não adicionar o membro e reverter a seleção no adapter
                    adapter.reverterSelecao(memberId)
                    return@SelecionarMembrosProjetoAdapter
                }
                membrosEquipe.add(memberId)
            } else {
                membrosEquipe.remove(memberId)
            }
            atualizarContadorSelecionados()
            atualizarEstadoBotoes()
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
                                    atualizarEstadoBotoes()
                                    binding.progressBar.visibility = View.GONE
                                }
                            }
                            .addOnFailureListener {
                                processedCount++
                                if (processedCount == membrosIds.size) {
                                    adapter.atualizarMembros(membrosProjeto, membrosEquipe)
                                    atualizarContadorSelecionados()
                                    atualizarEstadoBotoes()
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
        val textoContador = "${membrosEquipe.size} de ${membrosProjeto.size} selecionados"
        val textoLimite = " (máx. $MAX_MEMBROS)"
        binding.tvContador.text = textoContador + textoLimite
    }

    /**
     * RN07: Atualiza o estado dos botões com base na quantidade de membros
     */
    private fun atualizarEstadoBotoes() {
        val quantidadeSelecionados = membrosEquipe.size

        // Desabilitar "Selecionar Todos" se já atingiu o limite
        binding.btnSelecionarTodos.isEnabled = quantidadeSelecionados < MAX_MEMBROS

        // Desabilitar "Salvar" se não tiver membros suficientes
        binding.btnSalvar.isEnabled = quantidadeSelecionados >= MIN_MEMBROS

        // Atualizar alpha para feedback visual
        binding.btnSelecionarTodos.alpha = if (quantidadeSelecionados < MAX_MEMBROS) 1.0f else 0.5f
        binding.btnSalvar.alpha = if (quantidadeSelecionados >= MIN_MEMBROS) 1.0f else 0.5f
    }

    /**
     * RN07: Selecionar todos limitando ao máximo de membros
     */
    private fun selecionarTodos() {
        membrosEquipe.clear()

        // Adicionar no máximo MAX_MEMBROS
        var contador = 0
        for (membro in membrosProjeto) {
            if (contador >= MAX_MEMBROS) break

            membro["uid"]?.let {
                membrosEquipe.add(it)
                contador++
            }
        }

        adapter.atualizarSelecao(membrosEquipe)
        atualizarContadorSelecionados()
        atualizarEstadoBotoes()

        // Informar se nem todos foram selecionados
        if (membrosProjeto.size > MAX_MEMBROS) {
            Toast.makeText(
                requireContext(),
                "Apenas os primeiros $MAX_MEMBROS membros foram selecionados (limite máximo)",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun limparSelecao() {
        membrosEquipe.clear()
        adapter.atualizarSelecao(membrosEquipe)
        atualizarContadorSelecionados()
        atualizarEstadoBotoes()
    }

    /**
     * RN07: Salvar com validação de quantidade de membros
     */
    private fun salvarMembros() {
        val quantidadeMembros = membrosEquipe.size

        // Validar mínimo de membros
        if (quantidadeMembros < MIN_MEMBROS) {
            Toast.makeText(
                requireContext(),
                "A equipe deve ter pelo menos $MIN_MEMBROS membro!",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // Validar máximo de membros
        if (quantidadeMembros > MAX_MEMBROS) {
            Toast.makeText(
                requireContext(),
                "A equipe pode ter no máximo $MAX_MEMBROS membros!",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // Tudo ok, salvar
        binding.btnSalvar.isEnabled = false
        binding.btnSalvar.text = "Salvando..."

        firestore.collection("equipes")
            .document(equipeId)
            .update("membros", membrosEquipe.toList())
            .addOnSuccessListener {
                Toast.makeText(
                    requireContext(),
                    "Membros atualizados com sucesso! ($quantidadeMembros membro${if (quantidadeMembros > 1) "s" else ""})",
                    Toast.LENGTH_SHORT
                ).show()
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
        // RN07 - Constantes de validação
        const val MIN_MEMBROS = 1
        const val MAX_MEMBROS = 5

        fun newInstance(equipeId: String, projetoId: String, onMembrosAtualizados: () -> Unit) =
            GerenciarMembrosBottomSheet(equipeId, projetoId, onMembrosAtualizados)
    }
}