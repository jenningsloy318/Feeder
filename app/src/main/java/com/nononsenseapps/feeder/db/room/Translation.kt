package com.nononsenseapps.feeder.db.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nononsenseapps.feeder.db.COL_ID
import java.time.Instant

/**
 * Room entity for storing AI-powered translations of article paragraphs.
 *
 * Translations are cached to avoid re-translating the same content.
 * Each translation represents a single paragraph of an article in a target language.
 *
 * @property id Primary key (auto-increment)
 * @property articleId ID of the article this translation belongs to
 * @property targetLanguage Target language code (ISO 639-1, e.g., "zh", "es")
 * @property originalParagraph The original paragraph text before translation
 * @property translatedParagraph The translated paragraph text
 * @property paragraphIndex Index of the paragraph in the article (for ordering)
 * @property aiProvider AI provider used ("openai" or "anthropic")
 * @property aiModel Model ID used for translation (e.g., "gpt-4o", "claude-3-5-sonnet")
 * @property createdAt Timestamp when translation was created
 */
@Entity(
    tableName = "translations",
    indices = [
        Index(value = ["article_id", "target_language"]),
        Index(value = ["article_id"]),
    ],
)
data class Translation(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = COL_ID)
    val id: Long = 0,

    @ColumnInfo(name = "article_id")
    val articleId: Long,

    @ColumnInfo(name = "target_language")
    val targetLanguage: String,

    @ColumnInfo(name = "original_paragraph")
    val originalParagraph: String,

    @ColumnInfo(name = "translated_paragraph")
    val translatedParagraph: String,

    @ColumnInfo(name = "paragraph_index")
    val paragraphIndex: Int,

    @ColumnInfo(name = "ai_provider")
    val aiProvider: String,

    @ColumnInfo(name = "ai_model")
    val aiModel: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
)
