package com.kareem.nexus.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kareem.nexus.core.model.*
import com.kareem.nexus.platform.action.AndroidActionExecutor
import com.kareem.nexus.platform.reminder.ReminderScheduler
import com.kareem.nexus.ui.design.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(contentPadding: PaddingValues, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selected by remember { mutableStateOf<Observation?>(null) }
    var pendingReminder by remember { mutableStateOf<OpenLoop?>(null) }

    fun scheduleReminder(loop: OpenLoop) {
        val triggerAt = ReminderScheduler.defaultTriggerAt(loop)
        val reminderAction = loop.actions.firstOrNull { it.kind == NexusActionKind.REMIND }
            ?: ContextAction(NexusActionKind.REMIND, "Remind later")
        val success = ReminderScheduler.schedule(context, loop, triggerAt)
        if (success) {
            viewModel.snoozeOpenLoop(loop.id, triggerAt)
            viewModel.recordExecution(loop, reminderAction, true, "Reminder scheduled")
            Toast.makeText(context, "Reminder scheduled", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.recordExecution(loop, reminderAction, false, "Notifications are not allowed")
            Toast.makeText(context, "Allow NEXUS notifications to use reminders", Toast.LENGTH_LONG).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val loop = pendingReminder
        pendingReminder = null
        if (granted && loop != null) scheduleReminder(loop)
        else if (!granted) Toast.makeText(context, "Notification permission is required for reminders", Toast.LENGTH_LONG).show()
    }

    fun requestReminder(loop: OpenLoop) {
        val granted = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (granted) scheduleReminder(loop)
        else {
            pendingReminder = loop
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(18.dp, 22.dp, 18.dp, 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            val active = state.needsYou.size + state.waitingOn.size + state.upcoming.size
            NexusScreenHeader(
                title = "For You",
                subtitle = when {
                    active == 0 -> "Nothing important needs you right now."
                    state.needsYou.size == 1 -> "1 thing needs you."
                    state.needsYou.isNotEmpty() -> "${state.needsYou.size} things need you."
                    else -> "NEXUS is tracking ${state.waitingOn.size + state.upcoming.size} open loops."
                },
                trailing = {
                    TextButton(
                        onClick = viewModel::refreshUnderstanding,
                        enabled = "refresh" !in state.pending,
                    ) { Text("Refresh") }
                },
            )
        }

        state.error?.let { message -> item { Text(message, color = NexusColors.Rose) } }

        if (state.needsYou.isNotEmpty()) {
            item {
                NexusSectionHeader(
                    title = "Needs you",
                    subtitle = "Unresolved requests, payments and failures with a useful next step.",
                )
            }
            items(state.needsYou, key = { it.id }) { loop ->
                OpenLoopCard(
                    loop = loop,
                    busy = loop.id in state.pending,
                    onPrimary = { action ->
                        val result = AndroidActionExecutor.execute(context, loop, action)
                        viewModel.recordExecution(loop, action, result.success, result.message)
                        Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                    },
                    onRemind = { requestReminder(loop) },
                    onDone = { viewModel.resolveOpenLoop(loop.id) },
                    onDismiss = { viewModel.dismissOpenLoop(loop.id) },
                    onEvidence = {
                        selected = state.observations.firstOrNull { it.id == loop.observationId }
                    },
                )
            }
        }

        if (state.waitingOn.isNotEmpty()) {
            item {
                NexusSectionHeader(
                    title = "Waiting on",
                    subtitle = "Things that are currently in somebody else's court or still in transit.",
                )
            }
            items(state.waitingOn, key = { it.id }) { loop ->
                OpenLoopCard(
                    loop = loop,
                    busy = loop.id in state.pending,
                    onPrimary = { action ->
                        val result = AndroidActionExecutor.execute(context, loop, action)
                        viewModel.recordExecution(loop, action, result.success, result.message)
                        Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                    },
                    onRemind = { requestReminder(loop) },
                    onDone = { viewModel.resolveOpenLoop(loop.id) },
                    onDismiss = { viewModel.dismissOpenLoop(loop.id) },
                    onEvidence = { selected = state.observations.firstOrNull { it.id == loop.observationId } },
                )
            }
        }

        if (state.upcoming.isNotEmpty()) {
            item {
                NexusSectionHeader(
                    title = "Upcoming",
                    subtitle = "Commitments NEXUS detected from your recent context.",
                )
            }
            items(state.upcoming, key = { it.id }) { loop ->
                OpenLoopCard(
                    loop = loop,
                    busy = loop.id in state.pending,
                    onPrimary = { action ->
                        val result = AndroidActionExecutor.execute(context, loop, action)
                        viewModel.recordExecution(loop, action, result.success, result.message)
                        Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                    },
                    onRemind = { requestReminder(loop) },
                    onDone = { viewModel.resolveOpenLoop(loop.id) },
                    onDismiss = { viewModel.dismissOpenLoop(loop.id) },
                    onEvidence = { selected = state.observations.firstOrNull { it.id == loop.observationId } },
                )
            }
        }

        if (state.recentChanges.isNotEmpty()) {
            item {
                NexusSectionHeader(
                    title = "Changed",
                    subtitle = "Situations where new evidence changed the picture.",
                )
            }
            items(state.recentChanges.take(4), key = { it.situationId }) { brief ->
                NexusCard(accent = NexusColors.Violet) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        NexusStatusPill("${brief.evidenceCount} SIGNALS", NexusColors.Violet)
                        Text(timestamp(brief.lastUpdatedAt), color = NexusColors.TextMuted, style = MaterialTheme.typography.labelSmall)
                    }
                    Text(brief.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(brief.whatChanged, color = NexusColors.TextSecondary)
                    brief.nextStep?.let { next ->
                        Text("Next: $next", color = NexusColors.Cyan, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        if (state.needsYou.isEmpty() && state.waitingOn.isEmpty() && state.upcoming.isEmpty()) {
            item {
                NexusCard(accent = NexusColors.Mint) {
                    NexusStatusPill("ALL CLEAR", NexusColors.Mint)
                    Text("Nothing important needs you", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        if (state.observationCount == 0)
                            "Connect notifications or save a note, link or screenshot. NEXUS will stay quiet until something is actually useful."
                        else
                            "Your recent context is still searchable in Memory. NEXUS found no unresolved request, deadline, payment, failure or follow-up worth interrupting you for.",
                        color = NexusColors.TextSecondary,
                    )
                }
            }
        }
    }

    selected?.let { ObservationDetails(it) { selected = null } }
}

@Composable
private fun OpenLoopCard(
    loop: OpenLoop,
    busy: Boolean,
    onPrimary: (ContextAction) -> Unit,
    onRemind: () -> Unit,
    onDone: () -> Unit,
    onDismiss: () -> Unit,
    onEvidence: () -> Unit,
) {
    val accent = when (loop.kind) {
        OpenLoopKind.PAYMENT, OpenLoopKind.FAILURE -> NexusColors.Rose
        OpenLoopKind.UPCOMING -> NexusColors.Violet
        OpenLoopKind.WAITING_ON, OpenLoopKind.DELIVERY -> NexusColors.Amber
        OpenLoopKind.NEEDS_REPLY, OpenLoopKind.NEEDS_ACTION -> NexusColors.Cyan
        OpenLoopKind.FOLLOW_UP -> NexusColors.Mint
    }
    val primary = loop.actions.firstOrNull {
        it.kind !in setOf(NexusActionKind.REMIND, NexusActionKind.MARK_RESOLVED)
    }
    val canRemind = loop.actions.any { it.kind == NexusActionKind.REMIND }

    NexusCard(accent = accent) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            NexusStatusPill(loop.kind.label(), accent)
            Text(loop.dueAt?.let(::dueLabel) ?: timestamp(loop.createdAt), color = NexusColors.TextMuted, style = MaterialTheme.typography.labelSmall)
        }
        Text(loop.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        ContentText(loop.detail, maxLines = 4)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(humanSource(loop.source), color = NexusColors.TextMuted, style = MaterialTheme.typography.labelSmall)
            TextButton(onClick = onEvidence, enabled = !busy) { Text("Evidence") }
        }
        if (primary != null || canRemind) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (primary != null) {
                    Button(onClick = { onPrimary(primary) }, enabled = !busy, modifier = Modifier.weight(1f)) {
                        Text(primary.label)
                    }
                }
                if (canRemind) {
                    OutlinedButton(
                        onClick = onRemind,
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, NexusColors.Border),
                    ) { Text("Remind") }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onDone, enabled = !busy) { Text("Done") }
            TextButton(onClick = onDismiss, enabled = !busy) { Text("Dismiss", color = NexusColors.TextSecondary) }
        }
    }
}

private fun OpenLoopKind.label(): String = when (this) {
    OpenLoopKind.NEEDS_REPLY -> "NEEDS REPLY"
    OpenLoopKind.NEEDS_ACTION -> "NEEDS ACTION"
    OpenLoopKind.WAITING_ON -> "WAITING ON"
    OpenLoopKind.UPCOMING -> "UPCOMING"
    OpenLoopKind.PAYMENT -> "PAYMENT"
    OpenLoopKind.DELIVERY -> "DELIVERY"
    OpenLoopKind.FAILURE -> "FAILED"
    OpenLoopKind.FOLLOW_UP -> "FOLLOW UP"
}

private fun humanSource(source: String?): String {
    if (source.isNullOrBlank()) return "Saved in NEXUS"
    if (source == "NEXUS") return source
    return source.substringAfterLast('.').replace('_', ' ').replaceFirstChar { it.uppercase() }
}

private fun dueLabel(timestamp: Long): String {
    val zone = ZoneId.systemDefault()
    val date = Instant.ofEpochMilli(timestamp).atZone(zone)
    return "Due ${date.format(DateTimeFormatter.ofPattern("EEE HH:mm"))}"
}

@Composable
fun SectionTitle(title: String, subtitle: String) {
    NexusSectionHeader(title = title, subtitle = subtitle)
}
