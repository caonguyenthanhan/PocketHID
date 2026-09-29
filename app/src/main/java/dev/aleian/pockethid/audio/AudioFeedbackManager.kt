package dev.aleian.pockethid.audio

import android.content.Context

object AudioFeedbackManager {
    private var controller: IAudioFeedbackController? = null

    fun init(context: Context) {
        if (controller == null) {
            controller = AudioFeedbackController(context.applicationContext)
        }
    }

    fun play(event: AudioEvent) {
        controller?.play(event)
    }

    // For tests
    fun setController(newController: IAudioFeedbackController) {
        controller = newController
    }
}
