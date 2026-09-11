package com.kareem.nexus.domain.intelligence

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.ImagePart
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Runtime state for the optional Gemini Nano layer. */
enum class OnDeviceAiState { READY, DOWNLOADABLE, DOWNLOADING, UNAVAILABLE, ERROR }

/**
 * Thin, fail-closed wrapper around ML Kit Prompt API / Gemini Nano.
 *
 * NEXUS never requires this service to work. Deterministic local intelligence remains the
 * baseline. On supported devices, Nano adds richer understanding and multimodal text recovery
 * without sending the user's content to a server.
 */
@Singleton
class OnDeviceAi @Inject constructor() {
    private val model by lazy { Generation.getClient() }
    private val gate = Mutex()
    @Volatile private var cachedState: OnDeviceAiState? = null

    suspend fun state(forceRefresh: Boolean = false): OnDeviceAiState {
        if (!forceRefresh) cachedState?.let { return it }
        return gate.withLock {
            if (!forceRefresh) cachedState?.let { return@withLock it }
            val value = runCatching {
                when (model.checkStatus()) {
                    FeatureStatus.AVAILABLE -> OnDeviceAiState.READY
                    FeatureStatus.DOWNLOADABLE -> OnDeviceAiState.DOWNLOADABLE
                    FeatureStatus.DOWNLOADING -> OnDeviceAiState.DOWNLOADING
                    else -> OnDeviceAiState.UNAVAILABLE
                }
            }.getOrElse { OnDeviceAiState.ERROR }
            cachedState = value
            value
        }
    }

    suspend fun generate(prompt: String): String? {
        if (prompt.isBlank() || state() != OnDeviceAiState.READY) return null
        return runCatching {
            model.generateContent(
                generateContentRequest(TextPart(prompt)) {
                    temperature = 0.1f
                    topK = 8
                    candidateCount = 1
                    maxOutputTokens = 900
                }
            ).candidates.firstOrNull()?.text?.trim()?.takeIf(String::isNotBlank)
        }.getOrNull()
    }

    /**
     * Expands how the user remembers something into a few nearby Arabic/English concepts.
     * This is only an optional second pass; literal/fuzzy search always remains available.
     */
    suspend fun expandSearchQuery(query: String): List<String> {
        val clean = query.trim()
        if (clean.length < 4) return emptyList()
        val output = generate(
            "You are a private on-device search helper. Expand this memory search query into up to 8 short " +
                "alternative phrases or keywords that could describe the same thing. Include useful Arabic and English " +
                "equivalents when relevant. Do not answer the query. Do not add facts. Return one phrase per line, " +
                "no numbering and no explanation. Query: $clean"
        ) ?: return emptyList()

        return output.lineSequence()
            .map { it.trim().trimStart('-', '•', '*').trim() }
            .filter { it.length in 2..80 }
            .filterNot { it.equals(clean, ignoreCase = true) }
            .distinctBy(ContextIntelligence::normalize)
            .take(8)
            .toList()
    }

    /**
     * Uses Nano's multimodal Prompt API as a second OCR/context pass. This is intentionally only
     * used for user-selected images and only when the system model is already available.
     */
    suspend fun extractVisibleText(file: File): String? {
        if (!file.exists() || state() != OnDeviceAiState.READY) return null
        val bitmap = decodeForModel(file) ?: return null
        return try {
            val response = model.generateContent(
                generateContentRequest(
                    ImagePart(bitmap),
                    TextPart(
                        "Transcribe all visible text in this image exactly. Preserve Arabic and English. " +
                            "Do not explain, summarize, translate, or invent text. Return only the visible text. " +
                            "If there is no readable text, return NO_TEXT."
                    ),
                ) {
                    temperature = 0.0f
                    topK = 1
                    candidateCount = 1
                    maxOutputTokens = 1400
                }
            )
            response.candidates.firstOrNull()?.text
                ?.trim()
                ?.takeIf { it.isNotBlank() && !it.equals("NO_TEXT", ignoreCase = true) }
        } catch (_: Exception) {
            null
        } finally {
            bitmap.recycle()
        }
    }

    private fun decodeForModel(file: File): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        val largest = maxOf(bounds.outWidth, bounds.outHeight)
        while (largest / sample > 1600) sample *= 2

        return BitmapFactory.decodeFile(
            file.path,
            BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            },
        )
    }
}
