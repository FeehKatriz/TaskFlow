package com.example.taskflow.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

import com.example.taskflow.R

/**
 * A simple [androidx.fragment.app.Fragment] subclass.
 * Use the [TarefaFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class TarefaFragment : Fragment() {


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_tarefas, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }


}