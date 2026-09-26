package dev.aleian.pockethid.ui.components

import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aleian.pockethid.mapping.FastScrollController
import dev.aleian.pockethid.mapping.GestureInterpreter
import dev.aleian.pockethid.mapping.GestureType
import dev.aleian.pockethid.mapping.toDefaultAction
import dev.aleian.pockethid.model.AppSettings
import dev.aleian.pockethid.model.HidConstants
import dev.aleian.pockethid.transport.InputTransport
import dev.aleian.pockethid.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MultiTouchTrackpad(
    transport: InputTransport?,
    settings: AppSettings,
    canSendInput: () -> Boolean,
    onOpenCommandDeck: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    var isDragLocked by remember { mutableStateOf(false) }
    var isExplicitLeftHeld by remember { mutableStateOf(false) }
    var isExplicitMiddleHeld by remember { mutableStateOf(false) }
    var isExplicitRightHeld by remember { mutableStateOf(false) }

    var showGuideDialog by remember { mutableStateOf(false) }
    var gestureBannerText by remember { mutableStateOf<String?>(null) }

    val gestureInterpreter = remember(settings) {
        GestureInterpreter(
            accelerationFactor = settings.accelerationFactor,
            naturalScroll = settings.naturalScroll,
            deadZonePx = settings.deadZonePx.toFloat(),
            pinchZoomEnabled = settings.pinchZoomEnabled,
            pinchThresholdPx = settings.pinchThresholdPx
        )
    }

    val fastScrollController = remember(settings) {
        FastScrollController(
            enabled = settings.fastScrollEnabled,
            multiplier = settings.fastScrollMultiplier,
            widthPercent = settings.fastScrollWidthPercent
        )
    }
    var isFastScrollActive by remember { mutableStateOf(false) }
    var trackpadWidthPx by remember { mutableFloatStateOf(0f) }

    fun triggerLightHaptic() {
        if (settings.keyboardHaptics) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    fun triggerMediumHaptic() {
        if (settings.keyboardHaptics) {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    fun triggerStrongHaptic() {
        if (settings.keyboardHaptics) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        }
    }

    fun showGestureFeedback(label: String) {
        gestureBannerText = label
        scope.launch {
            delay(1200)
            if (gestureBannerText == label) {
                gestureBannerText = null
            }
        }
    }

    fun executeGestureAction(gesture: GestureType) {
        if (!canSendInput()) return

        if (gesture == GestureType.FOUR_FINGER_TAP) {
            triggerStrongHaptic()
            showGestureFeedback("Command Deck (4-Finger Tap)")
            onOpenCommandDeck()
            return
        }

        val action = gesture.toDefaultAction() ?: return

        when {
            gesture.name.startsWith("FOUR_FINGER") -> triggerStrongHaptic()
            gesture.name.startsWith("THREE_FINGER") -> triggerMediumHaptic()
            else -> triggerLightHaptic()
        }

        val feedback = when (gesture) {
            GestureType.TWO_FINGER_PINCH_OUT -> "Zoom In 🔍+"
            GestureType.TWO_FINGER_PINCH_IN -> "Zoom Out 🔍-"
            GestureType.THREE_FINGER_TAP -> "3-Finger Tap: Middle Click"
            GestureType.THREE_FINGER_SWIPE_LEFT -> "◀ Prev Desktop"
            GestureType.THREE_FINGER_SWIPE_RIGHT -> "Next Desktop ▶"
            GestureType.THREE_FINGER_SWIPE_UP -> "Task View / Overview ▲"
            GestureType.THREE_FINGER_SWIPE_DOWN -> "Show Desktop ▼"
            GestureType.FOUR_FINGER_SWIPE_LEFT -> "◀ Switch App Prev"
            GestureType.FOUR_FINGER_SWIPE_RIGHT -> "Switch App Next ▶"
            GestureType.FOUR_FINGER_SWIPE_UP -> "Task View / Overview ▲"
            GestureType.FOUR_FINGER_SWIPE_DOWN -> "Minimize / Desktop ▼"
            else -> null
        }
        if (feedback != null) {
            showGestureFeedback(feedback)
        }

        scope.launch {
            dev.aleian.pockethid.action.ActionDispatcher.dispatch(
                action = action,
                transport = transport,
                hostOs = settings.hostOs,
                zoomMode = settings.zoomMode
            )
        }
    }

    fun computeActiveButtons(): Byte {
        var buttons: Byte = 0
        if (isExplicitLeftHeld || isDragLocked || gestureInterpreter.isTapAndHold()) {
            buttons = (buttons.toInt() or HidConstants.MOUSE_BUTTON_LEFT.toInt()).toByte()
        }
        if (isExplicitRightHeld) {
            buttons = (buttons.toInt() or HidConstants.MOUSE_BUTTON_RIGHT.toInt()).toByte()
        }
        if (isExplicitMiddleHeld) {
            buttons = (buttons.toInt() or HidConstants.MOUSE_BUTTON_MIDDLE.toInt()).toByte()
        }
        return buttons
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // MAIN TRACKPAD SURFACE
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .onSizeChanged { size ->
                    trackpadWidthPx = size.width.toFloat()
                }
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurface)
                .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                .pointerInteropFilter { event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            val density = context.resources.displayMetrics.density
                            if (fastScrollController.onTouchDown(event.x, event.y, trackpadWidthPx, event.pointerCount, density)) {
                                isFastScrollActive = true
                                triggerLightHaptic()
                                showGestureFeedback("FAST SCROLL ×${String.format(Locale.US, "%.1f", settings.fastScrollMultiplier)}")
                                true
                            } else {
                                isFastScrollActive = false
                                val x1 = event.x
                                val y1 = event.y
                                val x2 = if (event.pointerCount > 1) event.getX(1) else 0f
                                val y2 = if (event.pointerCount > 1) event.getY(1) else 0f
                                val startGesture = gestureInterpreter.onTouchDown(x1, y1, event.pointerCount, x2, y2)
                                if (startGesture == GestureType.ONE_FINGER_DRAG) {
                                    triggerLightHaptic()
                                    showGestureFeedback("Drag Mode Active")
                                }
                                true
                            }
                        }

                        MotionEvent.ACTION_POINTER_DOWN -> {
                            if (isFastScrollActive) {
                                fastScrollController.onTouchUp()
                                isFastScrollActive = false
                            }
                            val x1 = event.getX(0)
                            val y1 = event.getY(0)
                            val x2 = if (event.pointerCount > 1) event.getX(1) else 0f
                            val y2 = if (event.pointerCount > 1) event.getY(1) else 0f
                            gestureInterpreter.onPointerDown(event.pointerCount, x1, y1, x2, y2)
                            true
                        }

                        MotionEvent.ACTION_MOVE -> {
                            if (!canSendInput()) return@pointerInteropFilter true

                            if (isFastScrollActive) {
                                if (event.pointerCount > 1) {
                                    fastScrollController.onTouchUp()
                                    isFastScrollActive = false
                                    return@pointerInteropFilter true
                                }
                                val wheelVal = fastScrollController.onTouchMove(
                                    currentY = event.y,
                                    pointerCount = event.pointerCount,
                                    naturalScroll = settings.naturalScroll
                                )
                                if (wheelVal != null && wheelVal != 0) {
                                    transport?.sendMouseMove(
                                        0,
                                        0,
                                        computeActiveButtons(),
                                        wheelVal
                                    )
                                }
                                true
                            } else {
                                val x1 = event.getX(0)
                                val y1 = event.getY(0)
                                val x2 = if (event.pointerCount > 1) event.getX(1) else 0f
                                val y2 = if (event.pointerCount > 1) event.getY(1) else 0f

                                val (gesture, action) = gestureInterpreter.onTouchMove(
                                    currentX = x1,
                                    currentY = y1,
                                    pointerCount = event.pointerCount,
                                    isExplicitLeftHeld = isExplicitLeftHeld,
                                    isDragLocked = isDragLocked,
                                    secondX = x2,
                                    secondY = y2
                                )

                                if (gesture != GestureType.NONE && action == null) {
                                    executeGestureAction(gesture)
                                } else if (action != null) {
                                    val currentButtons = (action.buttons.toInt() or computeActiveButtons().toInt()).toByte()
                                    transport?.sendMouseMove(
                                        action.dx,
                                        action.dy,
                                        currentButtons,
                                        action.wheel
                                    )
                                }
                                true
                            }
                        }

                        MotionEvent.ACTION_UP -> {
                            if (isFastScrollActive) {
                                fastScrollController.onTouchUp()
                                isFastScrollActive = false
                                true
                            } else {
                                val finalGesture = gestureInterpreter.onTouchUp(event.x, event.y)
                                if (finalGesture != GestureType.NONE) {
                                    executeGestureAction(finalGesture)
                                }
                                true
                            }
                        }

                        MotionEvent.ACTION_CANCEL -> {
                            if (isFastScrollActive) {
                                fastScrollController.onTouchUp()
                                isFastScrollActive = false
                                true
                            } else {
                                gestureInterpreter.onTouchUp(event.x, event.y)
                                true
                            }
                        }

                        else -> true
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Subtle instruction hint in center
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "PRECISION TRACKPAD",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary.copy(alpha = 0.5f),
                    letterSpacing = 1.2.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "1 finger · Move / Tap\n2 fingers · Scroll / Pinch Zoom / Right click\n3 fingers · Middle click / Desktop\n4 fingers · Command / Switch",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 13.sp,
                    textAlign = TextAlign.Center,
                    color = TextMuted.copy(alpha = 0.6f)
                )
            }

            // Top-right: Quick Gesture Guide button
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant.copy(alpha = 0.7f))
                    .clickable { showGuideDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = "Gesture Guide",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Real-time Gesture Feedback Banner (subtle popup)
            if (gestureBannerText != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(PrimaryBlue.copy(alpha = 0.85f))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = gestureBannerText ?: "",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // RIGHT EDGE FAST SCROLL ZONE (Dedicated high-speed vertical scroll strip)
            if (settings.fastScrollEnabled) {
                val density = LocalDensity.current
                val visualWidthDp = if (trackpadWidthPx > 0f) {
                    with(density) {
                        (trackpadWidthPx * settings.fastScrollWidthPercent.coerceIn(0.08f, 0.12f)).toDp().coerceIn(32.dp, 64.dp)
                    }
                } else {
                    40.dp
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(visualWidthDp)
                        .clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp))
                        .background(
                            if (isFastScrollActive) PrimaryBlue.copy(alpha = 0.22f)
                            else DarkSurfaceVariant.copy(alpha = 0.35f)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isFastScrollActive) PrimaryBlue.copy(alpha = 0.6f) else DarkBorder.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)
                        )
                        .semantics {
                            contentDescription = "Fast Scroll Area. Swipe vertically to quickly scroll."
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    ) {
                        Text(
                            text = "⋮\n⋮",
                            color = if (isFastScrollActive) PrimaryBlue.copy(alpha = 0.9f) else TextMuted.copy(alpha = 0.35f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = 10.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "↕",
                            color = if (isFastScrollActive) PrimaryBlue else TextSecondary.copy(alpha = 0.6f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "FAST",
                            color = if (isFastScrollActive) PrimaryBlue else TextMuted.copy(alpha = 0.4f),
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "⋮\n⋮",
                            color = if (isFastScrollActive) PrimaryBlue.copy(alpha = 0.9f) else TextMuted.copy(alpha = 0.35f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = 10.sp
                        )
                    }
                }
            }
        }

        // BOTTOM EXPLICIT MOUSE BUTTONS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Drag Lock Toggle
            Box(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isDragLocked) PrimaryBlue.copy(alpha = 0.25f) else DarkSurfaceVariant)
                    .border(1.dp, if (isDragLocked) PrimaryBlue else DarkBorder, RoundedCornerShape(8.dp))
                    .clickable {
                        isDragLocked = !isDragLocked
                        triggerMediumHaptic()
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isDragLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = null,
                        tint = if (isDragLocked) PrimaryBlue else TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "LOCK",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDragLocked) PrimaryBlue else TextSecondary
                    )
                }
            }

            // Explicit LEFT Button (Hold-to-drag support)
            Box(
                modifier = Modifier
                    .weight(1.8f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isExplicitLeftHeld) PrimaryBlue.copy(alpha = 0.35f) else DarkSurfaceVariant)
                    .border(1.dp, if (isExplicitLeftHeld) PrimaryBlue else DarkBorder, RoundedCornerShape(8.dp))
                    .pointerInteropFilter { ev ->
                        when (ev.actionMasked) {
                            MotionEvent.ACTION_DOWN -> {
                                isExplicitLeftHeld = true
                                triggerLightHaptic()
                                if (canSendInput()) {
                                    transport?.sendMouseMove(0, 0, computeActiveButtons(), 0)
                                }
                                true
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                isExplicitLeftHeld = false
                                if (canSendInput()) {
                                    transport?.sendMouseMove(0, 0, computeActiveButtons(), 0)
                                }
                                true
                            }
                            else -> true
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "LEFT",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isExplicitLeftHeld) PrimaryBlue else TextPrimary
                )
            }

            // Explicit MIDDLE Button
            Box(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isExplicitMiddleHeld) AccentAmber.copy(alpha = 0.25f) else DarkSurfaceVariant)
                    .border(1.dp, if (isExplicitMiddleHeld) AccentAmber else DarkBorder, RoundedCornerShape(8.dp))
                    .pointerInteropFilter { ev ->
                        when (ev.actionMasked) {
                            MotionEvent.ACTION_DOWN -> {
                                isExplicitMiddleHeld = true
                                triggerLightHaptic()
                                if (canSendInput()) {
                                    transport?.sendMouseMove(0, 0, computeActiveButtons(), 0)
                                }
                                true
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                isExplicitMiddleHeld = false
                                if (canSendInput()) {
                                    transport?.sendMouseMove(0, 0, computeActiveButtons(), 0)
                                }
                                true
                            }
                            else -> true
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "MID",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isExplicitMiddleHeld) AccentAmber else TextSecondary
                )
            }

            // Explicit RIGHT Button
            Box(
                modifier = Modifier
                    .weight(1.8f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isExplicitRightHeld) PrimaryBlue.copy(alpha = 0.35f) else DarkSurfaceVariant)
                    .border(1.dp, if (isExplicitRightHeld) PrimaryBlue else DarkBorder, RoundedCornerShape(8.dp))
                    .pointerInteropFilter { ev ->
                        when (ev.actionMasked) {
                            MotionEvent.ACTION_DOWN -> {
                                isExplicitRightHeld = true
                                triggerLightHaptic()
                                if (canSendInput()) {
                                    transport?.sendMouseMove(0, 0, computeActiveButtons(), 0)
                                }
                                true
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                isExplicitRightHeld = false
                                if (canSendInput()) {
                                    transport?.sendMouseMove(0, 0, computeActiveButtons(), 0)
                                }
                                true
                            }
                            else -> true
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "RIGHT",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isExplicitRightHeld) PrimaryBlue else TextPrimary
                )
            }
        }
    }

    // GESTURE GUIDE MODAL
    if (showGuideDialog) {
        AlertDialog(
            onDismissRequest = { showGuideDialog = false },
            title = {
                Text(
                    text = "Trackpad Gesture Guide",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GestureGuideRow("1 Finger Move", "Move PC cursor")
                    GestureGuideRow("1 Finger Tap", "Left click")
                    GestureGuideRow("1 Finger Double Tap", "Double left click")
                    GestureGuideRow("1 Finger Tap + Hold", "Left button drag")
                    HorizontalDivider(color = DarkBorder)
                    GestureGuideRow("2 Fingers Move", "Vertical / Horizontal scroll")
                    GestureGuideRow("2 Fingers Pinch Out", "Desktop Zoom In (Ctrl/Cmd + Wheel)")
                    GestureGuideRow("2 Fingers Pinch In", "Desktop Zoom Out (Ctrl/Cmd + Wheel)")
                    GestureGuideRow("2 Fingers Tap", "Right click")
                    HorizontalDivider(color = DarkBorder)
                    GestureGuideRow("3 Fingers Tap", "Middle click")
                    GestureGuideRow("3 Fingers Swipe ◀ / ▶", "Previous / Next virtual desktop")
                    GestureGuideRow("3 Fingers Swipe ▲ / ▼", "Task View / Show Desktop")
                    HorizontalDivider(color = DarkBorder)
                    GestureGuideRow("4 Fingers Tap", "Open Command Deck")
                    GestureGuideRow("4 Fingers Swipe ◀ / ▶", "Switch applications (Alt+Tab)")
                    GestureGuideRow("4 Fingers Swipe ▲ / ▼", "Task overview / Desktop")
                }
            },
            confirmButton = {
                TextButton(onClick = { showGuideDialog = false }) {
                    Text("Got it", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkSurface,
            tonalElevation = 6.dp
        )
    }
}

@Composable
private fun GestureGuideRow(gesture: String, action: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = gesture,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryBlue,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = action,
            fontSize = 11.sp,
            color = TextSecondary,
            textAlign = TextAlign.End
        )
    }
}
