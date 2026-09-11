package com.kareem.nexus.ui

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kareem.nexus.BuildConfig
import com.kareem.nexus.ui.design.*

private enum class NexusDestination(val label: String, val icon: NexusIconType) {
    ForYou("For You", NexusIconType.Home), Discover("Discover", NexusIconType.Discover), Memory("Memory", NexusIconType.Memory), Activity("Activity", NexusIconType.Activity), Settings("Settings", NexusIconType.Settings),
}

@Composable
fun NexusApp(viewModel: CaptureViewModel = hiltViewModel()) {
    var destination by rememberSaveable { mutableStateOf(NexusDestination.ForYou) }
    var showCapture by rememberSaveable { mutableStateOf(false) }
    var draft by rememberSaveable { mutableStateOf("") }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbars = remember { SnackbarHostState() }
    var savedRevision by remember { mutableIntStateOf(state.saveRevision) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshContext() }
    LaunchedEffect(state.message) {
        state.message?.let { snackbars.showSnackbar(it); viewModel.clearMessage() }
    }
    LaunchedEffect(state.saveRevision) {
        if (state.saveRevision > savedRevision) { showCapture = false; draft = ""; savedRevision = state.saveRevision }
    }
    BackHandler(showCapture || destination != NexusDestination.ForYou) {
        if (showCapture) showCapture = false else destination = NexusDestination.ForYou
    }
    val notificationSettings = { launchSafely(context, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
    val usageSettings = { launchSafely(context, Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
    Scaffold(
        containerColor = NexusColors.Background,
        snackbarHost = { SnackbarHost(snackbars) },
        floatingActionButton = {
            if (!showCapture && destination in setOf(NexusDestination.ForYou, NexusDestination.Memory)) {
                ExtendedFloatingActionButton(onClick = { showCapture = true }, containerColor = NexusColors.Cyan,
                    icon = { NexusIcon(NexusIconType.Capture, Modifier.size(22.dp), NexusColors.Background, NexusColors.Background) }, text = { Text("Add") })
            }
        },
        bottomBar = {
            NavigationBar(containerColor = NexusColors.Surface, tonalElevation = 0.dp) {
                NexusDestination.entries.forEach { item ->
                    NavigationBarItem(selected = destination == item,
                        onClick = { destination = item; showCapture = false },
                        icon = { NexusIcon(item.icon, Modifier.size(22.dp), if (destination == item) NexusColors.Cyan else NexusColors.TextSecondary, NexusColors.Violet) },
                        label = { Text(item.label, maxLines = 1, style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = NexusColors.SurfaceRaised, selectedTextColor = NexusColors.Cyan))
                }
            }
        },
    ) { padding ->
        if (showCapture) {
            Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Add to Memory", style = MaterialTheme.typography.headlineLarge)
                Text("Save a note, request or link. You can find it later and review any suggested next step.", color = NexusColors.TextSecondary)
                OutlinedTextField(draft, { draft = it }, Modifier.fillMaxWidth(), minLines = 5, label = { Text("Text or link") })
                Button(onClick = { viewModel.captureText(draft) }, enabled = draft.isNotBlank() && !state.busy, modifier = Modifier.fillMaxWidth()) { Text(if (state.busy) "Saving…" else "Save to Memory") }
                TextButton(onClick = { showCapture = false }, enabled = !state.busy) { Text("Back") }
            }
        } else when (destination) {
            NexusDestination.ForYou -> HomeScreen(padding)
            NexusDestination.Memory -> ObservationsScreen(padding)
            NexusDestination.Discover -> DiscoverScreen(padding)
            NexusDestination.Activity -> ActivityScreen(padding)
            NexusDestination.Settings -> Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Settings", style = MaterialTheme.typography.headlineLarge)
                Text("Your context. Your control.", color = NexusColors.TextSecondary)
                AccessCard("Notifications", state.notificationAccess, "Notification text stays on this device. Android controls access; sensitive notifications may be hidden by the system.", notificationSettings)
                AccessCard("App usage", state.usageAccess, "A recent usage summary helps identify patterns. Android's reporting window may extend beyond exactly 24 hours.", usageSettings)
                if (state.usageAccess) OutlinedButton(onClick = viewModel::captureUsage, enabled = !state.busy) { Text("Refresh app usage") }
                HorizontalDivider(color = NexusColors.Border)
                Text("On-device understanding", style = MaterialTheme.typography.titleMedium)
                Text("NEXUS uses local text rules and topic matches. Suggestions may be wrong; review their original evidence. Starting an action tracks your progress. Completion is recorded when you mark it done.", color = NexusColors.TextSecondary)
                Text("Images are saved locally for viewing. Image text recognition and semantic search are not included in this version.", color = NexusColors.TextSecondary)
                Text("NEXUS ${BuildConfig.VERSION_NAME} · ${BuildConfig.VERSION_CODE}", style = MaterialTheme.typography.labelMedium, color = NexusColors.Cyan)
                if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun AccessCard(title: String, enabled: Boolean, description: String, onClick: () -> Unit) {
    Surface(color = NexusColors.Surface, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(if (enabled) "Connected" else "Off", color = if (enabled) NexusColors.Mint else NexusColors.TextSecondary)
            }
            Text(description, color = NexusColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onClick) { Text("Manage access") }
        }
    }
}
