package com.kareem.nexus.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
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
        typography = Typography(
            headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.7).sp),
            headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 23.sp, lineHeight = 30.sp),
            titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = 28.sp),
            titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
            bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 25.sp, textDirection = TextDirection.Content),
            bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 22.sp, textDirection = TextDirection.Content),
            labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 16.sp),
        ),
        shapes = Shapes(medium = RoundedCornerShape(14.dp), large = RoundedCornerShape(20.dp)),
        content = content,
    )
}

