package dev.aleian.pockethid.haptic

import android.content.Context

object HapticFeedbackManager {
    private var controller: IHapticFeedbackController? = null

    fun init(context: Context) {
        if (controller == null) {
            controller = HapticFeedbackController(context.applicationContext)
        }
    }

    fun play(event: HapticEvent) {
        controller?.play(event)
    }

    // For tests
    fun setController(newController: IHapticFeedbackController) {
        controller = newController
    }
}
