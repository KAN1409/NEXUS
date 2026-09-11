package com.kareem.nexus.ui

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kareem.nexus.core.NexusWorkTracker
import com.kareem.nexus.core.model.ObservationType
import com.kareem.nexus.domain.repository.NexusRepository
import com.kareem.nexus.ingest.ShareIngestor
import com.kareem.nexus.observe.UsageObservationReader
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class CaptureUiState(
    val message: String? = null,
    val busy: Boolean = false,
    val usageAccess: Boolean = false,
    val notificationAccess: Boolean = false,
    val saveRevision: Int = 0,
    val processedRevision: Int = 0,
)

@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val repository: NexusRepository,
    private val shareIngestor: ShareIngestor,
    private val usageReader: UsageObservationReader,
) : ViewModel() {
    private val _state = MutableStateFlow(CaptureUiState())
    val state = _state.asStateFlow()
    private val gate = Mutex()
    private val pendingOperations = AtomicInteger(0)
    private var lastAutomaticUsageRefresh = 0L

    fun clearMessage() = _state.update { it.copy(message = null) }

    private fun runOperation(block: suspend () -> String?) {
        pendingOperations.incrementAndGet()
        NexusWorkTracker.begin()
        _state.update { it.copy(busy = true) }

        viewModelScope.launch {
            try {
                gate.withLock {
                    try {
                        val message = withContext(Dispatchers.IO) { block() }
                        _state.update { it.copy(message = message) }
                    } catch (cancel: CancellationException) {
                        throw cancel
                    } catch (_: Exception) {
                        _state.update {
                            it.copy(message = "Could not finish this operation. Your saved data is safe; please retry.")
                        }
                    }
                }
            } finally {
                if (pendingOperations.decrementAndGet() == 0) {
                    _state.update { it.copy(busy = false) }
                }
                NexusWorkTracker.end()
            }
        }
    }

    private fun markProcessedSave() {
        _state.update {
            it.copy(
                saveRevision = it.saveRevision + 1,
                processedRevision = it.processedRevision + 1,
            )
        }
    }

    fun captureText(text: String) {
        if (text.isBlank()) return
        runOperation {
            repository.captureObservation(
                if (text.trim().startsWith("https://") || text.trim().startsWith("http://")) {
                    ObservationType.SHARED_LINK
                } else {
                    ObservationType.MANUAL
                },
                text,
                "NEXUS",
            )
            repository.rebuildUnderstanding()
            markProcessedSave()
            "Saved to Memory"
        }
    }

    fun ingestShare(intent: Intent) {
        if (intent.action !in setOf(Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE)) return
        runOperation {
            val count = shareIngestor.ingest(intent)
            if (count > 0) {
                repository.rebuildUnderstanding()
                markProcessedSave()
            }
            if (count > 0) "Saved $count item(s) to Memory" else "This share contains no supported text or images"
        }
    }

    fun refreshAccessState() = _state.update {
        it.copy(
            usageAccess = usageReader.hasAccess(),
            notificationAccess = usageReader.hasNotificationAccess(),
        )
    }

    fun refreshContext() {
        refreshAccessState()
        val now = android.os.SystemClock.elapsedRealtime()
        if (state.value.busy) return
        if (lastAutomaticUsageRefresh != 0L && now - lastAutomaticUsageRefresh < AUTO_USAGE_REFRESH_MS) return
        lastAutomaticUsageRefresh = now
        if (!usageReader.hasAccess()) return

        runOperation {
            val count = usageReader.captureLast24Hours()
            if (count > 0) repository.rebuildUnderstanding()
            null
        }
    }

    fun rebuildContext() {
        if (state.value.busy) return
        runOperation {
            repository.rebuildUnderstanding()
            "Rebuilt local understanding"
        }
    }

    fun captureUsage() {
        if (state.value.busy) return
        runOperation {
            if (!usageReader.hasAccess()) return@runOperation "Enable usage access first"
            val count = usageReader.captureLast24Hours()
            if (count > 0) repository.rebuildUnderstanding()
            lastAutomaticUsageRefresh = android.os.SystemClock.elapsedRealtime()
            if (count > 0) "Updated usage for $count apps" else "No app usage is available in this window"
        }
    }

    private companion object {
        const val AUTO_USAGE_REFRESH_MS = 6L * 60L * 60L * 1000L
    }
}
