package dev.aleian.pockethid.ui.screens

import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aleian.pockethid.mapping.GestureInterpreter
import dev.aleian.pockethid.model.HidConstants
import dev.aleian.pockethid.transport.InputTransport
import dev.aleian.pockethid.ui.theme.AccentGreen
import dev.aleian.pockethid.ui.theme.DarkBg
import dev.aleian.pockethid.ui.theme.DarkBorder
import dev.aleian.pockethid.ui.theme.DarkSurface
import dev.aleian.pockethid.ui.theme.DarkSurfaceVariant
import dev.aleian.pockethid.ui.theme.TextMuted
import dev.aleian.pockethid.ui.theme.TextPrimary
import dev.aleian.pockethid.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MouseScreen(
    transport: InputTransport?,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val settings by dev.aleian.pockethid.model.SettingsRepository.settings.collectAsState()
    val connState by (transport?.connectionState?.collectAsState()
        ?: remember { mutableStateOf(dev.aleian.pockethid.model.ConnectionState.Disconnected) })
    var lastWarnTime by remember { mutableStateOf(0L) }

    fun checkConnectionWarn() {
        if (connState !is dev.aleian.pockethid.model.ConnectionState.Connected) {
            val now = System.currentTimeMillis()
            if (now - lastWarnTime > 3000) {
                lastWarnTime = now
                android.widget.Toast.makeText(context, "Chưa kết nối máy tính! Vui lòng kết nối Bluetooth trước.", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    val gestureInterpreter = remember(settings) {
        GestureInterpreter(
            accelerationFactor = settings.accelerationFactor,
            naturalScroll = settings.naturalScroll
        )
    }

    var isLeftButtonHeld by remember { mutableStateOf(false) }
    var isRightButtonHeld by remember { mutableStateOf(false) }
    var isMiddleButtonHeld by remember { mutableStateOf(false) }

    fun triggerHaptic() {
        if (settings.hapticsTrackpad) {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    fun sendClick(buttonMask: Byte) {
        checkConnectionWarn()
        scope.launch {
            triggerHaptic()
            transport?.sendMouseMove(0, 0, buttonMask, 0)
            delay(16)
            val remainingButtons: Byte = when {
                isLeftButtonHeld -> HidConstants.MOUSE_BUTTON_LEFT
                isRightButtonHeld -> HidConstants.MOUSE_BUTTON_RIGHT
                isMiddleButtonHeld -> HidConstants.MOUSE_BUTTON_MIDDLE
                else -> HidConstants.MOUSE_BUTTON_NONE
            }
            transport?.sendMouseMove(0, 0, remainingButtons, 0)
        }
    }

    // Touch tracking state
    var lastTouchX by remember { mutableStateOf(0f) }
    var lastTouchY by remember { mutableStateOf(0f) }
    var touchDownTime by remember { mutableStateOf(0L) }
    var hasMovedBeyondDeadzone by remember { mutableStateOf(false) }
    var pointerCount by remember { mutableStateOf(1) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Trackpad surface
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(DarkSurface)
                .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                .pointerInteropFilter { event ->
                    pointerCount = event.pointerCount

                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            checkConnectionWarn()
                            lastTouchX = event.x
                            lastTouchY = event.y
                            touchDownTime = System.currentTimeMillis()
                            hasMovedBeyondDeadzone = false
                            true
                        }

                        MotionEvent.ACTION_POINTER_DOWN -> {
                            pointerCount = event.pointerCount
                            true
                        }

                        MotionEvent.ACTION_MOVE -> {
                            val elapsed = System.currentTimeMillis() - touchDownTime
                            val currentX = event.x
                            val currentY = event.y

                            if (pointerCount == 1) {
                                val rawDx = currentX - lastTouchX
                                val rawDy = currentY - lastTouchY

                                // Deadzone check: discard < deadZonePx within first 30ms to prevent tap jitter
                                if (!hasMovedBeyondDeadzone) {
                                    val dz = settings.deadZonePx.toFloat()
                                    if (abs(rawDx) < dz && abs(rawDy) < dz && elapsed < 30) {
                                        return@pointerInteropFilter true
                                    }
                                    hasMovedBeyondDeadzone = true
                                }

                                val action = gestureInterpreter.processOneFingerMove(rawDx, rawDy, isLeftButtonHeld)
                                transport?.sendMouseMove(action.dx, action.dy, action.buttons, 0)

                                lastTouchX = currentX
                                lastTouchY = currentY
                            } else if (pointerCount >= 2) {
                                // 2-finger scroll
                                val rawDy = currentY - lastTouchY
                                val scrollAction = gestureInterpreter.processTwoFingerScroll(rawDy)
                                if (scrollAction != null) {
                                    transport?.sendMouseMove(0, 0, HidConstants.MOUSE_BUTTON_NONE, scrollAction.wheel)
                                }
                                lastTouchY = currentY
                            }
                            true
                        }

                        MotionEvent.ACTION_UP -> {
                            val duration = System.currentTimeMillis() - touchDownTime
                            if (!hasMovedBeyondDeadzone && duration < 250) {
                                // Single tap = left click
                                sendClick(HidConstants.MOUSE_BUTTON_LEFT)
                            }
                            gestureInterpreter.setDragging(false)
                            true
                        }

                        MotionEvent.ACTION_POINTER_UP -> {
                            val duration = System.currentTimeMillis() - touchDownTime
                            if (pointerCount == 2 && duration < 300 && !hasMovedBeyondDeadzone) {
                                // 2-finger tap = right click
                                sendClick(HidConstants.MOUSE_BUTTON_RIGHT)
                            }
                            true
                        }

                        MotionEvent.ACTION_CANCEL -> {
                            gestureInterpreter.setDragging(false)
                            true
                        }

                        else -> false
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Trackpad",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp
                    ),
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "1 finger: move / tap click  •  2 fingers: scroll / right-click",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Physical mouse buttons row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Left Button
            Button(
                onClick = {
                    sendClick(HidConstants.MOUSE_BUTTON_LEFT)
                },
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxSize(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLeftButtonHeld) AccentGreen else DarkSurfaceVariant,
                    contentColor = TextPrimary
                )
            ) {
                Text(
                    text = if (isLeftButtonHeld) "L-Hold" else "Left",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }

            // Middle Button (Scroll Click)
            Button(
                onClick = {
                    sendClick(HidConstants.MOUSE_BUTTON_MIDDLE)
                },
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxSize(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkSurfaceVariant,
                    contentColor = TextSecondary
                )
            ) {
                Text("Middle", fontSize = 13.sp)
            }

            // Right Button
            Button(
                onClick = {
                    sendClick(HidConstants.MOUSE_BUTTON_RIGHT)
                },
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxSize(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkSurfaceVariant,
                    contentColor = TextPrimary
                )
            ) {
                Text("Right", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }
    }
}
