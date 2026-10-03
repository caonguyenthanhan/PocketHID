package dev.aleian.pockethid.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer

interface ISpeechRecognizer {
    fun setRecognitionListener(listener: RecognitionListener)
    fun cancel()
    fun destroy()
}

class AndroidSpeechRecognizer(val recognizer: SpeechRecognizer) : ISpeechRecognizer {
    override fun setRecognitionListener(listener: RecognitionListener) {
        recognizer.setRecognitionListener(listener)
    }
    override fun cancel() {
        recognizer.cancel()
    }
    override fun destroy() {
        recognizer.destroy()
    }
}

interface IVoicePlatform {
    fun isRecognitionAvailable(): Boolean
    fun createRecognizer(): ISpeechRecognizer?
    fun checkAudioPermission(): Boolean
    fun startListening(recognizer: ISpeechRecognizer, languageCode: String)
}

class AndroidVoicePlatform(private val context: Context) : IVoicePlatform {
    override fun isRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }
    override fun createRecognizer(): ISpeechRecognizer? {
        return SpeechRecognizer.createSpeechRecognizer(context)?.let { AndroidSpeechRecognizer(it) }
    }
    override fun checkAudioPermission(): Boolean {
        return androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    override fun startListening(recognizer: ISpeechRecognizer, languageCode: String) {
        val intent = Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, languageCode)
            putExtra("android.speech.extra.LANGUAGE_PREFERENCE", languageCode)
            putExtra("android.speech.extra.ONLY_RETURN_LANGUAGE_PREFERENCE", true)
            putExtra(android.speech.RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        if (recognizer as? AndroidSpeechRecognizer != null) {
            recognizer.recognizer.startListening(intent)
        }
    }
}
