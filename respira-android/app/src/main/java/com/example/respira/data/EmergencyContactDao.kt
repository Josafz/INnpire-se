package com.example.respira.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface EmergencyContactDao {

    // REPLACE + índice único em "position": salvar de novo na mesma posição troca o contato,
    // em vez de criar um segundo registro para o mesmo slot.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(contact: EmergencyContact)

    @Query("SELECT * FROM emergency_contacts ORDER BY position ASC")
    suspend fun getAll(): List<EmergencyContact>

    @Query("DELETE FROM emergency_contacts WHERE position = :position")
    suspend fun deleteByPosition(position: Int)
}
