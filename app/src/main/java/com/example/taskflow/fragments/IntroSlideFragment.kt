package com.example.taskflow.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.taskflow.R

class IntroSlideFragment : Fragment() {

    companion object {
        fun newInstance(imageRes: Int, text: String) = IntroSlideFragment().apply {
            arguments = Bundle().apply {
                putInt("image", imageRes)
                putString("text", text)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_intro_slide, container, false)
        val imageView: ImageView = view.findViewById(R.id.imgSlide)
        val textView: TextView = view.findViewById(R.id.textSlide)

        imageView.setImageResource(requireArguments().getInt("image"))
        textView.text = requireArguments().getString("text")

        return view
    }
}