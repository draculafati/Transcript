package com.transcript.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.transcript.core.screen.AccessibilityTextExtractor

/**
 * Android Accessibility Service enabling zero-OCR screen text extraction (ADR-0001, FR-4).
 * Extracts text directly from active view hierarchies without screenshot or GPU overhead.
 */
class TranscriptAccessibilityService : AccessibilityService() {

    private lateinit var textExtractor: AccessibilityTextExtractor

    override fun onCreate() {
        super.onCreate()
        instance = this
        textExtractor = AccessibilityTextExtractor {
            rootInActiveWindow
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Events are filtered; on-demand extraction is triggered via tap-to-translate
    }

    override fun onInterrupt() {
        // Clean up or handle service interruption
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    fun extractScreenText(): List<String> {
        return textExtractor.extractForegroundText()
    }

    companion object {
        @Volatile
        var instance: TranscriptAccessibilityService? = null
            private set

        fun isServiceEnabled(): Boolean = instance != null
    }
}
