package com.kareem.nexus.platform.reminder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.kareem.nexus.core.model.OpenLoop
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    fun canNotify(context: Context): Boolean {
        val permission = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return permission && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun defaultTriggerAt(loop: OpenLoop, now: Long = System.currentTimeMillis()): Long {
        loop.dueAt?.takeIf { it > now + 15 * 60_000L }?.let { due ->
            return (due - 60 * 60_000L).coerceAtLeast(now + 30 * 60_000L)
        }
        val zone = ZoneId.systemDefault()
        val current = Instant.ofEpochMilli(now).atZone(zone)
        var tomorrowMorning = current.toLocalDate().plusDays(1).atTime(LocalTime.of(9, 0)).atZone(zone)
        if (!tomorrowMorning.isAfter(current)) tomorrowMorning = tomorrowMorning.plusDays(1)
        return tomorrowMorning.toInstant().toEpochMilli()
    }

    fun schedule(context: Context, loop: OpenLoop, triggerAt: Long = defaultTriggerAt(loop)): Boolean {
        if (!canNotify(context)) return false
        val delay = (triggerAt - System.currentTimeMillis()).coerceAtLeast(1_000L)
        val data = Data.Builder()
            .putString(ReminderWorker.KEY_LOOP_ID, loop.id)
            .putString(ReminderWorker.KEY_TITLE, loop.title)
            .putString(ReminderWorker.KEY_DETAIL, loop.detail)
            .build()
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "nexus-reminder-${loop.id}",
            ExistingWorkPolicy.REPLACE,
            request,
        )
        return true
    }
}
