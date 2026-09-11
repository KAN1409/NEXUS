package com.kareem.nexus.ingest

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.kareem.nexus.core.model.ObservationType
import com.kareem.nexus.domain.intelligence.ContextIntelligence
import com.kareem.nexus.domain.intelligence.OnDeviceAi
import com.kareem.nexus.domain.repository.NexusRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

@Singleton
class ShareIngestor @Inject constructor(
    private val repository: NexusRepository,
    private val onDeviceAi: OnDeviceAi,
    @ApplicationContext private val context: Context,
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

        collectUris(intent).forEach { uri ->
            require(uri.scheme == "content") { "Unsupported attachment" }
            val dir = File(context.filesDir, "attachments").apply { mkdirs() }
            val file = File(dir, UUID.randomUUID().toString() + ".image")
            try {
                context.contentResolver.openInputStream(uri).use { input ->
                    requireNotNull(input) { "Attachment is unavailable" }
                    file.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var total = 0L
                        while (true) {
                            val count = input.read(buffer)
                            if (count == -1) break
                            total += count
                            require(total <= 20L * 1024 * 1024) { "Image exceeds 20 MB" }
                            output.write(buffer, 0, count)
                        }
                    }
                }

                val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                android.graphics.BitmapFactory.decodeFile(file.path, options)
                require(options.outWidth > 0 && options.outHeight > 0) { "Unsupported image" }

                val mlKitText = recognizeLatinText(file).take(4000)
                val nanoText = onDeviceAi.extractVisibleText(file)?.take(4000).orEmpty()
                val ocrText = mergeRecognition(mlKitText, nanoText).take(6000)
                val searchableText = buildString {
                    append("Saved image")
                    if (ocrText.isNotBlank()) {
                        append("\n")
                        append(ocrText)
                    }
                }

                repository.captureObservation(
                    type = ObservationType.IMAGE,
                    rawText = searchableText,
                    source = source,
                    metadataJson = JSONObject()
                        .put("mime", intent.type)
                        .put("attachment", file.name)
                        .put("ocr", ocrText.isNotBlank())
                        .put("latinOcr", mlKitText.isNotBlank())
                        .put("nanoVision", nanoText.isNotBlank())
                        .toString(),
                )
                captured++
            } catch (error: Exception) {
                file.delete()
                throw error
            }
        }
        return captured
    }

    private suspend fun recognizeLatinText(file: File): String {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            val image = InputImage.fromFilePath(context, Uri.fromFile(file))
            recognizer.process(image).await().text.trim()
        } catch (_: Exception) {
            ""
        } finally {
            recognizer.close()
        }
    }

    private fun mergeRecognition(first: String, second: String): String {
        if (first.isBlank()) return second.trim()
        if (second.isBlank()) return first.trim()
        val a = ContextIntelligence.normalize(first)
        val b = ContextIntelligence.normalize(second)
        if (a == b || a.contains(b)) return first.trim()
        if (b.contains(a)) return second.trim()
        return first.trim() + "\n" + second.trim()
    }

    private fun collectUris(intent: Intent): List<Uri> {
        val result = linkedSetOf<Uri>()
        @Suppress("DEPRECATION")
        if (intent.action == Intent.ACTION_SEND_MULTIPLE) {
            intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.let(result::addAll)
        } else {
            intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let(result::add)
        }
        val clip: ClipData? = intent.clipData
        if (clip != null) {
            for (i in 0 until clip.itemCount) clip.getItemAt(i).uri?.let(result::add)
        }
        return result.toList()
    }

    private fun looksLikeUrl(value: String): Boolean = runCatching {
        val uri = Uri.parse(value)
        uri.scheme == "http" || uri.scheme == "https"
    }.getOrDefault(false)

    private fun escape(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"")
}
