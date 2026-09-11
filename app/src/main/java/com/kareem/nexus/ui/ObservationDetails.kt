package com.kareem.nexus.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.kareem.nexus.core.model.*
import com.kareem.nexus.ui.design.NexusColors
import java.io.File
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun timestamp(value: Long): String = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(value))
fun sourceLabel(context: Context, source: String?): String = when {
    source.isNullOrBlank() -> "Saved context"
    source == "NEXUS" || source == "Android Sharesheet" -> source
    else -> runCatching {
        context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(source, 0)).toString()
    }.getOrElse { source.substringAfterLast('.').replaceFirstChar(Char::uppercase) }
}
fun copyText(context: Context, text: String) {
    context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("NEXUS", text))
}
fun launchSafely(context: Context, intent: Intent) {
    try { context.startActivity(intent) }
    catch (_: Exception) { Toast.makeText(context, "This app or link is not available", Toast.LENGTH_SHORT).show() }
}
@Composable
fun ContentText(text: String, modifier: Modifier = Modifier, maxLines: Int = Int.MAX_VALUE) {
    Text(text, modifier = modifier, maxLines = maxLines,
        style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Content))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObservationDetails(observation: Observation, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val label = remember(observation.source) { sourceLabel(context, observation.source) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = NexusColors.Surface) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(label, style = MaterialTheme.typography.titleLarge)
            Text(timestamp(observation.createdAt), color = NexusColors.TextSecondary)
            if (observation.type == ObservationType.IMAGE) {
                val bitmap by produceState<android.graphics.Bitmap?>(null, observation.id) {
                    value = withContext(Dispatchers.IO) {
                        val name = observation.rawText.substringAfter("Saved image · ", "")
                        if (name.isBlank() || name != File(name).name) null else runCatching {
                            val path = File(context.filesDir, "attachments/$name").path
                            val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            android.graphics.BitmapFactory.decodeFile(path, options)
                            var sample = 1
                            while (maxOf(options.outWidth, options.outHeight) / sample > 1600) sample *= 2
                            options.inJustDecodeBounds = false; options.inSampleSize = sample
                            android.graphics.BitmapFactory.decodeFile(path, options)
                        }.getOrNull()
                    }
                }
                bitmap?.let { Image(it.asImageBitmap(), contentDescription = "Saved image", modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) }
                    ?: Text("Image preview unavailable. Older shared images may require sharing again.")
            }
            SelectionContainer { ContentText(observation.rawText) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { copyText(context, observation.rawText) }) { Text("Copy text") }
                val url = remember(observation.rawText) {
                    Regex("https?://[^\\s<>]+", RegexOption.IGNORE_CASE).find(observation.rawText)?.value
                }
                if (url != null) OutlinedButton(onClick = {
                    launchSafely(context, Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                }) { Text("Open link") }
            }
            val sourceIntent = remember(observation.source) { observation.source?.let(context.packageManager::getLaunchIntentForPackage) }
            if (sourceIntent != null) OutlinedButton(onClick = { launchSafely(context, sourceIntent) }) { Text("Open source app") }
            Spacer(Modifier.height(20.dp))
        }
    }
}
