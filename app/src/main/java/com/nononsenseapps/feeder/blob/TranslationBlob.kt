package com.nononsenseapps.feeder.blob

import com.nononsenseapps.feeder.ai.ArticleTranslation
import kotlinx.serialization.json.Json
import java.io.File
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

private val json = Json { ignoreUnknownKeys = true }

fun translationFile(
    itemId: Long,
    languageCode: String,
    translationsDir: File,
): File = File(translationsDir, "${itemId}_$languageCode.translation.json.gz")

fun loadTranslation(
    itemId: Long,
    languageCode: String,
    translationsDir: File,
): ArticleTranslation? {
    val file = translationFile(itemId, languageCode, translationsDir)
    if (!file.isFile) return null
    return try {
        GZIPInputStream(file.inputStream()).bufferedReader().use { reader ->
            json.decodeFromString<ArticleTranslation>(reader.readText())
        }
    } catch (e: Exception) {
        null
    }
}

fun saveTranslation(
    itemId: Long,
    languageCode: String,
    translationsDir: File,
    translation: ArticleTranslation,
) {
    translationsDir.mkdirs()
    val file = translationFile(itemId, languageCode, translationsDir)
    GZIPOutputStream(file.outputStream()).bufferedWriter().use { writer ->
        writer.write(json.encodeToString(ArticleTranslation.serializer(), translation))
    }
}

fun deleteTranslationCache(
    itemId: Long,
    translationsDir: File,
) {
    if (!translationsDir.isDirectory) return
    translationsDir
        .listFiles { _, name ->
            name.startsWith("${itemId}_") && name.endsWith(".translation.json.gz")
        }?.forEach { it.delete() }
}
