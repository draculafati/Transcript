package com.transcript.core.translation

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await
import java.util.concurrent.atomic.AtomicBoolean

/**
 * On-Device neural machine translation engine using Google ML Kit (ADR-0001).
 * Operates with zero API costs, full local offline capability, and low latency (<100ms).
 */
class MlKitOnDeviceTranslator(
    private var sourceLanguage: String = TranslateLanguage.ENGLISH,
    private var targetLanguage: String = TranslateLanguage.HINDI
) {

    private var translator: Translator? = null
    private val isModelDownloaded = AtomicBoolean(false)

    init {
        createTranslator()
    }

    private fun createTranslator() {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLanguage)
            .setTargetLanguage(targetLanguage)
            .build()
        translator = Translation.getClient(options)
    }

    suspend fun prepareModel(requireWifi: Boolean = false): Boolean {
        val client = translator ?: return false
        val conditions = DownloadConditions.Builder().apply {
            if (requireWifi) {
                requireWifi()
            }
        }.build()

        return try {
            client.downloadModelIfNeeded(conditions).await()
            isModelDownloaded.set(true)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun translate(text: String): String {
        if (text.isBlank()) return ""

        val client = translator ?: return text

        return try {
            if (!isModelDownloaded.get()) {
                val prepared = prepareModel()
                if (!prepared) {
                    return text // Return source text if offline model is unavailable
                }
            }
            client.translate(text).await()
        } catch (e: Exception) {
            text
        }
    }

    fun setLanguages(source: String, target: String) {
        if (sourceLanguage != source || targetLanguage != target) {
            release()
            sourceLanguage = source
            targetLanguage = target
            isModelDownloaded.set(false)
            createTranslator()
        }
    }

    fun release() {
        translator?.close()
        translator = null
        isModelDownloaded.set(false)
    }
}
