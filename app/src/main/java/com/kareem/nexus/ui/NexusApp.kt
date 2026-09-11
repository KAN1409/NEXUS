package com.kareem.nexus.ui

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kareem.nexus.BuildConfig
import com.kareem.nexus.ui.design.*

private enum class NexusDestination(val label: String, val icon: NexusIconType) {
    ForYou("For You", NexusIconType.Home),
    Discover("Discover", NexusIconType.Discover),
    Memory("Memory", NexusIconType.Memory),
    Activity("Activity", NexusIconType.Activity),
    Settings("Settings", NexusIconType.Settings),
}

@Composable
fun NexusApp(viewModel: CaptureViewModel = hiltViewModel()) {
    var destination by rememberSaveable { mutableStateOf(NexusDestination.ForYou) }
    var showCapture by rememberSaveable { mutableStateOf(false) }
    var draft by rememberSaveable { mutableStateOf("") }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val snackbars = remember { SnackbarHostState() }
    var savedRevision by remember { mutableIntStateOf(state.saveRevision) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val shareIntent = Intent(Intent.ACTION_SEND)
                .setType("image/*")
                .putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            viewModel.ingestShare(shareIntent)
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshContext() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbars.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(state.saveRevision) {
        if (state.saveRevision > savedRevision) {
            showCapture = false
            draft = ""
            savedRevision = state.saveRevision
        }
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
                ExtendedFloatingActionButton(
                    onClick = { showCapture = true },
                    containerColor = NexusColors.Cyan,
                    contentColor = NexusColors.Background,
                    icon = {
                        NexusIcon(
                            NexusIconType.Capture,
                            Modifier.size(22.dp),
                            NexusColors.Background,
                            NexusColors.Background,
                        )
                    },
                    text = { Text("Add", fontWeight = FontWeight.Bold) },
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = NexusColors.Surface,
                tonalElevation = 0.dp,
            ) {
                NexusDestination.entries.forEach { item ->
                    val selected = destination == item
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            destination = item
                            showCapture = false
                        },
                        icon = {
                            NexusIcon(
                                item.icon,
                                Modifier.size(22.dp),
                                if (selected) NexusColors.Cyan else NexusColors.TextSecondary,
                                NexusColors.Violet,
                            )
                        },
                        label = {
                            Text(item.label, maxLines = 1, style = MaterialTheme.typography.labelSmall)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = NexusColors.VioletSoft,
                            selectedTextColor = NexusColors.Violet,
                            unselectedTextColor = NexusColors.TextSecondary,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        if (showCapture) {
            AddToMemoryScreen(
                padding = padding,
                draft = draft,
                onDraftChange = { draft = it },
                busy = state.busy,
                onSave = { viewModel.captureText(draft) },
                onAddImage = { imagePicker.launch("image/*") },
                onBack = { showCapture = false },
            )
        } else {
            when (destination) {
                NexusDestination.ForYou -> HomeScreen(padding)
                NexusDestination.Memory -> ObservationsScreen(padding)
                NexusDestination.Discover -> DiscoverScreen(padding)
                NexusDestination.Activity -> ActivityScreen(padding)
                NexusDestination.Settings -> SettingsV2Screen(
                    padding = padding,
                    state = state,
                    onNotificationSettings = notificationSettings,
                    onUsageSettings = usageSettings,
                    onRefreshUsage = viewModel::captureUsage,
                    onRebuild = viewModel::refreshContext,
                )
            }
        }
    }
}

@Composable
private fun AddToMemoryScreen(
    padding: PaddingValues,
    draft: String,
    onDraftChange: (String) -> Unit,
    busy: Boolean,
    onSave: () -> Unit,
    onAddImage: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize()
            .padding(padding)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        NexusScreenHeader(
            title = "Add to Memory",
            subtitle = "Save something once. Let NEXUS connect it later.",
        )
        NexusCard(accent = NexusColors.Violet) {
            Text("Good things to save", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "A request, link, appointment, decision, idea, screenshot or detail you may want to find again.",
                color = NexusColors.TextSecondary,
            )
        }
        OutlinedTextField(
            value = draft,
            onValueChange = onDraftChange,
            modifier = Modifier.fillMaxWidth(),
            minLines = 6,
            label = { Text("Text or link") },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onSave,
                enabled = draft.isNotBlank() && !busy,
                modifier = Modifier.weight(1f),
            ) {
                Text(if (busy) "Saving…" else "Save text")
            }
            OutlinedButton(
                onClick = onAddImage,
                enabled = !busy,
                modifier = Modifier.weight(1f),
            ) {
                NexusIcon(NexusIconType.Image, Modifier.size(18.dp), NexusColors.Cyan, NexusColors.Violet)
                Spacer(Modifier.width(8.dp))
                Text("Add image")
            }
        }
        Text(
            "Images are copied into private NEXUS storage and scanned locally for searchable text when supported.",
            color = NexusColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
        TextButton(onClick = onBack, enabled = !busy) { Text("Back") }
    }
}

@Composable
private fun SettingsScreen(
    padding: PaddingValues,
    state: CaptureUiState,
    onNotificationSettings: () -> Unit,
    onUsageSettings: () -> Unit,
    onRefreshUsage: () -> Unit,
    onRebuild: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        NexusScreenHeader(
            title = "Settings",
            subtitle = "Control, privacy and permissions.",
        )

        AccessCard(
            title = "Notification observation",
            enabled = state.notificationAccess,
            description = "Notification text is observed locally. Android controls which notifications are exposed.",
            icon = NexusIconType.Activity,
            onClick = onNotificationSettings,
        )

        AccessCard(
            title = "App usage signals",
            enabled = state.usageAccess,
            description = "Recent usage helps NEXUS detect behavioral patterns without uploading your app history.",
            icon = NexusIconType.Discover,
            onClick = onUsageSettings,
        )

        if (state.usageAccess) {
            OutlinedButton(
                onClick = onRefreshUsage,
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Refresh app usage") }
        }

        NexusInfoCard(
            title = "Location",
            status = "Not requested",
            body = "NEXUS does not request location permission in this release.",
            icon = NexusIconType.Web,
            accent = NexusColors.TextSecondary,
        )

        NexusInfoCard(
            title = "Privacy",
            status = "On-device",
            body = "Observations, OCR text, actions and understanding stay in NEXUS local storage.",
            icon = NexusIconType.Saved,
            accent = NexusColors.Mint,
        )

        NexusInfoCard(
            title = "Memory intelligence",
            status = "Active",
            body = "Search includes typo tolerance, bilingual concept matching and OCR text extracted from supported images.",
            icon = NexusIconType.Memory,
            accent = NexusColors.Cyan,
        )

        NexusInfoCard(
            title = "Suggestion quality",
            status = "Noise filtered",
            body = "Promotions, review requests, social reactions and passive status notifications are suppressed before actions are surfaced.",
            icon = NexusIconType.Discover,
            accent = NexusColors.Violet,
        )

        OutlinedButton(
            onClick = onRebuild,
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Rebuild local context") }

        HorizontalDivider(color = NexusColors.Border)

        NexusCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("About", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("NEXUS ${BuildConfig.VERSION_NAME} · ${BuildConfig.VERSION_CODE}", color = NexusColors.Cyan)
                }
                NexusStatusPill("LOCAL-FIRST", NexusColors.Mint)
            }
            Text(
                "Observe → Understand → Connect → Prioritize → Suggest → Act → Learn",
                color = NexusColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        Spacer(Modifier.height(88.dp))
    }
}

@Composable
private fun AccessCard(
    title: String,
    enabled: Boolean,
    description: String,
    icon: NexusIconType,
    onClick: () -> Unit,
) {
    NexusCard(accent = if (enabled) NexusColors.Mint else NexusColors.TextMuted) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                NexusIcon(icon, Modifier.size(28.dp), NexusColors.Cyan, NexusColors.Violet)
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            NexusStatusPill(if (enabled) "CONNECTED" else "OFF", if (enabled) NexusColors.Mint else NexusColors.TextMuted)
        }
        Text(description, color = NexusColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = onClick) { Text("Manage access") }
    }
}

@Composable
private fun NexusInfoCard(
    title: String,
    status: String,
    body: String,
    icon: NexusIconType,
    accent: androidx.compose.ui.graphics.Color,
) {
    NexusCard(accent = accent) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                NexusIcon(icon, Modifier.size(28.dp), accent, NexusColors.Violet)
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            NexusStatusPill(status.uppercase(), accent)
        }
        Text(body, color = NexusColors.TextSecondary)
    }
}
