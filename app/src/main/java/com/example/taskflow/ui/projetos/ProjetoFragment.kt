package com.example.taskflow.ui.projetos

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taskflow.R
import com.example.taskflow.adapters.EquipesProjetoAdapter
import com.example.taskflow.adapters.MembroAdapter
import com.example.taskflow.databinding.FragmentProjetoBinding
//import com.example.taskflow.fragments.ARG_PARAM1
//import com.example.taskflow.fragments.ARG_PARAM2
import com.example.taskflow.data.model.Equipe
import com.example.taskflow.ui.equipes.criar.CriarEquipeActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.random.Random

class ProjetoFragment : Fragment() {
    private var param1: String? = null // Este será o ID do projeto
    private var param2: String? = null

    // Variável para armazenar se o usuário é criador do projeto
    private var isCreator: Boolean = false

    private val binding by lazy {
        FragmentProjetoBinding.inflate(layoutInflater)
    }

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val equipesAdapter by lazy {
        EquipesProjetoAdapter { equipe ->
            // Navegar para a tela de tarefas passando o ID da equipe
            val bundle = Bundle().apply {
                putString("equipeId", equipe.id)
                putString("equipeNome", equipe.nome)
            }
            findNavController().navigate(
                R.id.action_projetoFragment_to_equipeTarefasFragment,
                bundle
            )
        }
    }

    private val membrosAdapter by lazy {
        MembroAdapter(
            projetoId = param1 ?: "",
            onMembroRemovido = {
                // Recarregar membros quando alguém for removido
                carregarMembros()

                // Se o usuário atual foi removido, voltar para a tela anterior
                verificarSeUsuarioAindaEstaNoProjeto()
            }
        )
    }

    // Launcher para criar equipe com callback de resultado
    private val criarEquipeLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Equipe foi criada com sucesso, recarregar lista
            carregarEquipes()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1) // ID do projeto
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = binding.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configurar RecyclerView
        binding.rvProjetosEquipe.layoutManager = LinearLayoutManager(requireContext())

        // Carregar informações do projeto
        carregarInfoProjeto()

        // Configurar FAB para criar equipe
        binding.fabCriarProjeto?.setOnClickListener {
            val intent = Intent(requireContext(), CriarEquipeActivity::class.java)
            intent.putExtra("projetoId", param1)
            criarEquipeLauncher.launch(intent)
        }

        // Configurar botão de copiar código
        binding.btnCopiarCodigo.setOnClickListener {
            val codigoTexto = binding.tvCodigoEquipe.text.toString()
            val codigo = codigoTexto.substringAfter("Código: ").trim()
            if (codigo.isNotEmpty()) {
                copiarCodigoParaClipboard(codigo)
            }
        }

        // Configurar botão de atualizar código
        binding.btnAtualizarCodigo.setOnClickListener {
            if (isCreator) {
                confirmarGerarNovoCodigo()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Apenas o criador do projeto pode gerar um novo código",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Configurar adapter inicial baseado no toggle selecionado
        binding.rvProjetosEquipe.adapter = when (binding.toggleGroup.checkedButtonId) {
            R.id.btnMembros -> {
                binding.fabCriarProjeto?.visibility = View.GONE
                carregarMembros()
                membrosAdapter
            }
            else -> {
                binding.fabCriarProjeto?.visibility = View.VISIBLE
                carregarEquipes()
                equipesAdapter
            }
        }

        // Configurar troca de adapter pelo toggle
        binding.toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            binding.rvProjetosEquipe.adapter = when (checkedId) {
                R.id.btnProjetos -> {
                    binding.fabCriarProjeto?.visibility = View.VISIBLE
                    carregarEquipes()
                    equipesAdapter
                }
                R.id.btnMembros -> {
                    binding.fabCriarProjeto?.visibility = View.GONE
                    carregarMembros()
                    membrosAdapter
                }
                else -> equipesAdapter
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Recarregar dados quando voltar para o fragment
        when (binding.toggleGroup.checkedButtonId) {
            R.id.btnProjetos -> carregarEquipes()
            R.id.btnMembros -> carregarMembros()
        }
    }

    private fun verificarSeUsuarioAindaEstaNoProjeto() {
        val projetoId = param1 ?: return
        val usuarioAtualId = auth.currentUser?.uid ?: return

        firestore.collection("projetos")
            .document(projetoId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val membrosIds = document.get("membros") as? List<String> ?: emptyList()

                    // Se o usuário atual não está mais na lista de membros, voltar
                    if (!membrosIds.contains(usuarioAtualId)) {
                        Toast.makeText(
                            requireContext(),
                            "Você foi removido deste projeto",
                            Toast.LENGTH_SHORT
                        ).show()

                        findNavController().popBackStack()
                    }
                }
            }
    }

    private fun carregarInfoProjeto() {
        val projetoId = param1 ?: return

        firestore.collection("projetos")
            .document(projetoId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val nomeProjeto = document.getString("nome") ?: "Projeto"
                    val codigoProjeto = document.getString("codigo") ?: ""
                    val criadorId = document.getString("criador") ?: ""
                    val usuarioAtualId = auth.currentUser?.uid ?: ""

                    // Verificar se o usuário atual é o criador
                    isCreator = criadorId == usuarioAtualId

                    // Atualizar nome do projeto
                    binding.textView15.text = nomeProjeto

                    // Mostrar código do projeto se existir
                    if (codigoProjeto.isNotEmpty()) {
                        binding.layoutCodigoEquipe.visibility = View.VISIBLE
                        binding.tvCodigoEquipe.text = "Código: $codigoProjeto"

                        // Mostrar/ocultar botão de atualizar baseado na permissão
                        binding.btnAtualizarCodigo.visibility = if (isCreator) View.VISIBLE else View.GONE
                    } else {
                        binding.layoutCodigoEquipe.visibility = View.GONE
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Erro ao carregar projeto: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun mostrarOpcoesCodigoProjeto(codigoAtual: String, isCreator: Boolean) {
        val opcoes = if (isCreator) {
            arrayOf("Copiar código", "Gerar novo código")
        } else {
            arrayOf("Copiar código")
        }

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Opções do código do projeto")
        builder.setItems(opcoes) { _, index ->
            when (index) {
                0 -> copiarCodigoParaClipboard(codigoAtual)
                1 -> if (isCreator) confirmarGerarNovoCodigo()
            }
        }
        builder.show()
    }

    private fun confirmarGerarNovoCodigo() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Confirmar alteração")
        builder.setMessage("Tem certeza que deseja gerar um novo código para o projeto?\n\nO código atual ficará inválido e você precisará compartilhar o novo código com os membros.")
        builder.setPositiveButton("Sim, gerar novo") { _, _ ->
            gerarNovoCodigo()
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun gerarNovoCodigo() {
        val projetoId = param1 ?: return

        // Mostrar loading
        Toast.makeText(requireContext(), "Gerando novo código...", Toast.LENGTH_SHORT).show()

        // Desabilitar botão temporariamente
        binding.btnAtualizarCodigo.isEnabled = false

        gerarCodigoUnico { novoCodigo ->
            if (novoCodigo != null) {
                // Atualizar no Firestore
                firestore.collection("projetos")
                    .document(projetoId)
                    .update("codigo", novoCodigo)
                    .addOnSuccessListener {
                        Toast.makeText(
                            requireContext(),
                            "Novo código gerado!\nCódigo: $novoCodigo",
                            Toast.LENGTH_LONG
                        ).show()

                        // Reabilitar botão
                        binding.btnAtualizarCodigo.isEnabled = true

                        // Atualizar exibição do código
                        binding.tvCodigoEquipe.text = "Código: $novoCodigo"
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            requireContext(),
                            "Erro ao atualizar código: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Reabilitar botão
                        binding.btnAtualizarCodigo.isEnabled = true
                    }
            } else {
                Toast.makeText(
                    requireContext(),
                    "Erro ao gerar novo código. Tente novamente.",
                    Toast.LENGTH_SHORT
                ).show()

                // Reabilitar botão
                binding.btnAtualizarCodigo.isEnabled = true
            }
        }
    }

    private fun gerarCodigoProjeto(): String {
        // Gera um código de 10 caracteres alfanuméricos
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..10)
            .map { chars[Random.Default.nextInt(chars.length)] }
            .joinToString("")
    }

    private fun verificarCodigoUnico(codigo: String, callback: (Boolean) -> Unit) {
        firestore.collection("projetos")
            .whereEqualTo("codigo", codigo)
            .get()
            .addOnSuccessListener { documents ->
                callback(documents.isEmpty)
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    private fun gerarCodigoUnico(callback: (String?) -> Unit) {
        val codigo = gerarCodigoProjeto()

        verificarCodigoUnico(codigo) { isUnico ->
            if (isUnico) {
                callback(codigo)
            } else {
                // Código já existe, tentar novamente
                gerarCodigoUnico(callback)
            }
        }
    }

    private fun copiarCodigoParaClipboard(codigo: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Código do Projeto", codigo)
        clipboard.setPrimaryClip(clip)

        Toast.makeText(
            requireContext(),
            "Código '$codigo' copiado para área de transferência!",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun carregarEquipes() {
        val projetoId = param1 ?: return

        firestore.collection("equipes")
            .whereEqualTo("projetoId", projetoId)
            .get()
            .addOnSuccessListener { equipesSnapshot ->
                val equipes = equipesSnapshot.documents.mapNotNull { equipeDoc ->
                    equipeDoc.toObject(Equipe::class.java)?.copy(
                        id = equipeDoc.id
                    )
                }
                // Atualizar lista de equipes
                equipesAdapter.atualizarEquipes(equipes)
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Erro ao carregar equipes: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun carregarMembros() {
        val projetoId = param1 ?: return
        val usuarioAtualId = auth.currentUser?.uid ?: return

        Log.d("ProjetoFragment", "Carregando membros para projeto: $projetoId")
        Log.d("ProjetoFragment", "ID do usuário atual: $usuarioAtualId")

        firestore.collection("projetos")
            .document(projetoId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val membrosIds = document.get("membros") as? List<String> ?: emptyList()
                    val criadorId = document.getString("criador") // ID do criador do projeto

                    Log.d("ProjetoFragment", "IDs dos membros encontrados: $membrosIds")
                    Log.d("ProjetoFragment", "ID do criador: $criadorId")

                    if (membrosIds.isNotEmpty()) {
                        // Buscar cada documento individualmente pelo ID
                        val membros = mutableListOf<Map<String, String>>()
                        var processedCount = 0

                        membrosIds.forEach { userId ->
                            firestore.collection("usuarios")
                                .document(userId) // Buscar diretamente pelo ID do documento
                                .get()
                                .addOnSuccessListener { userDoc ->
                                    processedCount++

                                    if (userDoc.exists()) {
                                        // Determinar o tipo do membro
                                        val tipoMembro = if (userId == criadorId) "Criador" else "Membro"

                                        val nomeUsuario = userDoc.getString("nome") ?: "Usuário"
                                        // Se for o usuário atual, mostrar "Você" ao invés do nome
                                        val nomeExibir = if (userId == usuarioAtualId) "Você" else nomeUsuario

                                        val membro = mapOf(
                                            "uid" to userDoc.id, // Para puxar foto do Storage
                                            "id" to userDoc.id,
                                            "nome" to nomeExibir,
                                            "email" to (userDoc.getString("email") ?: ""),
                                            "tipo" to tipoMembro
                                        )
                                        membros.add(membro)
                                        Log.d("ProjetoFragment", "Membro encontrado: $nomeExibir - $tipoMembro")
                                    } else {
                                        Log.w("ProjetoFragment", "Documento de usuário não existe: $userId")
                                    }

                                    // Quando todos os documentos foram processados
                                    if (processedCount == membrosIds.size) {
                                        // Ordenar lista: usuário atual primeiro, depois criador, depois membros
                                        val membrosOrdenados = membros.sortedWith(compareBy<Map<String, String>> { membro ->
                                            when {
                                                membro["nome"] == "Você" -> 0
                                                membro["tipo"] == "Criador" -> 1
                                                else -> 2
                                            }
                                        }.thenBy { it["nome"] })

                                        Log.d("ProjetoFragment", "Total de membros carregados: ${membrosOrdenados.size}")
                                        membrosAdapter.atualizarMembros(membrosOrdenados)
                                    }
                                }
                                .addOnFailureListener { e ->
                                    processedCount++
                                    Log.e("ProjetoFragment", "Erro ao buscar usuário $userId: ${e.message}")

                                    // Mesmo com erro, verificar se terminou de processar todos
                                    if (processedCount == membrosIds.size) {
                                        val membrosOrdenados = membros.sortedWith(compareBy<Map<String, String>> { membro ->
                                            when {
                                                membro["nome"] == "Você" -> 0
                                                membro["tipo"] == "Criador" -> 1
                                                else -> 2
                                            }
                                        }.thenBy { it["nome"] })
                                        membrosAdapter.atualizarMembros(membrosOrdenados)
                                    }
                                }
                        }
                    } else {
                        Log.d("ProjetoFragment", "Nenhum membro encontrado no projeto")
                        membrosAdapter.atualizarMembros(emptyList())
                    }
                } else {
                    Log.w("ProjetoFragment", "Documento do projeto não existe: $projetoId")
                    membrosAdapter.atualizarMembros(emptyList())
                }
            }
            .addOnFailureListener { e ->
                Log.e("ProjetoFragment", "Erro ao carregar projeto: ${e.message}")
                Toast.makeText(
                    requireContext(),
                    "Erro ao carregar projeto: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ProjetoFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"