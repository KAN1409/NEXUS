package com.kareem.nexus.observe

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.kareem.nexus.core.model.ObservationType
import com.kareem.nexus.domain.repository.NexusRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.*
import org.json.JSONObject

@AndroidEntryPoint
class NexusNotificationListener : NotificationListenerService() {
    @Inject lateinit var repository: NexusRepository
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName || sbn.isOngoing ||
            sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        scope.launch {
            try {
                val extras = sbn.notification.extras
                val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
                val text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
                    ?.takeIf { it.isNotBlank() }
                    ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
                val body = listOf(title, text).filter { it.isNotBlank() }.distinct().joinToString(" — ")
                if (body.isBlank()) return@launch
                repository.captureObservation(ObservationType.NOTIFICATION, body, sbn.packageName,
                    JSONObject().put("key", sbn.key).put("postedAt", sbn.postTime).toString())
                UnderstandingWork.enqueue(applicationContext)
            } catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { Log.w("NEXUS", "Notification capture failed") }
        }
    }
    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}
