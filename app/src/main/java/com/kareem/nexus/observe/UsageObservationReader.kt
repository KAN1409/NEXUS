package com.kareem.nexus.observe

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.kareem.nexus.core.model.ObservationType
import com.kareem.nexus.domain.repository.NexusRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageObservationReader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: NexusRepository,
) {
    fun hasAccess(): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java)
        return appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName,
        ) == AppOpsManager.MODE_ALLOWED
    }

    fun settingsIntent(): Intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    suspend fun captureLast24Hours(): Int {
        if (!hasAccess()) return 0
        val manager = context.getSystemService(UsageStatsManager::class.java)
        val end = System.currentTimeMillis()
        val start = end - 24L * 60L * 60L * 1000L
        val rows = manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end).orEmpty()
            .filter { it.totalTimeInForeground > 0 && it.packageName != context.packageName }
            .sortedByDescending { it.totalTimeInForeground }
            .take(20)
        rows.forEach { stat ->
            repository.captureObservation(
                ObservationType.APP_USAGE,
                "${stat.packageName} used for ${stat.totalTimeInForeground / 60000} minutes in the last day",
                stat.packageName,
                "{\"foregroundMs\":${stat.totalTimeInForeground}}",
            )
        }
        return rows.size
    }
}
