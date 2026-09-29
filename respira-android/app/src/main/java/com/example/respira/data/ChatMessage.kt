package com.example.respira.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Uma mensagem da conversa. Tanto as falas do assistente ("bot") quanto
 * as do usuário ("user") ficam guardadas aqui, no próprio aparelho.
 */
@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String,       // ROLE_BOT ou ROLE_USER
    val text: String,
    val timestamp: Long     // System.currentTimeMillis()
) {
    companion object {
        const val ROLE_BOT = "bot"
        const val ROLE_USER = "user"
    }
}
