package com.example.respira.util

import android.content.Context
import android.content.Intent
import android.net.Uri

object Sms {
    /**
     * Abre o app de mensagens já com o número e o texto preenchidos.
     * O usuário confirma e toca em enviar — por isso não precisa da permissão
     * perigosa SEND_SMS (é o mesmo espírito do Dialer.dial com ACTION_DIAL).
     */
    fun compose(context: Context, phone: String, message: String) {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone")).apply {
            putExtra("sms_body", message)
        }
        context.startActivity(intent)
    }
}
