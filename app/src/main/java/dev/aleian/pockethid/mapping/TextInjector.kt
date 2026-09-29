package dev.aleian.pockethid.mapping

import dev.aleian.pockethid.action.ActionDispatcher
import dev.aleian.pockethid.action.ActionExecutionPlan
import dev.aleian.pockethid.transport.InputTransport
import kotlinx.coroutines.delay

object TextInjector {
    /**
     * Injects a sequence of characters into the transport using the standard mapping.
     */
    suspend fun injectText(
        text: String,
        transport: InputTransport?,
        pasteDelayMs: Long
    ) {
        if (transport == null) return
        val strokes = TextInputResolver.resolveText(text)
        for (stroke in strokes) {
            ActionDispatcher.execute(
                ActionExecutionPlan.KeyStroke(stroke.keyCode, stroke.modifiers),
                transport
            )
            if (pasteDelayMs > 0) {
                delay(pasteDelayMs)
            }
        }
    }
}
