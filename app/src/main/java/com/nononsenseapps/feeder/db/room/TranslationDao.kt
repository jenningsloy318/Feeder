package com.nononsenseapps.feeder.db.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Data Access Object for Translation entities.
 *
 * Provides methods for querying, inserting, and deleting translations.
 * Translations are cached by article ID and target language to avoid redundant API calls.
 */
@Dao
interface TranslationDao {
    /**
     * Get all translations for a specific article and target language.
     *
     * @param articleId The article ID
     * @param targetLanguage The target language code (e.g., "zh", "es")
     * @return List of translations ordered by paragraph index
     */
    @Query("SELECT * FROM translations WHERE article_id = :articleId AND target_language = :targetLanguage ORDER BY paragraph_index")
    suspend fun getTranslations(
        articleId: Long,
        targetLanguage: String,
    ): List<Translation>

    /**
     * Check if any translations exist for a specific article and target language.
     *
     * @param articleId The article ID
     * @param targetLanguage The target language code
     * @return The first translation if any exist, null otherwise
     */
    @Query("SELECT * FROM translations WHERE article_id = :articleId AND target_language = :targetLanguage ORDER BY paragraph_index LIMIT 1")
    suspend fun hasTranslations(
        articleId: Long,
        targetLanguage: String,
    ): Translation?

    /**
     * Insert a single translation into the database.
     *
     * @param translation The translation to insert
     * @return The row ID of the inserted translation
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(translation: Translation): Long

    /**
     * Insert multiple translations in a single transaction.
     *
     * @param translations List of translations to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(translations: List<Translation>)

    /**
     * Delete all translations for a specific article and target language.
     *
     * @param articleId The article ID
     * @param targetLanguage The target language code
     */
    @Query("DELETE FROM translations WHERE article_id = :articleId AND target_language = :targetLanguage")
    suspend fun delete(
        articleId: Long,
        targetLanguage: String,
    )

    /**
     * Delete all translations for a specific article (all languages).
     *
     * @param articleId The article ID
     */
    @Query("DELETE FROM translations WHERE article_id = :articleId")
    suspend fun deleteAll(articleId: Long)
}
