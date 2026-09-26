package dev.aleian.pockethid.ui.screens

import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aleian.pockethid.mapping.GestureInterpreter
import dev.aleian.pockethid.model.ConnectionState
import dev.aleian.pockethid.model.HidConstants
import dev.aleian.pockethid.model.SettingsRepository
import dev.aleian.pockethid.transport.InputTransport
import dev.aleian.pockethid.ui.theme.DarkBorder
import dev.aleian.pockethid.ui.theme.DarkSurface
import dev.aleian.pockethid.ui.theme.DarkSurfaceVariant
import dev.aleian.pockethid.ui.theme.PrimaryBlue
import dev.aleian.pockethid.ui.theme.StatusConnected
import dev.aleian.pockethid.ui.theme.StatusConnecting
import dev.aleian.pockethid.ui.theme.StatusDisconnected
import dev.aleian.pockethid.ui.theme.SurfaceCard
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
    connectionState: ConnectionState,
    onPairClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onHostInfoClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val settings by SettingsRepository.settings.collectAsState()

    var isDragLocked by remember { mutableStateOf(false) }
    var lastWarnTime by remember { mutableStateOf(0L) }

    val gestureInterpreter = remember(settings) {
        GestureInterpreter(
            accelerationFactor = settings.accelerationFactor,
            naturalScroll = settings.naturalScroll
        )
    }

    fun triggerHaptic() {
        if (settings.hapticsTrackpad) {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    /**
     * Validate action against real HID session.
     * Only displays notification if actually DISCONNECTED and user performed an HID action.
     */
    fun canSendInput(): Boolean {
        if (connectionState is ConnectionState.Connected || transport?.isConnected == true) {
            return true
        }
        if (connectionState is ConnectionState.Connecting) {
            // Subtle no-op, do not spam error toast
            return false
        }
        val now = System.currentTimeMillis()
        if (now - lastWarnTime > 3000) {
            lastWarnTime = now
            Toast.makeText(context, "Connect to a host first.", Toast.LENGTH_SHORT).show()
        }
        return false
    }

    fun sendClick(buttonMask: Byte) {
        if (!canSendInput()) return
        scope.launch {
            triggerHaptic()
            transport?.sendMouseMove(0, 0, buttonMask, 0)
            delay(16)
            val remaining: Byte = if (isDragLocked) HidConstants.MOUSE_BUTTON_LEFT else HidConstants.MOUSE_BUTTON_NONE
            transport?.sendMouseMove(0, 0, remaining, 0)
        }
    }

    // Touch tracking state
    var lastTouchX by remember { mutableStateOf(0f) }
    var lastTouchY by remember { mutableStateOf(0f) }
    var touchDownTime by remember { mutableStateOf(0L) }
    var hasMovedBeyondDeadzone by remember { mutableStateOf(false) }
    var pointerCount by remember { mutableStateOf(1) }
    var threeFingerStartX by remember { mutableStateOf(0f) }
    var threeFingerStartY by remember { mutableStateOf(0f) }
    var hasTriggeredThreeFingerGesture by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // TOP: Connection Status Card
        MouseConnectionStatusCard(
            connectionState = connectionState,
            onPairClick = onPairClick,
            onHostInfoClick = onHostInfoClick,
            onSettingsClick = onSettingsClick
        )

        // MAIN: Large Precision Trackpad
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurface)
                .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                .pointerInteropFilter { event ->
                    pointerCount = event.pointerCount

                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            lastTouchX = event.x
                            lastTouchY = event.y
                            touchDownTime = System.currentTimeMillis()
                            hasMovedBeyondDeadzone = false
                            true
                        }

                        MotionEvent.ACTION_POINTER_DOWN -> {
                            pointerCount = event.pointerCount
                            if (pointerCount == 3) {
                                threeFingerStartX = event.x
                                threeFingerStartY = event.y
                                hasTriggeredThreeFingerGesture = false
                            }
                            true
                        }

                        MotionEvent.ACTION_MOVE -> {
                            if (!canSendInput()) return@pointerInteropFilter true

                            val elapsed = System.currentTimeMillis() - touchDownTime
                            val currentX = event.x
                            val currentY = event.y

                            if (pointerCount == 1) {
                                val rawDx = currentX - lastTouchX
                                val rawDy = currentY - lastTouchY

                                if (!hasMovedBeyondDeadzone) {
                                    val dz = settings.deadZonePx.toFloat()
                                    if (abs(rawDx) < dz && abs(rawDy) < dz && elapsed < 30) {
                                        return@pointerInteropFilter true
                                    }
                                    hasMovedBeyondDeadzone = true
                                }

                                val action = gestureInterpreter.processOneFingerMove(rawDx, rawDy, isDragLocked)
                                transport?.sendMouseMove(action.dx, action.dy, action.buttons, 0)

                                lastTouchX = currentX
                                lastTouchY = currentY
                            } else if (pointerCount == 2) {
                                val rawDy = currentY - lastTouchY
                                val scrollAction = gestureInterpreter.processTwoFingerScroll(rawDy)
                                if (scrollAction != null) {
                                    transport?.sendMouseMove(0, 0, HidConstants.MOUSE_BUTTON_NONE, scrollAction.wheel)
                                }
                                lastTouchY = currentY
                            } else if (pointerCount >= 3 && !hasTriggeredThreeFingerGesture) {
                                val deltaX = currentX - threeFingerStartX
                                val deltaY = currentY - threeFingerStartY
                                val threshold = 100f
                                if (abs(deltaX) > threshold || abs(deltaY) > threshold) {
                                    hasTriggeredThreeFingerGesture = true
                                    triggerHaptic()
                                    scope.launch {
                                        if (abs(deltaX) > abs(deltaY)) {
                                            if (deltaX > 0) {
                                                // Swipe right -> Prev desktop
                                                transport?.sendKeyClick(
                                                    HidConstants.KEY_LEFT,
                                                    (HidConstants.MOD_LEFT_CTRL.toInt() or HidConstants.MOD_LEFT_GUI.toInt()).toByte()
                                                )
                                                Toast.makeText(context, "Desktop ◀", Toast.LENGTH_SHORT).show()
                                            } else {
                                                // Swipe left -> Next desktop
                                                transport?.sendKeyClick(
                                                    HidConstants.KEY_RIGHT,
                                                    (HidConstants.MOD_LEFT_CTRL.toInt() or HidConstants.MOD_LEFT_GUI.toInt()).toByte()
                                                )
                                                Toast.makeText(context, "Desktop ▶", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            if (deltaY < 0) {
                                                // Swipe up -> Task View
                                                transport?.sendKeyClick(HidConstants.KEY_TAB, HidConstants.MOD_LEFT_GUI)
                                                Toast.makeText(context, "Task View", Toast.LENGTH_SHORT).show()
                                            } else {
                                                // Swipe down -> Show Desktop
                                                transport?.sendKeyClick(HidConstants.KEY_D, HidConstants.MOD_LEFT_GUI)
                                                Toast.makeText(context, "Show Desktop", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            }
                            true
                        }

                        MotionEvent.ACTION_UP -> {
                            val duration = System.currentTimeMillis() - touchDownTime
                            if (!hasMovedBeyondDeadzone && duration < 250) {
                                sendClick(HidConstants.MOUSE_BUTTON_LEFT)
                            }
                            gestureInterpreter.setDragging(false)
                            hasTriggeredThreeFingerGesture = false
                            true
                        }

                        MotionEvent.ACTION_POINTER_UP -> {
                            val duration = System.currentTimeMillis() - touchDownTime
                            if (pointerCount == 2 && duration < 300 && !hasMovedBeyondDeadzone) {
                                sendClick(HidConstants.MOUSE_BUTTON_RIGHT)
                            }
                            hasTriggeredThreeFingerGesture = false
                            true
                        }

                        MotionEvent.ACTION_CANCEL -> {
                            gestureInterpreter.setDragging(false)
                            hasTriggeredThreeFingerGesture = false
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
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "1 finger: move / click  •  2: scroll  •  3: desktops / overview",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted.copy(alpha = 0.7f)
                )
            }
        }

        // BOTTOM: Mouse Buttons Bar (LEFT, MIDDLE, RIGHT, DRAG LOCK)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Drag Lock Toggle
            Box(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isDragLocked) PrimaryBlue.copy(alpha = 0.2f) else DarkSurfaceVariant)
                    .border(1.dp, if (isDragLocked) PrimaryBlue else DarkBorder, RoundedCornerShape(8.dp))
                    .clickable {
                        isDragLocked = !isDragLocked
                        triggerHaptic()
                        if (isDragLocked) {
                            transport?.sendMouseMove(0, 0, HidConstants.MOUSE_BUTTON_LEFT, 0)
                        } else {
                            transport?.sendMouseMove(0, 0, HidConstants.MOUSE_BUTTON_NONE, 0)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (isDragLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "Drag Lock",
                        tint = if (isDragLocked) PrimaryBlue else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "LOCK",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (isDragLocked) PrimaryBlue else TextSecondary
                    )
                }
            }

            // Left Click
            TouchMouseButton(
                label = "LEFT",
                modifier = Modifier.weight(2f),
                isPrimary = true,
                onClick = { sendClick(HidConstants.MOUSE_BUTTON_LEFT) }
            )

            // Middle Click
            TouchMouseButton(
                label = "MIDDLE",
                modifier = Modifier.weight(1.3f),
                onClick = { sendClick(HidConstants.MOUSE_BUTTON_MIDDLE) }
            )

            // Right Click
            TouchMouseButton(
                label = "RIGHT",
                modifier = Modifier.weight(2f),
                onClick = { sendClick(HidConstants.MOUSE_BUTTON_RIGHT) }
            )
        }
    }
}

/**
 * Top Connection Status Card
 */
@Composable
private fun MouseConnectionStatusCard(
    connectionState: ConnectionState,
    onPairClick: () -> Unit,
    onHostInfoClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val (statusDot, statusText, hostName) = when (connectionState) {
        is ConnectionState.Connected -> Triple(
            StatusConnected,
            "Connected",
            connectionState.device.name ?: connectionState.device.address
        )
        is ConnectionState.Connecting -> Triple(
            StatusConnecting,
            "Connecting…",
            connectionState.device?.name ?: "Searching for Host…"
        )
        is ConnectionState.Disconnecting -> Triple(
            StatusConnecting,
            "Disconnecting…",
            "Terminating session"
        )
        is ConnectionState.Error -> Triple(
            Color(0xFFEF4444),
            "Connection Error",
            connectionState.message
        )
        is ConnectionState.Disconnected -> Triple(
            StatusDisconnected,
            "Disconnected",
            "No active host"
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Status
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusDot)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = hostName,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = if (connectionState is ConnectionState.Connected) PrimaryBlue else TextSecondary,
                    maxLines = 1
                )
            }

            // Right Actions: Pair / Host Info / Settings
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceCard)
                        .border(1.dp, DarkBorder, RoundedCornerShape(6.dp))
                        .clickable(onClick = onPairClick)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (connectionState is ConnectionState.Connected) "Switch" else "Pair",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                }

                IconButton(onClick = onHostInfoClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Host Info",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(onClick = onSettingsClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Settings",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TouchMouseButton(
    label: String,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val bgColor = when {
        isPressed -> PrimaryBlue.copy(alpha = 0.3f)
        isPrimary -> SurfaceCard
        else -> DarkSurfaceVariant
    }
    val borderColor = if (isPressed || isPrimary) PrimaryBlue.copy(alpha = 0.5f) else DarkBorder

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = if (isPrimary) PrimaryBlue else TextPrimary
        )
    }
}
