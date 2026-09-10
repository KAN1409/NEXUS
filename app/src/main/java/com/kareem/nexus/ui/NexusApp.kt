package com.kareem.nexus.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kareem.nexus.ui.design.NexusColors
import com.kareem.nexus.ui.design.NexusIcon
import com.kareem.nexus.ui.design.NexusIconType

private enum class NexusDestination(
    val label: String,
    val icon: NexusIconType,
) {
    ForYou("For You", NexusIconType.Home),
    Discover("Discover", NexusIconType.Discover),
    Memory("Memory", NexusIconType.Memory),
    Activity("Activity", NexusIconType.Activity),
    Settings("Settings", NexusIconType.Settings),
}

@Composable
fun NexusApp() {
    var destination by rememberSaveable { mutableStateOf(NexusDestination.ForYou) }

    BackHandler(enabled = destination != NexusDestination.ForYou) {
        destination = NexusDestination.ForYou
    }

    Scaffold(
        containerColor = NexusColors.Background,
        bottomBar = {
            NavigationBar(containerColor = NexusColors.Surface) {
                NexusDestination.entries.forEach { item ->
                    NavigationBarItem(
                        selected = destination == item,
                        onClick = { destination = item },
                        icon = {
                            NexusIcon(
                                type = item.icon,
                                modifier = Modifier.size(24.dp),
                                primary = if (destination == item) NexusColors.Cyan else NexusColors.TextSecondary,
                                secondary = if (destination == item) NexusColors.Violet else NexusColors.TextSecondary,
                            )
                        },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            when (destination) {
                NexusDestination.ForYou -> HomeScreen(contentPadding = padding)
                NexusDestination.Discover -> PlaceholderScreen("Discover", "Fresh things worth your attention.", padding)
                NexusDestination.Memory -> PlaceholderScreen("Memory", "Everything NEXUS remembers for you.", padding)
                NexusDestination.Activity -> PlaceholderScreen("Activity", "What NEXUS has been doing for you.", padding)
                NexusDestination.Settings -> PlaceholderScreen("Settings", "Control data, providers and behavior.", padding)
            }
        }
    }
}
