package com.example.respira.reminder

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Agenda o lembrete com o WorkManager (funciona offline e sobrevive a reiniciar o celular).
 * Não é um relógio exato: o Android pode adiantar ou atrasar alguns minutos para poupar bateria.
 */
object ReminderScheduler {
    private const val WORK_NAME = "respira_lembrete_diario"

    fun schedule(context: Context, hour: Int, minute: Int) {
        val delay = ReminderTime.initialDelayMillis(ZonedDateTime.now(), hour, minute)
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        // UPDATE: se já havia um lembrete, troca pelo novo horário sem duplicar
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(WORK_NAME)
    }
}
