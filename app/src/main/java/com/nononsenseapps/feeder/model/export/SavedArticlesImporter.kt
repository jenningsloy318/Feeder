package com.nononsenseapps.feeder.model.export

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import com.nononsenseapps.feeder.R
import com.nononsenseapps.feeder.db.room.FeedItemDao
import com.nononsenseapps.feeder.util.Either
import com.nononsenseapps.feeder.util.ToastMaker
import com.nononsenseapps.feeder.util.logDebug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.kodein.di.DI
import org.kodein.di.direct
import org.kodein.di.instance
import kotlin.system.measureTimeMillis

private const val LOG_TAG = "FEEDER_SAVEDARTIMPORT"

/**
 * Result of a saved articles import operation.
 *
 * @property importedCount Number of articles successfully imported
 * @property totalCount Total number of URLs in file
 * @property skippedCount Number of URLs not found in database (totalCount - importedCount)
 */
data class ImportResult(
    val importedCount: Int,
    val totalCount: Int,
    val skippedCount: Int = totalCount - importedCount,
)

/**
 * Imports saved articles from a plain text file.
 *
 * The file should contain one URL per line (matching the export format).
 * Empty lines and whitespace-only lines are ignored.
 * Duplicate URLs are removed before processing.
 *
 * @param di Dependency injection container
 * @param uri URI of the file to import
 * @return Either<SavedArticlesImportError, ImportResult> - Left on error, Right on success
 */
suspend fun importSavedArticles(
    di: DI,
    uri: Uri,
): Either<SavedArticlesImportError, ImportResult> =
    Either.catching(
        onCatch = {
            Log.e(LOG_TAG, "Failed to import saved articles", it)
            val toastMaker = di.direct.instance<ToastMaker>()
            toastMaker.makeToast(R.string.failed_to_import_saved_articles)
            (it.localizedMessage ?: it.message)?.let { message ->
                toastMaker.makeToast(message)
            }

            SavedArticlesImportUnknownError(it)
        },
    ) {
        withContext(Dispatchers.IO) {
            val contentResolver: ContentResolver by di.instance()
            val feedItemDao: FeedItemDao by di.instance()

            // Step 1: Read URLs from file
            val urls = readUrlsFromFile(contentResolver, uri)

            // Prepare result
            val result: ImportResult
            val time = measureTimeMillis {
                if (urls.isNotEmpty()) {
                    // Step 2: Batch query to find matching feed items
                    val idPairs = feedItemDao.getFeedItemIdsByLinks(urls)

                    // Step 3: Batch update to mark as bookmarked
                    val ids = idPairs.map { it.id }
                    val importedCount =
                        if (ids.isNotEmpty()) {
                            feedItemDao.setBookmarked(ids)
                        } else {
                            0
                        }

                    // Step 4: Show success message
                    val toastMaker = di.direct.instance<ToastMaker>()
                    val message = getImportedCountMessage(importedCount, urls.size)
                    toastMaker.makeToast(message)

                    // Prepare result for return
                    result = ImportResult(
                        importedCount = importedCount,
                        totalCount = urls.size,
                    )
                } else {
                    val toastMaker = di.direct.instance<ToastMaker>()
                    toastMaker.makeToast(R.string.no_valid_urls_in_file)

                    // Prepare result for return
                    result = ImportResult(0, 0)
                }
            }

            logDebug(LOG_TAG, "Imported saved articles in $time ms on ${Thread.currentThread().name}")

            // Return the result
            result
        }
    }

/**
 * Reads and parses URLs from a text file.
 *
 * @param contentResolver ContentResolver for file access
 * @param uri URI of the file to read
 * @return List of unique, non-empty URLs
 */
private suspend fun readUrlsFromFile(
    contentResolver: ContentResolver,
    uri: Uri,
): List<String> = withContext(Dispatchers.IO) {
    contentResolver.openInputStream(uri)?.use { inputStream ->
        inputStream.bufferedReader().use { reader ->
            reader.readLines()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct() // Remove duplicates
        }
    } ?: throw Exception("Could not open file: $uri")
}

/**
 * Sealed class representing import errors.
 */
sealed class SavedArticlesImportError {
    abstract val throwable: Throwable?
}

/**
 * Error thrown for unknown import failures.
 */
data class SavedArticlesImportUnknownError(
    override val throwable: Throwable,
) : SavedArticlesImportError()

/**
 * Generates appropriate message for import results.
 */
private fun getImportedCountMessage(
    importedCount: Int,
    totalCount: Int,
): String {
    val skippedCount = totalCount - importedCount
    return when {
        importedCount == 0 -> "No matching articles found in database"
        skippedCount == 0 -> "Imported $importedCount saved articles"
        else -> "Imported $importedCount of $totalCount saved articles ($skippedCount not found)"
    }
}
