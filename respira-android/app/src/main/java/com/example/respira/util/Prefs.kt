package com.example.respira.util

import android.content.Context
import com.example.respira.chat.ChatTree

/**
 * Configurações simples (pouco dado, formato chave/valor) via SharedPreferences.
 * Dado que cresce e precisa de consulta (humor, conversa) fica no Room; aqui só ajustes.
 */
object Prefs {
    private const val FILE = "respira_prefs"

    private fun sp(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    // ---- chat ----
    fun chatNode(ctx: Context): String = sp(ctx).getString("chat_node", ChatTree.ROOT) ?: ChatTree.ROOT
    fun setChatNode(ctx: Context, id: String) = sp(ctx).edit().putString("chat_node", id).apply()

    fun aiConsent(ctx: Context): Boolean = sp(ctx).getBoolean("ai_consent", false)
    fun setAiConsent(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean("ai_consent", v).apply()

    fun aiEnabled(ctx: Context): Boolean = sp(ctx).getBoolean("ai_enabled", false)
    fun setAiEnabled(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean("ai_enabled", v).apply()

    // ---- lembrete diário ----
    fun reminderEnabled(ctx: Context): Boolean = sp(ctx).getBoolean("reminder_enabled", false)
    fun setReminderEnabled(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean("reminder_enabled", v).apply()

    fun reminderHour(ctx: Context): Int = sp(ctx).getInt("reminder_hour", 20)
    fun reminderMinute(ctx: Context): Int = sp(ctx).getInt("reminder_minute", 0)
    fun setReminderTime(ctx: Context, hour: Int, minute: Int) =
        sp(ctx).edit().putInt("reminder_hour", hour).putInt("reminder_minute", minute).apply()
}
