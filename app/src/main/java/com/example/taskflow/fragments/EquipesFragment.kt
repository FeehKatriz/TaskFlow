package com.example.taskflow.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taskflow.R
import com.example.taskflow.adapters.EquipesAdapter
import com.example.taskflow.databinding.FragmentEquipesBinding

class EquipesFragment: Fragment() {

    private val binding by lazy {
        FragmentEquipesBinding.inflate(layoutInflater)
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = binding.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvEquipes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEquipes.adapter = EquipesAdapter {
            findNavController().navigate(
                R.id.action_equipesFragment_to_equipeFragment
            )
        }

    }
}