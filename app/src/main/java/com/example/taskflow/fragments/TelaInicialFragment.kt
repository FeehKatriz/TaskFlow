package com.example.taskflow.fragments

import com.example.taskflow.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class TelaInicialFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, //talvez o erro seja aq
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            R.layout.fragment_telainicial,
            container,
            false
        )
    }
}