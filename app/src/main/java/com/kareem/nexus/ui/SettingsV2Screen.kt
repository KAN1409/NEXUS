package com.kareem.nexus.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.kareem.nexus.BuildConfig
import com.kareem.nexus.domain.intelligence.OnDeviceAi
import com.kareem.nexus.domain.intelligence.OnDeviceAiState
import com.kareem.nexus.ui.design.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsV2ViewModel @Inject constructor(
    private val onDeviceAi: OnDeviceAi,
) : ViewModel() {
    private val _aiState = MutableStateFlow<OnDeviceAiState?>(null)
    val aiState = _aiState.asStateFlow()

    init { refreshAi() }

    fun refreshAi() {
        viewModelScope.launch { _aiState.value = onDeviceAi.state(forceRefresh = true) }
    }
}

@Composable
fun SettingsV2Screen(
    padding: PaddingValues,
    state: CaptureUiState,
    onNotificationSettings: () -> Unit,
    onUsageSettings: () -> Unit,
    onRefreshUsage: () -> Unit,
    onRebuild: () -> Unit,
    viewModel: SettingsV2ViewModel = hiltViewModel(),
) {
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(18.dp, 22.dp, 18.dp, 104.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            NexusScreenHeader(
                title = "Settings",
                subtitle = "Sources, local intelligence, reminders and privacy.",
            )
        }

        item { CompactSectionTitle("Sources") }
        item {
            SettingsRow(
                icon = NexusIconType.Activity,
                title = "Notification observation",
                subtitle = "Turn notification content into searchable evidence and open loops",
                status = if (state.notificationAccess) "Enabled" else "Off",
                statusColor = if (state.notificationAccess) NexusColors.Mint else NexusColors.TextMuted,
                onClick = onNotificationSettings,
            )
        }
        item {
            SettingsRow(
                icon = NexusIconType.Discover,
                title = "App usage signals",
                subtitle = "Optional low-detail usage context; never a primary task feed",
                status = if (state.usageAccess) "Enabled" else "Off",
                statusColor = if (state.usageAccess) NexusColors.Mint else NexusColors.TextMuted,
                onClick = onUsageSettings,
            )
        }
        if (state.usageAccess) {
            item {
                SettingsRow(
                    icon = NexusIconType.Activity,
                    title = "Refresh usage snapshot",
                    subtitle = "Update recent behavioral context",
                    status = if (state.busy) "Working" else "Run",
                    statusColor = NexusColors.Cyan,
                    enabled = !state.busy,
                    onClick = onRefreshUsage,
                )
            }
        }

        item { CompactSectionTitle("Value engine") }
        item {
            SettingsRow(
                icon = NexusIconType.Activity,
                title = "Open loops",
                subtitle = "Requests, waiting-on items, payments, failures and upcoming commitments",
                status = "Active",
                statusColor = NexusColors.Cyan,
            )
        }
        item {
            SettingsRow(
                icon = NexusIconType.Saved,
                title = "Real actions",
                subtitle = "Open source apps, calendar, reminders, navigation, dialer and copy",
                status = "Enabled",
                statusColor = NexusColors.Mint,
            )
        }
        item {
            SettingsRow(
                icon = NexusIconType.Memory,
                title = "Memory recall",
                subtitle = "Arabic/English fuzzy recall, numeric anchors, OCR text and optional Nano expansion",
                status = "Active",
                statusColor = NexusColors.Violet,
            )
        }

        item { CompactSectionTitle("On-device intelligence") }
        item {
            val aiLabel = when (aiState) {
                OnDeviceAiState.READY -> "Ready"
                OnDeviceAiState.DOWNLOADABLE -> "Available"
                OnDeviceAiState.DOWNLOADING -> "Preparing"
                OnDeviceAiState.UNAVAILABLE -> "Unavailable"
                OnDeviceAiState.ERROR -> "Fallback"
                null -> "Checking"
            }
            val aiColor = when (aiState) {
                OnDeviceAiState.READY -> NexusColors.Mint
                OnDeviceAiState.DOWNLOADABLE, OnDeviceAiState.DOWNLOADING -> NexusColors.Cyan
                else -> NexusColors.TextMuted
            }
            SettingsRow(
                icon = NexusIconType.Discover,
                title = "Gemini Nano assist",
                subtitle = when (aiState) {
                    OnDeviceAiState.READY -> "Can enrich image/context understanding locally when useful"
                    OnDeviceAiState.DOWNLOADABLE -> "Supported by this device; Android model is not ready yet"
                    OnDeviceAiState.DOWNLOADING -> "Android is preparing the local model"
                    else -> "Deterministic local intelligence remains the required fallback"
                },
                status = aiLabel,
                statusColor = aiColor,
                onClick = viewModel::refreshAi,
            )
        }
        item {
            SettingsRow(
                icon = NexusIconType.Discover,
                title = "Noise control",
                subtitle = "Promotions, reviews, social reactions and passive status are suppressed",
                status = "Filtered",
                statusColor = NexusColors.Violet,
            )
        }
        item {
            SettingsRow(
                icon = NexusIconType.Activity,
                title = "Rebuild intelligence",
                subtitle = "Backfill Memory, Situations and open loops from saved evidence",
                status = if (state.busy) "Working" else "Run",
                statusColor = NexusColors.Cyan,
                enabled = !state.busy,
                onClick = onRebuild,
            )
        }

        item { CompactSectionTitle("Privacy & data") }
        item {
            SettingsRow(
                icon = NexusIconType.Saved,
                title = "Privacy",
                subtitle = "Evidence, OCR, situations, open loops and outcomes stay in local app storage",
                status = "On-device",
                statusColor = NexusColors.Mint,
            )
        }
        item {
            SettingsRow(
                icon = NexusIconType.Web,
                title = "Location",
                subtitle = "No location permission is requested in this release",
                status = "Not requested",
                statusColor = NexusColors.TextMuted,
            )
        }

        item { CompactSectionTitle("About") }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = NexusColors.Surface,
                shape = RoundedCornerShape(NexusRadius.Medium),
                border = BorderStroke(1.dp, NexusColors.BorderSoft),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("NEXUS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "${BuildConfig.VERSION_NAME} · ${BuildConfig.VERSION_CODE}",
                                color = NexusColors.Cyan,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                        NexusStatusPill("LOCAL-FIRST", NexusColors.Mint)
                    }
                    Text(
                        "Capture → Remember → Understand → Connect → Track → Act → Learn",
                        color = NexusColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        if (state.busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
    }
}

@Composable
private fun CompactSectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = NexusColors.TextSecondary,
        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
    )
}

@Composable
private fun SettingsRow(
    icon: NexusIconType,
    title: String,
    subtitle: String,
    status: String,
    statusColor: Color,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().then(
            if (onClick != null && enabled) Modifier.clickable(onClick = onClick) else Modifier
        ),
        color = NexusColors.Surface,
        shape = RoundedCornerShape(NexusRadius.Medium),
        border = BorderStroke(1.dp, NexusColors.BorderSoft),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                color = statusColor.copy(alpha = .10f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    NexusIcon(
                        type = icon,
                        modifier = Modifier.size(23.dp),
                        primary = if (enabled) statusColor else NexusColors.TextMuted,
                        secondary = NexusColors.Violet,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(subtitle, color = NexusColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            NexusStatusPill(status.uppercase(), statusColor)
        }
    }
}
