package dev.aleian.pockethid.action

import dev.aleian.pockethid.transport.InputTransport
import kotlinx.coroutines.delay
import dev.aleian.pockethid.audio.AudioFeedbackManager
import dev.aleian.pockethid.audio.AudioEvent
import dev.aleian.pockethid.haptic.HapticFeedbackManager
import dev.aleian.pockethid.haptic.HapticEvent

/**
 * Coordinates execution of [ActionExecutionPlan]s and [PocketAction]s onto an [InputTransport].
 */
object ActionDispatcher {

    /**
     * Resolves and executes a high-level [PocketAction] using the given [hostOs].
     */
    suspend fun dispatch(
        action: PocketAction,
        transport: InputTransport?,
        hostOs: HostOs = HostOs.WINDOWS,
        zoomMode: String = "Wheel",
        playFeedback: Boolean = true
    ): Boolean {
        if (transport == null) return false
        val plan = ActionResolver.resolve(action, hostOs, zoomMode)
        val success = execute(plan, transport)
        if (success && playFeedback) {
            AudioFeedbackManager.play(AudioEvent.ACTION_ACCEPTED)
            HapticFeedbackManager.play(HapticEvent.ACTION_ACCEPTED)
        }
        return success
    }

    /**
     * Executes a concrete low-level [ActionExecutionPlan] directly onto [transport].
     */
    suspend fun execute(
        plan: ActionExecutionPlan,
        transport: InputTransport
    ): Boolean {
        return when (plan) {
            is ActionExecutionPlan.KeyStroke -> {
                transport.sendKeyClick(plan.keyCode, plan.modifiers)
                true
            }

            is ActionExecutionPlan.ConsumerKey -> {
                transport.sendConsumerClick(plan.usageCode)
            }

            is ActionExecutionPlan.MouseButtonClick -> {
                transport.sendMouseClick(plan.buttonMask)
                true
            }

            is ActionExecutionPlan.GamepadButtonClick -> {
                transport.sendGamepadReport(
                    buttons = plan.buttonMask,
                    hat = dev.aleian.pockethid.model.HidConstants.GAMEPAD_HAT_CENTERED,
                    leftX = 0,
                    leftY = 0,
                    rightX = 0,
                    rightY = 0,
                    leftTrigger = 0,
                    rightTrigger = 0
                )
                delay(12L)
                transport.sendGamepadNeutral()
                true
            }

            is ActionExecutionPlan.ZoomWheel -> {
                // Press modifier (Ctrl or Cmd)
                transport.sendKeyPress(dev.aleian.pockethid.model.HidConstants.KEY_NONE, plan.modifier)
                delay(10L)
                // Emit mouse wheel step (+1 for Zoom In, -1 for Zoom Out)
                transport.sendMouseMove(0, 0, dev.aleian.pockethid.model.HidConstants.MOUSE_BUTTON_NONE, plan.wheelDelta)
                delay(10L)
                // Release modifier
                transport.sendKeyRelease()
                true
            }

            is ActionExecutionPlan.Sequence -> {
                var allSuccess = true
                for (step in plan.steps) {
                    val stepOk = execute(step, transport)
                    if (!stepOk) allSuccess = false
                    if (plan.stepDelayMs > 0) {
                        delay(plan.stepDelayMs)
                    }
                }
                allSuccess
            }

            is ActionExecutionPlan.NoOp -> true
        }
    }
}
