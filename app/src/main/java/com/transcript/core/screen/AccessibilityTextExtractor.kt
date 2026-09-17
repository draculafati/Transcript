package com.transcript.core.screen

import android.graphics.Bitmap
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

/**
 * Zero-OCR Native Text Extraction utilizing Android's AccessibilityNodeInfo tree (ADR-0001).
 * Extracts foreground strings directly from view hierarchies without camera or screenshot computation.
 * Includes Google ML Kit on-device OCR fallback for non-text views (video frames/images).
 */
class AccessibilityTextExtractor(
    private val rootNodeProvider: () -> AccessibilityNodeInfo?
) : ITextExtractor {

    private val textRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    override fun extractForegroundText(): List<String> {
        val rootNode = rootNodeProvider() ?: return emptyList()
        val extractedStrings = mutableListOf<String>()
        val visitedNodes = mutableSetOf<Int>()

        try {
            traverseNode(rootNode, extractedStrings, visitedNodes, currentDepth = 0, maxDepth = 25)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return extractedStrings.distinct().filter { it.isNotBlank() }
    }

    private fun traverseNode(
        node: AccessibilityNodeInfo?,
        results: MutableList<String>,
        visited: MutableSet<Int>,
        currentDepth: Int,
        maxDepth: Int
    ) {
        if (node == null || currentDepth > maxDepth || results.size >= MAX_TEXT_ELEMENTS) return

        val hashCode = node.hashCode()
        if (visited.contains(hashCode)) return
        visited.add(hashCode)

        // 1. Extract text from the current view
        val text = node.text?.toString()?.trim()
        if (!text.isNullOrBlank() && text.length > 1) {
            results.add(text)
        }

        // 2. Extract content description (useful for icon buttons, accessible labels)
        val contentDesc = node.contentDescription?.toString()?.trim()
        if (!contentDesc.isNullOrBlank() && contentDesc.length > 1 && contentDesc != text) {
            results.add(contentDesc)
        }

        // 3. Traverse child nodes recursively
        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i)
            if (child != null) {
                traverseNode(child, results, visited, currentDepth + 1, maxDepth)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    @Suppress("DEPRECATION")
                    child.recycle()
                }
            }
        }
    }

    /**
     * Secondary Fallback (ADR-0001 Detail 4 / FR-5):
     * Visual OCR run only when triggered for non-text views (video frames, canvas, burned-in subtitles).
     */
    suspend fun extractFromBitmapFallback(bitmap: Bitmap): List<String> {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val visionText = textRecognizer.process(image).await()
            visionText.textBlocks.map { it.text.trim() }.filter { it.isNotBlank() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        private const val MAX_TEXT_ELEMENTS = 100
    }
}
