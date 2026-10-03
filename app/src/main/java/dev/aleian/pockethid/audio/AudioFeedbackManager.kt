package dev.aleian.pockethid.audio

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object AudioFeedbackManager {
    private var controller: IAudioFeedbackController? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun init(context: Context) {
        if (controller == null) {
            controller = AudioFeedbackController(context.applicationContext)
        }
    }

    fun play(event: AudioEvent) {
        scope.launch {
            controller?.play(event)
        }
    }

    // For tests
    fun setController(newController: IAudioFeedbackController) {
        controller = newController
    }
}
