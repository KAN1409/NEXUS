package com.kareem.nexus.ingest

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import com.kareem.nexus.core.model.ObservationType
import com.kareem.nexus.domain.repository.NexusRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShareIngestor @Inject constructor(
    private val repository: NexusRepository,
) {
    suspend fun ingest(intent: Intent): Int {
        if (intent.action != Intent.ACTION_SEND && intent.action != Intent.ACTION_SEND_MULTIPLE) return 0
        val source = intent.getStringExtra(Intent.EXTRA_REFERRER_NAME) ?: "Android Sharesheet"
        var captured = 0
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim()
        if (!text.isNullOrBlank()) {
            repository.captureObservation(
                type = if (looksLikeUrl(text)) ObservationType.SHARED_LINK else ObservationType.SHARED_TEXT,
                rawText = text,
                source = source,
                metadataJson = "{\"mime\":\"${escape(intent.type.orEmpty())}\"}",
            )
            captured++
        }
        val uris = collectUris(intent)
        uris.forEach { uri ->
            repository.captureObservation(
                type = ObservationType.IMAGE,
                rawText = uri.toString(),
                source = source,
                metadataJson = "{\"mime\":\"${escape(intent.type.orEmpty())}\",\"uri\":\"${escape(uri.toString())}\"}",
            )
            captured++
        }
        return captured
    }

    private fun collectUris(intent: Intent): List<Uri> {
        val result = linkedSetOf<Uri>()
        @Suppress("DEPRECATION")
        intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let(result::add)
        @Suppress("DEPRECATION")
        intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.let(result::addAll)
        val clip: ClipData? = intent.clipData
        if (clip != null) for (i in 0 until clip.itemCount) clip.getItemAt(i).uri?.let(result::add)
        return result.toList()
    }

    private fun looksLikeUrl(value: String): Boolean = runCatching {
        val uri = Uri.parse(value)
        uri.scheme == "http" || uri.scheme == "https"
    }.getOrDefault(false)

    private fun escape(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"")
}
