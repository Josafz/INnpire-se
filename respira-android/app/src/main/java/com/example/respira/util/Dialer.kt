package com.example.respira.util

import android.content.Context
import android.content.Intent
import android.net.Uri

object Dialer {
    /** Abre o discador com o número preenchido. Não liga sozinho, então não precisa de permissão. */
    fun dial(context: Context, number: String) {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
    }
}
