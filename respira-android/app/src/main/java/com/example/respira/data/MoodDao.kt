package com.example.respira.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MoodDao {

    @Insert
    suspend fun insert(entry: MoodEntry): Long

    @Query("DELETE FROM mood_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM mood_entries")
    suspend fun deleteAll()

    @Query("SELECT * FROM mood_entries ORDER BY timestamp DESC")
    suspend fun getAll(): List<MoodEntry>
}
