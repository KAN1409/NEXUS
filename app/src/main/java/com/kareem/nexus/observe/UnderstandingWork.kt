package com.kareem.nexus.observe

import android.content.Context
import androidx.work.*
import com.kareem.nexus.domain.repository.NexusRepository
import dagger.EntryPoint
import dagger.InstallIn
import dagger.hilt.EntryPoints
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException

// Uses the default WorkManager initializer and constructor: safe on a cold process start.
class UnderstandingWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    @EntryPoint @InstallIn(SingletonComponent::class)
    interface Dependencies { fun repository(): NexusRepository }
    override suspend fun doWork(): Result = try {
        EntryPoints.get(applicationContext, Dependencies::class.java).repository().rebuildUnderstanding()
        Result.success()
    } catch (cancel: CancellationException) { throw cancel }
    catch (_: Exception) { if (runAttemptCount < 3) Result.retry() else Result.failure() }
}

object UnderstandingWork {
    fun enqueue(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork("understand-captured-context",
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<UnderstandingWorker>()
                .setInitialDelay(2, TimeUnit.SECONDS).build())
    }
    fun schedule(context: Context) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("resurface-deferred-actions",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<UnderstandingWorker>(1, TimeUnit.HOURS).build())
    }
}
