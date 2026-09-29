package com.example.respira.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Banco de dados local (SQLite via Room). Fica salvo dentro do próprio
 * aparelho, então o app continua funcionando 100% offline.
 *
 * Versão 1: tabela mood_entries.
 * Versão 2: adiciona a tabela chat_messages (a conversa do chat).
 * Versão 3: adiciona a tabela emergency_contacts (os 3 contatos de confiança).
 */
@Database(
    entities = [MoodEntry::class, ChatMessage::class, EmergencyContact::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun moodDao(): MoodDao
    abstract fun chatDao(): ChatDao
    abstract fun emergencyContactDao(): EmergencyContactDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migração 1 -> 2: só cria a tabela nova. O histórico de humor já salvo NÃO é apagado.
        // O SQL precisa ser idêntico ao que o Room esperaria para a entidade ChatMessage.
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS chat_messages (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "role TEXT NOT NULL, " +
                        "text TEXT NOT NULL, " +
                        "timestamp INTEGER NOT NULL)"
                )
            }
        }

        // Migração 2 -> 3: cria a tabela dos contatos de confiança, com índice único em
        // "position" (o nome do índice segue exatamente o padrão que o Room gera sozinho).
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS emergency_contacts (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "position INTEGER NOT NULL, " +
                        "name TEXT NOT NULL, " +
                        "phone TEXT NOT NULL)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_emergency_contacts_position " +
                        "ON emergency_contacts (position)"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "respira.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
