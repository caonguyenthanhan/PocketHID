package dev.aleian.pockethid.mapping

import dev.aleian.pockethid.model.HidConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt

data class MouseAction(
    val dx: Int = 0,
    val dy: Int = 0,
    val buttons: Byte = HidConstants.MOUSE_BUTTON_NONE,
    val wheel: Int = 0,
    val panH: Int = 0
)

enum class GestureType {
    NONE,
    ONE_FINGER_MOVE,
    ONE_FINGER_TAP,
    ONE_FINGER_DOUBLE_TAP,
    ONE_FINGER_DRAG,
    TWO_FINGER_SCROLL_V,
    TWO_FINGER_SCROLL_H,
    TWO_FINGER_TAP,
    TWO_FINGER_DRAG,
    TWO_FINGER_PINCH_OUT,
    TWO_FINGER_PINCH_IN,
    THREE_FINGER_TAP,
    THREE_FINGER_SWIPE_LEFT,
    THREE_FINGER_SWIPE_RIGHT,
    THREE_FINGER_SWIPE_UP,
    THREE_FINGER_SWIPE_DOWN,
    FOUR_FINGER_TAP,
    FOUR_FINGER_SWIPE_LEFT,
    FOUR_FINGER_SWIPE_RIGHT,
    FOUR_FINGER_SWIPE_UP,
    FOUR_FINGER_SWIPE_DOWN
}

/**
 * Maps a discrete [GestureType] to its default semantic [dev.aleian.pockethid.action.PocketAction].
 */
fun GestureType.toDefaultAction(): dev.aleian.pockethid.action.PocketAction? = when (this) {
    GestureType.ONE_FINGER_TAP -> dev.aleian.pockethid.action.PocketAction.PointerAction.LeftClick
    GestureType.ONE_FINGER_DOUBLE_TAP -> dev.aleian.pockethid.action.PocketAction.PointerAction.DoubleLeftClick
    GestureType.TWO_FINGER_TAP -> dev.aleian.pockethid.action.PocketAction.PointerAction.RightClick
    GestureType.TWO_FINGER_PINCH_OUT -> dev.aleian.pockethid.action.PocketAction.SystemAction.ZoomIn
    GestureType.TWO_FINGER_PINCH_IN -> dev.aleian.pockethid.action.PocketAction.SystemAction.ZoomOut
    GestureType.THREE_FINGER_TAP -> dev.aleian.pockethid.action.PocketAction.PointerAction.MiddleClick
    GestureType.THREE_FINGER_SWIPE_LEFT -> dev.aleian.pockethid.action.PocketAction.NavAction.DesktopPrevious
    GestureType.THREE_FINGER_SWIPE_RIGHT -> dev.aleian.pockethid.action.PocketAction.NavAction.DesktopNext
    GestureType.THREE_FINGER_SWIPE_UP -> dev.aleian.pockethid.action.PocketAction.SystemAction.TaskView
    GestureType.THREE_FINGER_SWIPE_DOWN -> dev.aleian.pockethid.action.PocketAction.SystemAction.ShowDesktop
    GestureType.FOUR_FINGER_SWIPE_LEFT -> dev.aleian.pockethid.action.PocketAction.SystemAction.AppSwitcherPrev
    GestureType.FOUR_FINGER_SWIPE_RIGHT -> dev.aleian.pockethid.action.PocketAction.SystemAction.AppSwitcherNext
    GestureType.FOUR_FINGER_SWIPE_UP -> dev.aleian.pockethid.action.PocketAction.SystemAction.TaskView
    GestureType.FOUR_FINGER_SWIPE_DOWN -> dev.aleian.pockethid.action.PocketAction.SystemAction.ShowDesktop
    else -> null
}

data class GestureDiagnostics(
    val gestureType: GestureType = GestureType.NONE,
    val pointerCount: Int = 0,
    val actionDescription: String = "IDLE",
    val outputShortcut: String = "None",
    val timestamp: Long = System.currentTimeMillis()
)

object GestureDiagnosticsHub {
    private val _diagnostics = MutableStateFlow(GestureDiagnostics())
    val diagnostics: StateFlow<GestureDiagnostics> = _diagnostics.asStateFlow()

    fun record(gestureType: GestureType, pointerCount: Int, actionDesc: String, output: String) {
        _diagnostics.value = GestureDiagnostics(
            gestureType = gestureType,
            pointerCount = pointerCount,
            actionDescription = actionDesc,
            outputShortcut = output,
            timestamp = System.currentTimeMillis()
        )
    }
}

enum class TwoFingerMode {
    UNDECIDED,
    SCROLLING,
    PINCHING
}

class GestureInterpreter(
    var accelerationFactor: Float = 1.5f,
    var scrollSensitivity: Float = 30.0f,
    var naturalScroll: Boolean = false,
    var deadZonePx: Float = 4.0f,
    var pinchZoomEnabled: Boolean = true,
    var pinchThresholdPx: Float = 35.0f,
    var pinchStepThresholdPx: Float = 36.0f,
    var pinchRateLimitMs: Long = 85L
) {
    private var accumulatedScrollY = 0f
    private var accumulatedScrollX = 0f
    private var isDragging = false

    // State machine tracking
    private var touchDownTime = 0L
    private var lastTapTime = 0L
    private var startX = 0f
    private var startY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var maxPointers = 0
    private var hasMovedBeyondDeadzone = false
    private var isMultiFingerLocked = false
    private var hasFiredSwipe = false
    private var isTapAndHoldActive = false

    // Two-finger pinch / scroll state
    private var twoFingerMode = TwoFingerMode.UNDECIDED
    private var initialSpan = 0f
    private var lastSpan = 0f
    private var accumulatedPinchDelta = 0f
    private var initialCenterX = 0f
    private var initialCenterY = 0f
    private var lastCenterX = 0f
    private var lastCenterY = 0f
    private var lastZoomTime = 0L

    fun applyAcceleration(dx: Float, dy: Float): Pair<Int, Int> {
        val speed = hypot(dx, dy)
        val accelMultiplier = (1.0f + 0.5f * min(speed / 8.0f, 1.0f)) * accelerationFactor
        val scaledDx = (dx * accelMultiplier).roundToInt()
        val scaledDy = (dy * accelMultiplier).roundToInt()
        return Pair(scaledDx, scaledDy)
    }

    fun processOneFingerMove(dx: Float, dy: Float, isLeftHeld: Boolean): MouseAction {
        val (finalDx, finalDy) = applyAcceleration(dx, dy)
        val buttons = if (isLeftHeld || isDragging || isTapAndHoldActive) {
            HidConstants.MOUSE_BUTTON_LEFT
        } else {
            HidConstants.MOUSE_BUTTON_NONE
        }
        return MouseAction(dx = finalDx, dy = finalDy, buttons = buttons)
    }

    fun processTwoFingerScroll(deltaY: Float): MouseAction? {
        accumulatedScrollY += deltaY
        val steps = (accumulatedScrollY / scrollSensitivity).toInt()
        return if (steps != 0) {
            accumulatedScrollY -= steps * scrollSensitivity
            val wheelVal = if (naturalScroll) steps else -steps
            MouseAction(wheel = wheelVal)
        } else {
            null
        }
    }

    fun processTwoFingerScrollH(deltaX: Float): MouseAction? {
        accumulatedScrollX += deltaX
        val steps = (accumulatedScrollX / scrollSensitivity).toInt()
        return if (steps != 0) {
            accumulatedScrollX -= steps * scrollSensitivity
            val panVal = if (naturalScroll) steps else -steps
            MouseAction(panH = panVal)
        } else {
            null
        }
    }

    fun setDragging(dragging: Boolean) {
        isDragging = dragging
    }

    fun setTapAndHold(active: Boolean) {
        isTapAndHoldActive = active
    }

    fun isTapAndHold(): Boolean = isTapAndHoldActive

    // --- High-level Touch Event Processor ---

    fun onTouchDown(
        x: Float,
        y: Float,
        pointerCount: Int,
        secondX: Float = 0f,
        secondY: Float = 0f
    ): GestureType {
        touchDownTime = System.currentTimeMillis()
        startX = x
        startY = y
        lastX = x
        lastY = y
        maxPointers = pointerCount
        hasMovedBeyondDeadzone = false
        hasFiredSwipe = false

        if (pointerCount >= 2) {
            isMultiFingerLocked = true
            twoFingerMode = TwoFingerMode.UNDECIDED
            if (secondX != 0f || secondY != 0f) {
                initialSpan = hypot(secondX - x, secondY - y)
                lastSpan = initialSpan
                initialCenterX = (x + secondX) / 2f
                initialCenterY = (y + secondY) / 2f
                lastCenterX = initialCenterX
                lastCenterY = initialCenterY
            } else {
                initialSpan = 0f
                lastSpan = 0f
            }
            accumulatedPinchDelta = 0f
        }

        // Tap and hold detection: if user taps again within 250ms of a previous tap
        val now = System.currentTimeMillis()
        if (pointerCount == 1 && (now - lastTapTime) < 280) {
            val dist = hypot(x - startX, y - startY)
            if (dist < 30) {
                isTapAndHoldActive = true
                GestureDiagnosticsHub.record(GestureType.ONE_FINGER_DRAG, 1, "Tap + Hold Initiated", "MOUSE_LEFT DOWN")
                return GestureType.ONE_FINGER_DRAG
            }
        }
        return GestureType.NONE
    }

    fun onPointerDown(
        pointerCount: Int,
        x1: Float = 0f,
        y1: Float = 0f,
        x2: Float = 0f,
        y2: Float = 0f
    ) {
        if (pointerCount > maxPointers) {
            maxPointers = pointerCount
        }
        if (pointerCount >= 2) {
            isMultiFingerLocked = true
            startX = x1
            startY = y1
            lastX = x1
            lastY = y1
            twoFingerMode = TwoFingerMode.UNDECIDED
            if (x2 != 0f || y2 != 0f) {
                initialSpan = hypot(x2 - x1, y2 - y1)
                lastSpan = initialSpan
                initialCenterX = (x1 + x2) / 2f
                initialCenterY = (y1 + y2) / 2f
                lastCenterX = initialCenterX
                lastCenterY = initialCenterY
            } else {
                initialSpan = 0f
                lastSpan = 0f
            }
            accumulatedPinchDelta = 0f
        }
    }

    fun onTouchMove(
        currentX: Float,
        currentY: Float,
        pointerCount: Int,
        isExplicitLeftHeld: Boolean = false,
        isDragLocked: Boolean = false,
        secondX: Float = 0f,
        secondY: Float = 0f
    ): Pair<GestureType, MouseAction?> {
        val currentMax = maxOf(maxPointers, pointerCount)
        maxPointers = currentMax

        val rawDx = currentX - lastX
        val rawDy = currentY - lastY
        lastX = currentX
        lastY = currentY

        // PRIORITY RESOLUTION: 4 fingers > 3 fingers > 2 fingers > 1 finger
        if (currentMax >= 4) {
            if (!hasFiredSwipe) {
                val totalDx = currentX - startX
                val totalDy = currentY - startY
                val swipeThreshold = 70f
                if (abs(totalDx) > swipeThreshold || abs(totalDy) > swipeThreshold) {
                    hasFiredSwipe = true
                    val gesture = if (abs(totalDx) > abs(totalDy)) {
                        if (totalDx > 0) GestureType.FOUR_FINGER_SWIPE_RIGHT else GestureType.FOUR_FINGER_SWIPE_LEFT
                    } else {
                        if (totalDy > 0) GestureType.FOUR_FINGER_SWIPE_DOWN else GestureType.FOUR_FINGER_SWIPE_UP
                    }
                    val shortcutDesc = when (gesture) {
                        GestureType.FOUR_FINGER_SWIPE_RIGHT -> "ALT + TAB (Next App)"
                        GestureType.FOUR_FINGER_SWIPE_LEFT -> "ALT + SHIFT + TAB (Prev App)"
                        GestureType.FOUR_FINGER_SWIPE_UP -> "WIN + TAB (Task View)"
                        GestureType.FOUR_FINGER_SWIPE_DOWN -> "WIN + D (Show Desktop)"
                        else -> "None"
                    }
                    GestureDiagnosticsHub.record(gesture, 4, "4-Finger Swipe", shortcutDesc)
                    return Pair(gesture, null)
                }
            }
            return Pair(GestureType.NONE, null)
        }

        if (currentMax == 3) {
            if (!hasFiredSwipe) {
                val totalDx = currentX - startX
                val totalDy = currentY - startY
                val swipeThreshold = 75f
                if (abs(totalDx) > swipeThreshold || abs(totalDy) > swipeThreshold) {
                    hasFiredSwipe = true
                    val gesture = if (abs(totalDx) > abs(totalDy)) {
                        if (totalDx > 0) GestureType.THREE_FINGER_SWIPE_RIGHT else GestureType.THREE_FINGER_SWIPE_LEFT
                    } else {
                        if (totalDy > 0) GestureType.THREE_FINGER_SWIPE_DOWN else GestureType.THREE_FINGER_SWIPE_UP
                    }
                    val shortcutDesc = when (gesture) {
                        GestureType.THREE_FINGER_SWIPE_RIGHT -> "CTRL + WIN + RIGHT (Next Desktop)"
                        GestureType.THREE_FINGER_SWIPE_LEFT -> "CTRL + WIN + LEFT (Prev Desktop)"
                        GestureType.THREE_FINGER_SWIPE_UP -> "WIN + TAB (Task View)"
                        GestureType.THREE_FINGER_SWIPE_DOWN -> "WIN + D (Show Desktop)"
                        else -> "None"
                    }
                    GestureDiagnosticsHub.record(gesture, 3, "3-Finger Swipe", shortcutDesc)
                    return Pair(gesture, null)
                }
            }
            return Pair(GestureType.NONE, null)
        }

        if (currentMax == 2) {
            // Disambiguation between 2-finger Pinch (Zoom) and 2-finger Scroll
            if (secondX != 0f || secondY != 0f) {
                val curSpan = hypot(secondX - currentX, secondY - currentY)
                val curCenterX = (currentX + secondX) / 2f
                val curCenterY = (currentY + secondY) / 2f

                if (initialSpan <= 0f) {
                    initialSpan = curSpan
                    lastSpan = curSpan
                    initialCenterX = curCenterX
                    initialCenterY = curCenterY
                    lastCenterX = curCenterX
                    lastCenterY = curCenterY
                }

                val spanDelta = abs(curSpan - initialSpan)
                val centerDelta = hypot(curCenterX - initialCenterX, curCenterY - initialCenterY)

                // Observation phase: classify Pinch vs Scroll
                if (twoFingerMode == TwoFingerMode.UNDECIDED) {
                    if (pinchZoomEnabled && spanDelta >= pinchThresholdPx && spanDelta > centerDelta * 0.75f) {
                        // Pinch intent confirmed: lock to PINCHING!
                        twoFingerMode = TwoFingerMode.PINCHING
                        accumulatedPinchDelta = curSpan - initialSpan
                        lastSpan = curSpan
                    } else if (centerDelta >= 20f && centerDelta > spanDelta * 1.25f) {
                        // Scroll intent confirmed: lock to SCROLLING!
                        twoFingerMode = TwoFingerMode.SCROLLING
                    } else if (pinchZoomEnabled && spanDelta >= pinchThresholdPx * 1.5f) {
                        // Dominant span change
                        twoFingerMode = TwoFingerMode.PINCHING
                        accumulatedPinchDelta = curSpan - initialSpan
                        lastSpan = curSpan
                    }
                }

                if (twoFingerMode == TwoFingerMode.PINCHING) {
                    // In PINCHING mode: strictly NO scroll wheel events
                    val deltaSpan = curSpan - lastSpan
                    lastSpan = curSpan
                    accumulatedPinchDelta += deltaSpan

                    val now = System.currentTimeMillis()
                    if (now - lastZoomTime >= pinchRateLimitMs) {
                        if (accumulatedPinchDelta >= pinchStepThresholdPx) {
                            accumulatedPinchDelta = 0f
                            lastZoomTime = now
                            GestureDiagnosticsHub.record(
                                GestureType.TWO_FINGER_PINCH_OUT,
                                2,
                                "Pinch Out",
                                "ZOOM_IN"
                            )
                            return Pair(GestureType.TWO_FINGER_PINCH_OUT, null)
                        } else if (accumulatedPinchDelta <= -pinchStepThresholdPx) {
                            accumulatedPinchDelta = 0f
                            lastZoomTime = now
                            GestureDiagnosticsHub.record(
                                GestureType.TWO_FINGER_PINCH_IN,
                                2,
                                "Pinch In",
                                "ZOOM_OUT"
                            )
                            return Pair(GestureType.TWO_FINGER_PINCH_IN, null)
                        }
                    }
                    return Pair(GestureType.NONE, null)
                }

                if (twoFingerMode == TwoFingerMode.SCROLLING) {
                    val deltaCenterY = curCenterY - lastCenterY
                    val deltaCenterX = curCenterX - lastCenterX
                    lastCenterX = curCenterX
                    lastCenterY = curCenterY

                    val action = if (abs(deltaCenterY) >= abs(deltaCenterX)) {
                        val scroll = processTwoFingerScroll(deltaCenterY)
                        if (scroll != null) Pair(GestureType.TWO_FINGER_SCROLL_V, scroll) else Pair(GestureType.NONE, null)
                    } else {
                        val scrollH = processTwoFingerScrollH(deltaCenterX)
                        if (scrollH != null) Pair(GestureType.TWO_FINGER_SCROLL_H, scrollH) else Pair(GestureType.NONE, null)
                    }
                    return action
                }

                // Still in observation phase: suppress premature scroll or zoom
                return Pair(GestureType.NONE, null)
            } else {
                // Fallback for single-pointer input (e.g. tests or devices without pointer 2 coords)
                val action = if (abs(rawDy) >= abs(rawDx)) {
                    val scroll = processTwoFingerScroll(rawDy)
                    if (scroll != null) Pair(GestureType.TWO_FINGER_SCROLL_V, scroll) else Pair(GestureType.NONE, null)
                } else {
                    val scrollH = processTwoFingerScrollH(rawDx)
                    if (scrollH != null) Pair(GestureType.TWO_FINGER_SCROLL_H, scrollH) else Pair(GestureType.NONE, null)
                }
                return action
            }
        }

        // Single finger movement (ONLY when NOT locked to multi-finger gesture)
        if (!isMultiFingerLocked && currentMax == 1) {
            if (!hasMovedBeyondDeadzone) {
                val dist = hypot(currentX - startX, currentY - startY)
                if (dist < deadZonePx) {
                    return Pair(GestureType.NONE, null)
                }
                hasMovedBeyondDeadzone = true
            }

            val action = processOneFingerMove(rawDx, rawDy, isExplicitLeftHeld || isDragLocked)
            val gestureType = if (isTapAndHoldActive || isExplicitLeftHeld || isDragLocked) {
                GestureType.ONE_FINGER_DRAG
            } else {
                GestureType.ONE_FINGER_MOVE
            }
            return Pair(gestureType, action)
        }

        return Pair(GestureType.NONE, null)
    }

    fun onTouchUp(endX: Float, endY: Float): GestureType {
        val elapsed = System.currentTimeMillis() - touchDownTime
        val dist = hypot(endX - startX, endY - startY)
        val finalPointers = maxPointers

        var recognizedGesture = GestureType.NONE

        if (!hasFiredSwipe) {
            when (finalPointers) {
                1 -> {
                    if (elapsed < 240 && dist < 25f && !hasMovedBeyondDeadzone) {
                        val now = System.currentTimeMillis()
                        if (now - lastTapTime < 280) {
                            recognizedGesture = GestureType.ONE_FINGER_DOUBLE_TAP
                            lastTapTime = 0L
                            GestureDiagnosticsHub.record(recognizedGesture, 1, "Double Tap", "DOUBLE_LEFT_CLICK")
                        } else {
                            recognizedGesture = GestureType.ONE_FINGER_TAP
                            lastTapTime = now
                            GestureDiagnosticsHub.record(recognizedGesture, 1, "Tap", "LEFT_CLICK")
                        }
                    }
                }
                2 -> {
                    // Suppress right-click tap if touch was recognized as pinch
                    if (twoFingerMode != TwoFingerMode.PINCHING && elapsed < 260 && dist < 30f) {
                        recognizedGesture = GestureType.TWO_FINGER_TAP
                        GestureDiagnosticsHub.record(recognizedGesture, 2, "2-Finger Tap", "RIGHT_CLICK")
                    }
                }
                3 -> {
                    if (elapsed < 320 && dist < 35f) {
                        recognizedGesture = GestureType.THREE_FINGER_TAP
                        GestureDiagnosticsHub.record(recognizedGesture, 3, "3-Finger Tap", "MIDDLE_CLICK")
                    }
                }
                4 -> {
                    if (elapsed < 320 && dist < 40f) {
                        recognizedGesture = GestureType.FOUR_FINGER_TAP
                        GestureDiagnosticsHub.record(recognizedGesture, 4, "4-Finger Tap", "OPEN_COMMAND_DECK")
                    }
                }
            }
        }

        // Reset sequence
        maxPointers = 0
        isMultiFingerLocked = false
        hasFiredSwipe = false
        hasMovedBeyondDeadzone = false
        isTapAndHoldActive = false
        twoFingerMode = TwoFingerMode.UNDECIDED
        initialSpan = 0f
        lastSpan = 0f
        accumulatedPinchDelta = 0f
        initialCenterX = 0f
        initialCenterY = 0f
        lastCenterX = 0f
        lastCenterY = 0f

        return recognizedGesture
    }
}
