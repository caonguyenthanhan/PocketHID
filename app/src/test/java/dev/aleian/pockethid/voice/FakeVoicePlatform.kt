package dev.aleian.pockethid.voice

import android.speech.RecognitionListener

class FakeVoicePlatform(
    var available: Boolean = true,
    var hasPermission: Boolean = true
) : IVoicePlatform {
    var listener: RecognitionListener? = null
    var started = false
    var cancelled = false
    var destroyed = false
    var languageCode: String? = null

    override fun isRecognitionAvailable(): Boolean = available

    override fun createRecognizer(): ISpeechRecognizer? {
        if (!available) return null
        return object : ISpeechRecognizer {
            override fun setRecognitionListener(l: RecognitionListener) {
                listener = l
            }
            override fun cancel() {
                cancelled = true
            }
            override fun destroy() {
                destroyed = true
            }
        }
    }

    override fun checkAudioPermission(): Boolean = hasPermission
    
    override fun startListening(recognizer: ISpeechRecognizer, languageCode: String) {
        started = true
        this.languageCode = languageCode
    }
}
