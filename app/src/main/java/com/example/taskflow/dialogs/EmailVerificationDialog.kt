package com.example.taskflow.ui.entrar

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.Window
import com.example.taskflow.databinding.DialogVerificacaoEmailBinding

class EmailVerificationDialog(
    private val context: Context,
    private val email: String,
    private val onReenviar: () -> Unit,
    private val onFechar: () -> Unit
) {

    private lateinit var binding: DialogVerificacaoEmailBinding
    private lateinit var dialog: Dialog

    fun show() {
        dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        binding = DialogVerificacaoEmailBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)

        configurarDialog()
        dialog.show()
    }

    private fun configurarDialog() {
        binding.txtEmailEnviado.text = "Enviamos um link de verificação para:\n$email"

        binding.btnEntendi.setOnClickListener {
            dialog.dismiss()
            onFechar()
        }

        binding.btnReenviarEmail.setOnClickListener {
            onReenviar()
        }
    }

    fun dismiss() {
        if (::dialog.isInitialized && dialog.isShowing) {
            dialog.dismiss()
        }
    }
}