package com.kareem.nexus.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.kareem.nexus.ui.design.NexusColors

private val NexusDarkScheme = darkColorScheme(
    primary = NexusColors.Cyan,
    secondary = NexusColors.Violet,
    tertiary = NexusColors.Mint,
    background = NexusColors.Background,
    surface = NexusColors.Surface,
    surfaceVariant = NexusColors.SurfaceRaised,
    outline = NexusColors.Border,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = NexusColors.TextPrimary,
    onSurface = NexusColors.TextPrimary,
    onSurfaceVariant = NexusColors.TextSecondary,
    error = NexusColors.Rose,
)

@Composable
fun NexusTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NexusDarkScheme,
        typography = Typography(),
        content = content,
    )
}
