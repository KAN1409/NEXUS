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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kareem.nexus.ui.design.*

private enum class NexusDestination(val label: String, val icon: NexusIconType) {
    ForYou("For You", NexusIconType.Home),
    Situations("Situations", NexusIconType.Discover),
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
            NavigationBar(containerColor = NexusColors.Surface, tonalElevation = 0.dp) {
                NexusDestination.entries.forEach { item ->
                    val selected = destination == item
                    NavigationBarItem(
                        selected = selected,
                        onClick = { destination = item; showCapture = false },
                        icon = {
                            NexusIcon(
                                item.icon,
                                Modifier.size(22.dp),
                                if (selected) NexusColors.Cyan else NexusColors.TextSecondary,
                                NexusColors.Violet,
                            )
                        },
                        label = { Text(item.label, maxLines = 1, style = MaterialTheme.typography.labelSmall) },
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
                NexusDestination.Situations -> DiscoverScreen(padding)
                NexusDestination.Memory -> ObservationsScreen(padding)
                NexusDestination.Activity -> ActivityScreen(padding)
                NexusDestination.Settings -> SettingsV2Screen(
                    padding = padding,
                    state = state,
                    onNotificationSettings = notificationSettings,
                    onUsageSettings = usageSettings,
                    onRefreshUsage = viewModel::captureUsage,
                    onRebuild = viewModel::rebuildContext,
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
            subtitle = "Capture now. NEXUS will connect it to people, situations and open loops locally.",
        )
        NexusCard(accent = NexusColors.Violet) {
            Text("Save anything worth finding again", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "A request, promise, link, appointment, decision, amount, screenshot or detail. Saving is immediate; enrichment happens after capture.",
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
            ) { Text(if (busy) "Saving…" else "Save") }
            OutlinedButton(
                onClick = onAddImage,
                enabled = !busy,
                modifier = Modifier.weight(1f),
            ) {
                NexusIcon(NexusIconType.Image, Modifier.size(18.dp), NexusColors.Cyan, NexusColors.Violet)
                Spacer(Modifier.width(8.dp))
                Text("Image")
            }
        }
        Text(
            "Images are copied into private NEXUS storage. Searchable text extraction and local enrichment run without uploading the image to NEXUS servers.",
            color = NexusColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
        TextButton(onClick = onBack, enabled = !busy) { Text("Back") }
    }
}
