package com.nononsenseapps.feeder.ui.textaction

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.nononsenseapps.feeder.R

/**
 * Activity for handling Translate text action from Android 13+ contextual toolbar.
 *
 * This activity is invoked when users select text and choose "Translate" from the
 * system contextual toolbar. It receives the selected text via ACTION_PROCESS_TEXT.
 *
 * Note: This is a placeholder implementation for Android 13+ text selection.
 * The full translation feature requires integration with the ArticleViewModel
 * and AI translation service, which is designed for translating entire articles,
 * not arbitrary selected text.
 *
 * Future enhancement: Implement proper translation of selected text using the AI service.
 */
class TranslateActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get the selected text from the intent
        val text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)

        if (text.isNullOrBlank()) {
            Log.e(LOG_TAG, "No text provided to TranslateActivity")
            finish()
            return
        }

        Log.d(LOG_TAG, "Received text for translation: ${text.take(50)}...")

        // Show a toast message indicating translation feature
        // This is a placeholder - full implementation would use the AI translation service
        Toast.makeText(
            this,
            getString(R.string.translation_placeholder_message, text.take(50)),
            Toast.LENGTH_LONG,
        ).show()

        finish()
    }

    companion object {
        private const val LOG_TAG = "FeederTranslateActivity"
    }
}
