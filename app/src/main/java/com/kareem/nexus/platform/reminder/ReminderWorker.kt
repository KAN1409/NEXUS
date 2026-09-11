package com.kareem.nexus.platform.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.kareem.nexus.MainActivity
import com.kareem.nexus.R

class ReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : Worker(appContext, params) {
    override fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE)?.takeIf { it.isNotBlank() } ?: "NEXUS reminder"
        val detail = inputData.getString(KEY_DETAIL).orEmpty()
        val loopId = inputData.getString(KEY_LOOP_ID).orEmpty()
        ensureChannel()

        val intent = Intent(applicationContext, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(
            applicationContext,
            loopId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(detail.take(140))
            .setStyle(NotificationCompat.BigTextStyle().bigText(detail))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        return runCatching {
            NotificationManagerCompat.from(applicationContext).notify(loopId.hashCode(), notification)
            Result.success()
        }.getOrElse { Result.failure() }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "NEXUS reminders",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Reminders created from unresolved NEXUS open loops"
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val KEY_LOOP_ID = "loop_id"
        const val KEY_TITLE = "title"
        const val KEY_DETAIL = "detail"
        private const val CHANNEL_ID = "nexus_open_loop_reminders"
    }
}
