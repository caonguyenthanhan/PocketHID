package dev.aleian.pockethid.haptic

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object HapticFeedbackManager {
    private var controller: IHapticFeedbackController? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun init(context: Context) {
        if (controller == null) {
            controller = HapticFeedbackController(context.applicationContext)
        }
    }

    fun play(event: HapticEvent) {
        scope.launch {
            controller?.play(event)
        }
    }

    // For tests
    fun setController(newController: IHapticFeedbackController) {
        controller = newController
    }
}
