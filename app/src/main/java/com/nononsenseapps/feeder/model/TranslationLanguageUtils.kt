package com.nononsenseapps.feeder.model

import com.nononsenseapps.feeder.ai.model.TranslationLanguage
import org.jsoup.Jsoup
import java.util.Locale

private const val MAX_LANGUAGE_DETECTION_TEXT_LENGTH = 4000
private const val MIN_LANGUAGE_DETECTION_LETTERS = 20

/**
 * Shared helpers for language detection and target-language matching
 * used by feed-card translation and on-device translation.
 *
 * Ported from upstream master's TranslationLanguageUtils, simplified to
 * the generic (non-DeepL) comparison because target codes on this branch
 * are plain language tags or enum codes.
 */
internal fun prepareTextForLanguageDetection(
    content: String,
    preserveHtml: Boolean,
): String =
    prepareFullTextForLanguageDetection(
        content = content,
        preserveHtml = preserveHtml,
    ).take(MAX_LANGUAGE_DETECTION_TEXT_LENGTH)

internal fun prepareTextSamplesForLanguageDetection(
    content: String,
    preserveHtml: Boolean = false,
): List<String> {
    val text =
        prepareFullTextForLanguageDetection(
            content = content,
            preserveHtml = preserveHtml,
        )
    if (text.length <= MAX_LANGUAGE_DETECTION_TEXT_LENGTH) {
        return listOf(text)
    }

    val middleStart = ((text.length - MAX_LANGUAGE_DETECTION_TEXT_LENGTH) / 2).coerceAtLeast(0)
    val endStart = (text.length - MAX_LANGUAGE_DETECTION_TEXT_LENGTH).coerceAtLeast(0)
    return listOf(
        text.take(MAX_LANGUAGE_DETECTION_TEXT_LENGTH),
        text.substring(middleStart, middleStart + MAX_LANGUAGE_DETECTION_TEXT_LENGTH),
        text.substring(endStart),
    ).distinct()
}

private fun prepareFullTextForLanguageDetection(
    content: String,
    preserveHtml: Boolean,
): String =
    (
        if (preserveHtml) {
            Jsoup.parse(content).text()
        } else {
            content
        }
    ).replace(Regex("\\s+"), " ")
        .trim()

internal fun hasEnoughTextForLanguageDetection(content: String): Boolean = content.count(Char::isLetter) >= MIN_LANGUAGE_DETECTION_LETTERS

internal fun detectedLanguageMatchesTranslationTarget(
    detectedLanguage: String,
    targetLanguage: String,
): Boolean {
    val detected = detectedLanguage.asComparableDetectedLanguage() ?: return false
    val target = targetLanguage.asComparableTranslationTarget() ?: return false

    return detected.language == target.language &&
        (target.region == null || detected.region == target.region)
}

/**
 * Resolve the target language code used for cache keys and language
 * matching: the configured code, or the device locale tag for
 * DEVICE_DEFAULT.
 */
internal fun TranslationLanguage.resolvedTargetCode(): String =
    if (this == TranslationLanguage.DEVICE_DEFAULT) {
        Locale.getDefault().toLanguageTag()
    } else {
        code
    }

internal fun String?.isUsableCachedTranslation(
    original: String,
    sourceLanguage: String,
    targetLanguage: String,
): Boolean {
    if (isNullOrBlank()) {
        return false
    }
    if (this != original) {
        return true
    }
    return sourceLanguage.isBlank() ||
        detectedLanguageMatchesTranslationTarget(
            detectedLanguage = sourceLanguage,
            targetLanguage = targetLanguage,
        )
}

private data class ComparableTranslationLanguage(
    val language: String,
    val region: String? = null,
)

private fun String.asComparableDetectedLanguage(): ComparableTranslationLanguage? {
    val normalized = trim().replace('_', '-')
    if (normalized.isBlank()) {
        return null
    }

    val locale = Locale.forLanguageTag(normalized)
    val language =
        locale.language
            .takeUnless { it.isBlank() || it == "und" }
            ?: normalized.substringBefore('-').takeIf { it.isNotBlank() }
            ?: return null

    val region =
        locale.country
            .takeIf { it.isNotBlank() }
            ?: normalized
                .split('-')
                .drop(1)
                .firstOrNull { it.length == 2 || it.length == 3 }

    return ComparableTranslationLanguage(
        language = language.toCanonicalLanguageCode(),
        region = region?.uppercase(Locale.ROOT),
    )
}

private fun String.asComparableTranslationTarget(): ComparableTranslationLanguage? {
    val normalized = trim().replace('_', '-')
    if (normalized.isBlank()) {
        return null
    }

    val locale = Locale.forLanguageTag(normalized)
    val language =
        locale.language
            .takeUnless { it.isBlank() || it == "und" }
            ?: normalized.substringBefore('-').takeIf { it.isNotBlank() }
            ?: return null

    val region =
        locale.country
            .takeIf { it.isNotBlank() }
            ?: normalized
                .split('-')
                .drop(1)
                .firstOrNull { it.length == 2 || it.length == 3 }

    return ComparableTranslationLanguage(
        language = language.toCanonicalLanguageCode(),
        region = region?.uppercase(Locale.ROOT),
    )
}

private fun String.toCanonicalLanguageCode(): String =
    when (lowercase(Locale.ROOT)) {
        "nb" -> "no"
        else -> lowercase(Locale.ROOT)
    }
