package com.example.respira.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Um dos 3 contatos de confiança. `position` (1, 2 ou 3) é o "slot" fixo:
 * o índice único garante que nunca existam dois contatos na mesma posição.
 */
@Entity(
    tableName = "emergency_contacts",
    indices = [Index(value = ["position"], unique = true)]
)
data class EmergencyContact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val position: Int,   // 1, 2 ou 3
    val name: String,
    val phone: String
)
