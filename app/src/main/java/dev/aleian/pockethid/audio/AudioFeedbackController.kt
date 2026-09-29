package dev.aleian.pockethid.audio

import android.content.Context
import android.media.AudioManager
import dev.aleian.pockethid.model.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

enum class AudioEvent {
    ACTION_ACCEPTED,
    MODE_CHANGED,
    CONNECTED,
    DISCONNECTED,
    ERROR,
    VOICE_LISTENING_STARTED,
    VOICE_LISTENING_STOPPED,
    VOICE_RESULT,
    VOICE_ERROR
}

interface IAudioFeedbackController {
    fun play(event: AudioEvent)
    fun release()
}

class AudioFeedbackController(private val context: Context) : IAudioFeedbackController {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    override fun play(event: AudioEvent) {
        val settings = SettingsRepository.settings.value
        if (!settings.soundEffectsEnabled) return

        val effectId = when (event) {
            AudioEvent.ACTION_ACCEPTED -> AudioManager.FX_KEY_CLICK
            AudioEvent.MODE_CHANGED -> AudioManager.FX_FOCUS_NAVIGATION_UP
            AudioEvent.CONNECTED -> AudioManager.FX_KEYPRESS_RETURN
            AudioEvent.DISCONNECTED -> AudioManager.FX_KEYPRESS_DELETE
            AudioEvent.ERROR -> AudioManager.FX_KEYPRESS_INVALID
            AudioEvent.VOICE_LISTENING_STARTED -> AudioManager.FX_FOCUS_NAVIGATION_UP
            AudioEvent.VOICE_LISTENING_STOPPED -> AudioManager.FX_FOCUS_NAVIGATION_DOWN
            AudioEvent.VOICE_RESULT -> AudioManager.FX_KEYPRESS_STANDARD
            AudioEvent.VOICE_ERROR -> AudioManager.FX_KEYPRESS_INVALID
        }

        audioManager?.playSoundEffect(effectId)
    }

    override fun release() {
        // AudioManager effects don't require manual release, but interface allows for Future expansions
    }
}
