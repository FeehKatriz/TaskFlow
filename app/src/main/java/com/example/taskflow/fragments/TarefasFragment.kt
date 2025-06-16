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

class TarefasFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_tarefas, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /*// 1) Em andamento
        view.findViewById<RecyclerView>(R.id.rvTarefasAndamento).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = TarefasAdapter(
                itemCount = 3,                               // tools:itemCount="3"
                layoutRes   = R.layout.item_tarefa_andamento // seu XML de item
            ) {
                // TODO: ação ao clicar em tarefa em andamento
            }
        }

        // 2) Finalizadas
        view.findViewById<RecyclerView>(R.id.rvTarefasFinalizadas).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = TarefasAdapter(
                itemCount = 2,
                layoutRes   = R.layout.item_tarefa_finalizada
            ) {
                // TODO: ação ao clicar em tarefa finalizada
            }
        }

        // 3) A começar
        view.findViewById<RecyclerView>(R.id.rvTarefasAComecar).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = TarefasAdapter(
                itemCount = 4,
                layoutRes   = R.layout.item_tarefa_afazer
            ) {
                // TODO: ação ao clicar em tarefa a começar
            }
        }*/
    }
}
