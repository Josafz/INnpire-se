package com.example.respira.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Uma linha do "banco de humor". Cada toque em Bem / Ansiedade / Pânico
 * na tela inicial cria um registro destes.
 */
@Entity(tableName = "mood_entries")
data class MoodEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mood: String,       // "bem" | "ansiedade" | "panico"
    val timestamp: Long     // System.currentTimeMillis()
)
