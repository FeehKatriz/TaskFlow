// file: com/example/taskflow/fragments/EquipeFragment.kt
package com.example.taskflow.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taskflow.R
import com.example.taskflow.adapters.MembroAdapter
import com.example.taskflow.adapters.ProjetosEquipeAdapter
import com.example.taskflow.databinding.FragmentEquipeBinding

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class EquipeFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null

    private val binding by lazy {
        FragmentEquipeBinding.inflate(layoutInflater)
    }

    // ProjetosAdapter dispara a navegação via NavController
    private val projetosAdapter by lazy {
        ProjetosEquipeAdapter {
            // aqui usamos a action definida no nav-graph
            findNavController().navigate(
                R.id.action_equipeFragment_to_projetoTarefasFragment
            )
        }
    }
    private val membrosAdapter by lazy { MembroAdapter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
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

        // adapter inicial
        binding.rvProjetosEquipe.adapter = when (binding.toggleGroup.checkedButtonId) {
            R.id.btnMembros  -> membrosAdapter
            else             -> projetosAdapter
        }

        // troca de adapter pelo toggle
        binding.toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            binding.rvProjetosEquipe.adapter = when (checkedId) {
                R.id.btnProjetos -> projetosAdapter
                R.id.btnMembros  -> membrosAdapter
                else             -> projetosAdapter
            }
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
