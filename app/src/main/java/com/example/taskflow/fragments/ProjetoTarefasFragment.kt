// file: com/example/taskflow/fragments/ProjetoTarefasFragment.kt
package com.example.taskflow.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.taskflow.R
import com.example.taskflow.adapters.TarefasAdapter

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class ProjetoTarefasFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_projeto_tarefas, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Em andamento
        view.findViewById<RecyclerView>(R.id.rvTarefasAndamento).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = TarefasAdapter(
                itemCount = 3,
                layoutRes = R.layout.item_tarefa_andamento
            ) {
                // TODO: clique em “Em andamento” (projeto)
            }
        }

        // Finalizadas
        view.findViewById<RecyclerView>(R.id.rvTarefasFinalizadas).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = TarefasAdapter(
                itemCount = 2,
                layoutRes = R.layout.item_tarefa_finalizada
            ) {
                // TODO: clique em “Finalizadas” (projeto)
            }
        }

        // A começar
        view.findViewById<RecyclerView>(R.id.rvTarefasAComecar).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = TarefasAdapter(
                itemCount = 4,
                layoutRes = R.layout.item_tarefa_afazer
            ) {
                // TODO: clique em “A começar” (projeto)
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ProjetoTarefasFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
