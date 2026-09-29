package com.example.respira.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ChatDao {

    @Insert
    suspend fun insert(message: ChatMessage): Long

    // Conversa inteira, da mais antiga para a mais nova (id desempata mensagens do mesmo milissegundo)
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC, id ASC")
    suspend fun getAll(): List<ChatMessage>

    // As últimas N mensagens (da mais nova para a mais antiga): é o "contexto" enviado à IA
    @Query("SELECT * FROM chat_messages ORDER BY id DESC LIMIT :limit")
    suspend fun getLast(limit: Int): List<ChatMessage>

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAll()
}
