package com.nononsenseapps.feeder.ui.textaction

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import com.nononsenseapps.feeder.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Activity for handling Read Aloud text action from Android 13+ contextual toolbar.
 *
 * This activity is invoked when users select text and choose "Read Aloud" from the
 * system contextual toolbar. It receives the selected text via ACTION_PROCESS_TEXT
 * and uses Android's TextToSpeech to read it aloud.
 *
 * On Android 13+, the system bypasses custom ActionMode.Callback implementations,
 * so this activity provides a way for users to access the Read Aloud functionality
 * through the standard system menu.
 */
class ReadAloudActivity : Activity() {
    private var textToSpeech: TextToSpeech? = null
    private var ttsJob: Job? = null
    private var initializedState: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get the selected text from the intent
        val text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)

        if (text.isNullOrBlank()) {
            Log.e(LOG_TAG, "No text provided to ReadAloudActivity")
            finish()
            return
        }

        Log.d(LOG_TAG, "Received text for read aloud: ${text.take(50)}...")

        // Initialize TTS and speak the text
        initializeAndSpeak(text.toString())
    }

    private fun initializeAndSpeak(text: String) {
        ttsJob =
            CoroutineScope(Dispatchers.Main).launch {
                // Initialize TextToSpeech
                textToSpeech =
                    TextToSpeech(
                        this@ReadAloudActivity,
                    ) { status ->
                        initializedState = status
                        if (status != TextToSpeech.SUCCESS) {
                            Log.e(LOG_TAG, "Failed to initialize TextToSpeech: $status")
                            showToast(R.string.failed_to_load_text_to_speech)
                            finish()
                        }
                    }

                // Wait for initialization
                var attempts = 0
                while (initializedState == null && attempts < 50) {
                    delay(100)
                    attempts++
                }

                if (initializedState != TextToSpeech.SUCCESS) {
                    Log.e(LOG_TAG, "TextToSpeech initialization failed")
                    finish()
                    return@launch
                }

                // Set language to default
                val result = textToSpeech?.setLanguage(Locale.getDefault())
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(LOG_TAG, "Language not supported for TTS")
                    showToast(R.string.failed_to_load_text_to_speech)
                    finish()
                    return@launch
                }

                // Speak the text
                textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "read_aloud_utterance")

                // Wait a bit for speech to start, then finish
                delay(500)
                finish()
            }
    }

    private fun showToast(resourceId: Int) {
        Toast.makeText(this, resourceId, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsJob?.cancel()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
    }

    companion object {
        private const val LOG_TAG = "FeederReadAloudActivity"
    }
}
