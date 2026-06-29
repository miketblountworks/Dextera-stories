package com.example.service.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class TtsManager(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false
    private var onInitSuccess: (() -> Unit)? = null

    private var onStartCallback: (() -> Unit)? = null
    private var onDoneCallback: (() -> Unit)? = null

    init {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                onStartCallback?.invoke()
            }

            override fun onDone(utteranceId: String?) {
                onDoneCallback?.invoke()
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                Log.e("TtsManager", "TTS Error: $utteranceId")
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                Log.e("TtsManager", "TTS Error: $utteranceId, Code: $errorCode")
            }
        })
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TtsManager", "English language is not supported or missing data.")
            } else {
                isInitialized = true
                onInitSuccess?.invoke()
                Log.d("TtsManager", "TTS initialized successfully in US English.")
            }
        } else {
            Log.e("TtsManager", "TTS Initialization failed.")
        }
    }

    fun setOnInitSuccessListener(listener: () -> Unit) {
        onInitSuccess = listener
        if (isInitialized) {
            listener()
        }
    }

    fun speak(text: String, onStart: () -> Unit = {}, onDone: () -> Unit = {}) {
        if (!isInitialized) {
            Log.w("TtsManager", "TTS not initialized yet. Queueing speech...")
            return
        }

        onStartCallback = onStart
        onDoneCallback = onDone

        val params = android.os.Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "storybook_page")

        // Speak page aloud, interrupt previous speech if active
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "storybook_page")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
