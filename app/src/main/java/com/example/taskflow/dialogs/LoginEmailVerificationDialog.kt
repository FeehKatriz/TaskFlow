package com.example.taskflow.ui.entrar

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.Window
import com.example.taskflow.databinding.DialogLoginVerificacaoEmailBinding

class LoginEmailVerificationDialog(
    private val context: Context,
    private val email: String,
    private val onReenviar: () -> Unit,
    private val onFechar: () -> Unit
) {

    private lateinit var binding: DialogLoginVerificacaoEmailBinding
    private lateinit var dialog: Dialog

    fun show() {
        dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        binding = DialogLoginVerificacaoEmailBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)

        configurarDialog()
        dialog.show()
    }

    private fun configurarDialog() {
        binding.txtEmailEnviado.text = "Você ainda não verificou seu email:\n$email"

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