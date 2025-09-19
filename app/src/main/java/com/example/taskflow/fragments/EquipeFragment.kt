package com.example.taskflow.fragments

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
import com.example.taskflow.adapters.MembroAdapter
import com.example.taskflow.adapters.ProjetosEquipeAdapter
import com.example.taskflow.databinding.FragmentEquipeBinding
import com.example.taskflow.models.Projeto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.taskflow.CriarNovoProjeto
import kotlin.random.Random

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class EquipeFragment : Fragment() {
    private var param1: String? = null // Este será o ID da equipe
    private var param2: String? = null

    // Variável para armazenar se o usuário é criador da equipe
    private var isCreator: Boolean = false

    private val binding by lazy {
        FragmentEquipeBinding.inflate(layoutInflater)
    }

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val projetosAdapter by lazy {
        ProjetosEquipeAdapter { projeto ->
            // Navegar para a tela de tarefas passando o ID do projeto
            val bundle = Bundle().apply {
                putString("projetoId", projeto.id)
                putString("projetoNome", projeto.nome)
            }
            findNavController().navigate(
                R.id.action_equipeFragment_to_projetoTarefasFragment,
                bundle
            )
        }
    }

    private val membrosAdapter by lazy {
        MembroAdapter(
            equipeId = param1 ?: "",
            onMembroRemovido = {
                // Recarregar membros quando alguém for removido
                carregarMembros()

                // Se o usuário atual foi removido, voltar para a tela anterior
                verificarSeUsuarioAindaEstaNaEquipe()
            }
        )
    }

    // Launcher para criar projeto com callback de resultado
    private val criarProjetoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            // Projeto foi criado com sucesso, recarregar lista
            carregarProjetos()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1) // ID da equipe
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = binding.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvProjetosEquipe.layoutManager = LinearLayoutManager(requireContext())

        // Carregar nome da equipe e código
        carregarInfoEquipe()

        // Configurar FAB para criar projeto - USAR LAUNCHER
        binding.fabCriarProjeto?.setOnClickListener {
            val intent = Intent(requireContext(), CriarNovoProjeto::class.java)
            intent.putExtra("equipeId", param1) // Passar o ID da equipe
            criarProjetoLauncher.launch(intent) // Em vez de startActivity
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
                    "Apenas o criador da equipe pode gerar um novo código",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // adapter inicial
        binding.rvProjetosEquipe.adapter = when (binding.toggleGroup.checkedButtonId) {
            R.id.btnMembros -> {
                binding.fabCriarProjeto?.visibility = View.GONE
                carregarMembros()
                membrosAdapter
            }
            else -> {
                binding.fabCriarProjeto?.visibility = View.VISIBLE
                carregarProjetos()
                projetosAdapter
            }
        }

        // troca de adapter pelo toggle
        binding.toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            binding.rvProjetosEquipe.adapter = when (checkedId) {
                R.id.btnProjetos -> {
                    binding.fabCriarProjeto?.visibility = View.VISIBLE
                    carregarProjetos()
                    projetosAdapter
                }
                R.id.btnMembros -> {
                    binding.fabCriarProjeto?.visibility = View.GONE
                    carregarMembros()
                    membrosAdapter
                }
                else -> projetosAdapter
            }
        }

        // Configurar botão voltar
        binding.button15.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun onResume() {
        super.onResume()
        // Recarregar dados quando voltar para o fragment
        when (binding.toggleGroup.checkedButtonId) {
            R.id.btnProjetos -> carregarProjetos()
            R.id.btnMembros -> carregarMembros()
        }
    }

    private fun verificarSeUsuarioAindaEstaNaEquipe() {
        val equipeId = param1 ?: return
        val usuarioAtualId = auth.currentUser?.uid ?: return

        firestore.collection("equipes")
            .document(equipeId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val membrosIds = document.get("membros") as? List<String> ?: emptyList()

                    // Se o usuário atual não está mais na lista de membros, voltar
                    if (!membrosIds.contains(usuarioAtualId)) {
                        Toast.makeText(
                            requireContext(),
                            "Você foi removido desta equipe",
                            Toast.LENGTH_SHORT
                        ).show()

                        findNavController().popBackStack()
                    }
                }
            }
    }

    private fun carregarInfoEquipe() {
        val equipeId = param1 ?: return

        firestore.collection("equipes")
            .document(equipeId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val nomeEquipe = document.getString("nome") ?: "Equipe"
                    val codigoEquipe = document.getString("codigo") ?: ""
                    val criadorId = document.getString("criador") ?: ""
                    val usuarioAtualId = auth.currentUser?.uid ?: ""

                    // Verificar se o usuário atual é o criador
                    isCreator = criadorId == usuarioAtualId

                    // Atualizar nome da equipe
                    binding.textView15.text = nomeEquipe

                    // Mostrar código da equipe se existir
                    if (codigoEquipe.isNotEmpty()) {
                        binding.layoutCodigoEquipe.visibility = View.VISIBLE
                        binding.tvCodigoEquipe.text = "Código: $codigoEquipe"

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
                    "Erro ao carregar equipe: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun mostrarOpcoesCodigoEquipe(codigoAtual: String, isCreator: Boolean) {
        val opcoes = if (isCreator) {
            arrayOf("Copiar código", "Gerar novo código")
        } else {
            arrayOf("Copiar código")
        }

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Opções do código da equipe")
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
        builder.setMessage("Tem certeza que deseja gerar um novo código para a equipe?\n\nO código atual ficará inválido e você precisará compartilhar o novo código com os membros.")
        builder.setPositiveButton("Sim, gerar novo") { _, _ ->
            gerarNovoCodigo()
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun gerarNovoCodigo() {
        val equipeId = param1 ?: return

        // Mostrar loading
        Toast.makeText(requireContext(), "Gerando novo código...", Toast.LENGTH_SHORT).show()

        // Desabilitar botão temporariamente
        binding.btnAtualizarCodigo.isEnabled = false

        gerarCodigoUnico { novoCodigo ->
            if (novoCodigo != null) {
                // Atualizar no Firestore
                firestore.collection("equipes")
                    .document(equipeId)
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

    private fun gerarCodigoEquipe(): String {
        // Gera um código de 10 caracteres alfanuméricos
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..10)
            .map { chars[Random.nextInt(chars.length)] }
            .joinToString("")
    }

    private fun verificarCodigoUnico(codigo: String, callback: (Boolean) -> Unit) {
        firestore.collection("equipes")
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
        val codigo = gerarCodigoEquipe()

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
        val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Código da Equipe", codigo)
        clipboard.setPrimaryClip(clip)

        Toast.makeText(
            requireContext(),
            "Código '$codigo' copiado para área de transferência!",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun carregarProjetos() {
        val equipeId = param1 ?: return

        firestore.collection("projetos")
            .whereEqualTo("equipeId", equipeId)
            .get()
            .addOnSuccessListener { projetosSnapshot ->
                val projetos = projetosSnapshot.documents.mapNotNull { projetoDoc ->
                    projetoDoc.toObject(Projeto::class.java)?.copy(
                        id = projetoDoc.id
                    )
                }
                // USAR O MÉTODO DE ATUALIZAÇÃO DO ADAPTER
                projetosAdapter.atualizarProjetos(projetos)
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Erro ao carregar projetos: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun carregarMembros() {
        val equipeId = param1 ?: return
        val usuarioAtualId = auth.currentUser?.uid ?: return

        Log.d("EquipeFragment", "Carregando membros para equipe: $equipeId")
        Log.d("EquipeFragment", "ID do usuário atual: $usuarioAtualId")

        firestore.collection("equipes")
            .document(equipeId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val membrosIds = document.get("membros") as? List<String> ?: emptyList()
                    val criadorId = document.getString("criador") // ID do criador da equipe

                    Log.d("EquipeFragment", "IDs dos membros encontrados: $membrosIds")
                    Log.d("EquipeFragment", "ID do criador: $criadorId")

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
                                            "uid" to userDoc.id, // <-- adicionado para puxar foto do Storage
                                            "id" to userDoc.id,
                                            "nome" to nomeExibir,
                                            "email" to (userDoc.getString("email") ?: ""),
                                            "tipo" to tipoMembro
                                        )
                                        membros.add(membro)
                                        Log.d("EquipeFragment", "Membro encontrado: $nomeExibir - $tipoMembro")
                                    } else {
                                        Log.w("EquipeFragment", "Documento de usuário não existe: $userId")
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

                                        Log.d("EquipeFragment", "Total de membros carregados: ${membrosOrdenados.size}")
                                        membrosAdapter.atualizarMembros(membrosOrdenados)
                                    }
                                }
                                .addOnFailureListener { e ->
                                    processedCount++
                                    Log.e("EquipeFragment", "Erro ao buscar usuário $userId: ${e.message}")

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
                        Log.d("EquipeFragment", "Nenhum membro encontrado na equipe")
                        membrosAdapter.atualizarMembros(emptyList())
                    }
                } else {
                    Log.w("EquipeFragment", "Documento da equipe não existe: $equipeId")
                    membrosAdapter.atualizarMembros(emptyList())
                }
            }
            .addOnFailureListener { e ->
                Log.e("EquipeFragment", "Erro ao carregar equipe: ${e.message}")
                Toast.makeText(
                    requireContext(),
                    "Erro ao carregar equipe: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            EquipeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}