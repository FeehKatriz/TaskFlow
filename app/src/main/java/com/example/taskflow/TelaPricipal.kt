package com.example.taskflow

import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions

class TelaPricipal : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_tela_pricipal)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.teste)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets

        }
        val imageView = findViewById<ImageView>(R.id.imageView16)

        Glide.with(this)
            .load(R.drawable.usertype) // ou uma URL se preferir
            .apply(RequestOptions.circleCropTransform()) // <-- imagem redonda
            .into(imageView)

    }

}