package com.kareem.nexus.ui

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kareem.nexus.ui.design.*

private enum class NexusDestination(val label: String, val icon: NexusIconType) {
    ForYou("For You", NexusIconType.Home), Discover("Discover", NexusIconType.Discover), Memory("Memory", NexusIconType.Memory), Activity("Activity", NexusIconType.Activity), Settings("Settings", NexusIconType.Settings),
}

@Composable
fun NexusApp(viewModel: CaptureViewModel = hiltViewModel()) {
    var destination by rememberSaveable { mutableStateOf(NexusDestination.ForYou) }
    var showCapture by rememberSaveable { mutableStateOf(false) }
    val captureState by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    BackHandler(enabled = showCapture || destination != NexusDestination.ForYou) {
        if (showCapture) showCapture = false else destination = NexusDestination.ForYou
    }

    Scaffold(
        containerColor = NexusColors.Background,
        floatingActionButton = {
            if (destination == NexusDestination.ForYou || destination == NexusDestination.Memory) {
                ExtendedFloatingActionButton(
                    onClick = { showCapture = true },
                    containerColor = NexusColors.Violet,
                    icon = { NexusIcon(NexusIconType.Capture, Modifier.size(22.dp), NexusColors.TextPrimary, NexusColors.Cyan) },
                    text = { Text("Add") },
                )
            }
        },
        bottomBar = {
            NavigationBar(containerColor = NexusColors.Surface) {
                NexusDestination.entries.forEach { item ->
                    NavigationBarItem(
                        selected = destination == item,
                        onClick = { destination = item; showCapture = false },
                        icon = { NexusIcon(item.icon, Modifier.size(24.dp), if (destination == item) NexusColors.Cyan else NexusColors.TextSecondary, if (destination == item) NexusColors.Violet else NexusColors.TextSecondary) },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { padding ->
        when {
            showCapture -> CaptureScreen(
                padding,
                captureState,
                onSave = { viewModel.captureText(it); showCapture = false },
                onNotificationAccess = { context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")) },
                onUsageAccess = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
                onCaptureUsage = viewModel::captureUsage,
            )
            destination == NexusDestination.ForYou -> HomeScreen(contentPadding = padding)
            destination == NexusDestination.Memory -> ObservationsScreen(contentPadding = padding)
            destination == NexusDestination.Discover -> PlaceholderScreen("Discover", "Fresh things worth your attention.", padding)
            destination == NexusDestination.Activity -> PlaceholderScreen("Activity", "Your observation and action history will appear here as NEXUS starts working for you.", padding)
            else -> SettingsScreen(padding, captureState, { context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")) }, { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }, viewModel::captureUsage)
        }
    }
}

@Composable
private fun CaptureScreen(padding: PaddingValues, state: CaptureUiState, onSave: (String) -> Unit, onNotificationAccess: () -> Unit, onUsageAccess: () -> Unit, onCaptureUsage: () -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, top = padding.calculateTopPadding() + 28.dp, bottom = padding.calculateBottomPadding() + 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Add to NEXUS", style = MaterialTheme.typography.headlineLarge)
        Text("Paste a thought, link or anything you want NEXUS to remember.", color = NexusColors.TextSecondary)
        OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth(), minLines = 4, label = { Text("Text or link") })
        Button(onClick = { onSave(text) }, enabled = text.isNotBlank() && !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Save observation") }
        HorizontalDivider()
        Text("Passive context", style = MaterialTheme.typography.titleLarge)
        TextButton(onClick = onNotificationAccess) { Text("Notification access") }
        TextButton(onClick = onUsageAccess) { Text("Usage access") }
        Button(onClick = onCaptureUsage, enabled = state.usageAccess && !state.busy) { Text("Learn from last 24 hours") }
    }
}

@Composable
private fun SettingsScreen(padding: PaddingValues, state: CaptureUiState, notificationAccess: () -> Unit, usageAccess: () -> Unit, captureUsage: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, top = padding.calculateTopPadding() + 28.dp, bottom = padding.calculateBottomPadding() + 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineLarge)
        Text("Control what NEXUS is allowed to observe.", color = NexusColors.TextSecondary)
        Surface(color = NexusColors.Surface, shape = MaterialTheme.shapes.large) { Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text("Notification observation", style = MaterialTheme.typography.titleMedium); Text("Explicit Android access. NEXUS stores captured text locally.", color = NexusColors.TextSecondary); TextButton(onClick = notificationAccess) { Text("Open notification access") }
        }}
        Surface(color = NexusColors.Surface, shape = MaterialTheme.shapes.large) { Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text("App usage signals", style = MaterialTheme.typography.titleMedium); Text(if (state.usageAccess) "Access granted" else "Access not granted", color = NexusColors.TextSecondary); TextButton(onClick = usageAccess) { Text("Open usage access") }; if (state.usageAccess) TextButton(onClick = captureUsage) { Text("Capture last 24 hours") }
        }}
    }
}
