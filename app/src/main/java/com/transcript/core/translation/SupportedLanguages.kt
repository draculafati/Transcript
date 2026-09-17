package com.transcript.core.translation

import com.google.mlkit.nl.translate.TranslateLanguage

data class LanguageItem(
    val code: String,
    val displayName: String,
    val nativeName: String
)

/**
 * Comprehensive registry of supported languages for on-device translation
 * using Google ML Kit and Whisper models (ADR-0001).
 */
object SupportedLanguages {

    val ALL: List<LanguageItem> = listOf(
        LanguageItem(TranslateLanguage.ENGLISH, "English", "English"),
        LanguageItem(TranslateLanguage.KOREAN, "Korean", "한국어 (Hangul)"),
        LanguageItem(TranslateLanguage.JAPANESE, "Japanese", "日本語"),
        LanguageItem(TranslateLanguage.CHINESE, "Chinese", "中文"),
        LanguageItem(TranslateLanguage.SPANISH, "Spanish", "Español"),
        LanguageItem(TranslateLanguage.FRENCH, "French", "Français"),
        LanguageItem(TranslateLanguage.GERMAN, "German", "Deutsch"),
        LanguageItem(TranslateLanguage.HINDI, "Hindi", "हिन्दी"),
        LanguageItem(TranslateLanguage.URDU, "Urdu", "اردو"),
        LanguageItem(TranslateLanguage.ARABIC, "Arabic", "العربية"),
        LanguageItem(TranslateLanguage.RUSSIAN, "Russian", "Русский"),
        LanguageItem(TranslateLanguage.PORTUGUESE, "Portuguese", "Português"),
        LanguageItem(TranslateLanguage.ITALIAN, "Italian", "Italiano"),
        LanguageItem(TranslateLanguage.TURKISH, "Turkish", "Türkçe"),
        LanguageItem(TranslateLanguage.VIETNAMESE, "Vietnamese", "Tiếng Việt"),
        LanguageItem(TranslateLanguage.BENGALI, "Bengali", "বাংলা"),
        LanguageItem(TranslateLanguage.INDONESIAN, "Indonesian", "Bahasa Indonesia"),
        LanguageItem(TranslateLanguage.THAI, "Thai", "ไทย"),
        LanguageItem(TranslateLanguage.DUTCH, "Dutch", "Nederlands"),
        LanguageItem(TranslateLanguage.POLISH, "Polish", "Polski"),
        LanguageItem(TranslateLanguage.SWEDISH, "Swedish", "Svenska")
    )

    fun getByCode(code: String): LanguageItem {
        return ALL.firstOrNull { it.code == code } ?: LanguageItem(TranslateLanguage.ENGLISH, "English", "English")
    }
}
