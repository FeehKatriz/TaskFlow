package com.example.taskflow.activity

import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.taskflow.R
import com.example.taskflow.fragments.IntroSlideFragment

class IntroAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    private val pages = listOf(
        IntroSlideFragment.newInstance(R.drawable.illustration_first, "Gerenciamento de Tarefas Colaborativas"),
        IntroSlideFragment.newInstance(R.drawable.illustration_first2, "Desenvolvido para ajudar a gerenciar melhor as suas tarefas")
    )

    override fun getItemCount() = pages.size

    override fun createFragment(position: Int) = pages[position]
}
