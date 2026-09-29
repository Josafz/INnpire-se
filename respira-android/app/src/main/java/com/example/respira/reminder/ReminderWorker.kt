package com.example.respira.reminder

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.respira.util.Prefs

/** Roda em segundo plano uma vez por dia (agendada pelo ReminderScheduler) e mostra a notificação. */
class ReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        if (Prefs.reminderEnabled(applicationContext)) {
            Notifier.showReminder(applicationContext)
        }
        return Result.success()
    }
}
