package dev.aleian.pockethid.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class VoiceState {
    IDLE,
    LISTENING,
    PROCESSING,
    RESULT,
    ERROR,
    UNAVAILABLE
}

class VoiceInputController(
    private val platform: IVoicePlatform
) : RecognitionListener {

    private val _state = MutableStateFlow(VoiceState.IDLE)
    val state: StateFlow<VoiceState> = _state.asStateFlow()

    private val _transcript = MutableStateFlow("")
    val transcript: StateFlow<String> = _transcript.asStateFlow()

    private var speechRecognizer: ISpeechRecognizer? = null

    init {
        initializeRecognizer()
    }

    private fun initializeRecognizer() {
        if (!platform.isRecognitionAvailable()) {
            _state.value = VoiceState.UNAVAILABLE
        } else {
            speechRecognizer = platform.createRecognizer()
            speechRecognizer?.setRecognitionListener(this)
        }
    }

    fun startListening(languageCode: String = "en-US") {
        if (_state.value == VoiceState.UNAVAILABLE) return
        
        if (!platform.checkAudioPermission()) {
            _state.value = VoiceState.ERROR
            return
        }

        _transcript.value = ""
        _state.value = VoiceState.LISTENING

        try {
            speechRecognizer?.let { platform.startListening(it, languageCode) }
        } catch (e: Exception) {
            _state.value = VoiceState.ERROR
        }
    }

    fun cancel() {
        if (_state.value == VoiceState.UNAVAILABLE) return
        speechRecognizer?.cancel()
        _state.value = VoiceState.IDLE
        _transcript.value = ""
    }

    fun cleanup() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        _state.value = VoiceState.IDLE
    }

    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        if (_state.value == VoiceState.LISTENING) {
            _state.value = VoiceState.PROCESSING
        }
    }

    override fun onError(error: Int) {
        if (_state.value == VoiceState.UNAVAILABLE) return
        
        // Handle common cancellation or no-match errors gracefully
        if (error == SpeechRecognizer.ERROR_CLIENT || error == SpeechRecognizer.ERROR_NO_MATCH) {
            _state.value = VoiceState.IDLE
        } else {
            _state.value = VoiceState.ERROR
        }
    }

    override fun onResults(results: Bundle?) {
        if (_state.value == VoiceState.UNAVAILABLE) return

        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull()?.trim() ?: ""
        
        processResultText(text)
    }

    fun processResultText(text: String) {
        if (_state.value == VoiceState.UNAVAILABLE) return
        
        if (text.isEmpty()) {
            _state.value = VoiceState.ERROR
        } else {
            _transcript.value = text
            _state.value = VoiceState.RESULT
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull() ?: ""
        _transcript.value = text
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}
}
