package com.example.respira.reminder

import java.time.Duration
import java.time.ZonedDateTime

/** Conta pura (sem Android) de "quanto falta até a próxima vez que der HH:MM". */
object ReminderTime {

    fun initialDelayMillis(now: ZonedDateTime, hour: Int, minute: Int): Long {
        var next = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!next.isAfter(now)) {
            next = next.plusDays(1)   // já passou hoje: vale amanhã, no mesmo horário
        }
        return Duration.between(now, next).toMillis()
    }
}
