package com.kareem.nexus.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kareem.nexus.core.model.ObservationType
import com.kareem.nexus.domain.repository.NexusRepository
import com.kareem.nexus.ingest.ShareIngestor
import com.kareem.nexus.observe.UsageObservationReader
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CaptureUiState(
    val message: String? = null,
    val busy: Boolean = false,
    val usageAccess: Boolean = false,
    val notificationAccess: Boolean = false,
)

@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val repository: NexusRepository,
    private val shareIngestor: ShareIngestor,
    private val usageReader: UsageObservationReader,
) : ViewModel() {
    private val _state = MutableStateFlow(
        CaptureUiState(
            usageAccess = usageReader.hasAccess(),
            notificationAccess = usageReader.hasNotificationAccess(),
        )
    )
    val state: StateFlow<CaptureUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.rebuildUnderstanding()
        }
    }

    fun captureText(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.captureObservation(
                if (text.trim().startsWith("http://") || text.trim().startsWith("https://")) ObservationType.SHARED_LINK else ObservationType.MANUAL,
                text,
                "NEXUS",
            )
            repository.rebuildUnderstanding()
            _state.update { it.copy(message = "Saved and understood") }
        }
    }

    fun ingestShare(intent: android.content.Intent) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            val count = shareIngestor.ingest(intent)
            if (count > 0) repository.rebuildUnderstanding()
            _state.update { it.copy(busy = false, message = if (count > 0) "Captured and understood $count item${if (count == 1) "" else "s"}" else null) }
        }
    }

    fun refreshAccessState() = _state.update {
        it.copy(
            usageAccess = usageReader.hasAccess(),
            notificationAccess = usageReader.hasNotificationAccess(),
        )
    }

    fun refreshContext() {
        viewModelScope.launch {
            val usageAccess = usageReader.hasAccess()
            val notificationAccess = usageReader.hasNotificationAccess()
            _state.update { it.copy(usageAccess = usageAccess, notificationAccess = notificationAccess) }
            if (usageAccess) {
                usageReader.captureLast24Hours()
            }
            repository.rebuildUnderstanding()
        }
    }

    fun captureUsage() {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            val count = usageReader.captureLast24Hours()
            if (count > 0) repository.rebuildUnderstanding()
            _state.update {
                it.copy(
                    busy = false,
                    usageAccess = usageReader.hasAccess(),
                    notificationAccess = usageReader.hasNotificationAccess(),
                    message = if (count > 0) "Understood $count apps" else "Usage access is required",
                )
            }
        }
    }
}
