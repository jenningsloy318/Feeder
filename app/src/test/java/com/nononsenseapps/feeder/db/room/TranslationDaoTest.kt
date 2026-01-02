package com.nononsenseapps.feeder.db.room

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nononsenseapps.feeder.ai.model.TargetLanguage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TranslationDaoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: AppDatabase
    private lateinit var translationDao: TranslationDao

    private val testArticleId = 1L
    private val testTargetLanguage = TargetLanguage.CHINESE.code

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).build()
        translationDao = db.translationDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `insert and retrieve translation`() = runTest {
        val translation = Translation(
            articleId = testArticleId,
            targetLanguage = testTargetLanguage,
            originalParagraph = "Hello world",
            translatedParagraph = "你好世界",
            paragraphIndex = 0,
            aiProvider = "openai",
            aiModel = "gpt-4",
        )

        translationDao.insert(translation)

        val retrieved = translationDao.getTranslations(testArticleId, testTargetLanguage)

        assertEquals(1, retrieved.size)
        assertEquals("Hello world", retrieved[0].originalParagraph)
        assertEquals("你好世界", retrieved[0].translatedParagraph)
    }

    @Test
    fun `insert multiple translations for same article`() = runTest {
        val translations = listOf(
            Translation(
                articleId = testArticleId,
                targetLanguage = testTargetLanguage,
                originalParagraph = "First",
                translatedParagraph = "第一",
                paragraphIndex = 0,
                aiProvider = "openai",
                aiModel = "gpt-4",
            ),
            Translation(
                articleId = testArticleId,
                targetLanguage = testTargetLanguage,
                originalParagraph = "Second",
                translatedParagraph = "第二",
                paragraphIndex = 1,
                aiProvider = "openai",
                aiModel = "gpt-4",
            ),
        )

        translationDao.insertAll(translations)

        val retrieved = translationDao.getTranslations(testArticleId, testTargetLanguage)

        assertEquals(2, retrieved.size)
        assertEquals("First", retrieved[0].originalParagraph)
        assertEquals("Second", retrieved[1].originalParagraph)
    }

    @Test
    fun `delete translations for specific language`() = runTest {
        val translation = Translation(
            articleId = testArticleId,
            targetLanguage = testTargetLanguage,
            originalParagraph = "Hello",
            translatedParagraph = "你好",
            paragraphIndex = 0,
            aiProvider = "openai",
            aiModel = "gpt-4",
        )

        translationDao.insert(translation)
        translationDao.delete(testArticleId, testTargetLanguage)

        val retrieved = translationDao.getTranslations(testArticleId, testTargetLanguage)

        assertTrue(retrieved.isEmpty())
    }

    @Test
    fun `delete all translations for article`() = runTest {
        val chineseTranslation = Translation(
            articleId = testArticleId,
            targetLanguage = TargetLanguage.CHINESE.code,
            originalParagraph = "Hello",
            translatedParagraph = "你好",
            paragraphIndex = 0,
            aiProvider = "openai",
            aiModel = "gpt-4",
        )

        val spanishTranslation = Translation(
            articleId = testArticleId,
            targetLanguage = TargetLanguage.SPANISH.code,
            originalParagraph = "Hello",
            translatedParagraph = "Hola",
            paragraphIndex = 0,
            aiProvider = "openai",
            aiModel = "gpt-4",
        )

        translationDao.insert(chineseTranslation)
        translationDao.insert(spanishTranslation)
        translationDao.deleteAll(testArticleId)

        val chineseRetrieved = translationDao.getTranslations(testArticleId, TargetLanguage.CHINESE.code)
        val spanishRetrieved = translationDao.getTranslations(testArticleId, TargetLanguage.SPANISH.code)

        assertTrue(chineseRetrieved.isEmpty())
        assertTrue(spanishRetrieved.isEmpty())
    }

    @Test
    fun `getTranslations returns empty list when no translations exist`() = runTest {
        val retrieved = translationDao.getTranslations(testArticleId, testTargetLanguage)

        assertTrue(retrieved.isEmpty())
    }
}
