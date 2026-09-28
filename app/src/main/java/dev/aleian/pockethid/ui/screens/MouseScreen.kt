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
import dev.aleian.pockethid.ui.components.MultiTouchTrackpad
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(10.dp)
    ) {
        // MAIN: Precision Multi-Touch Trackpad with Gesture Engine and Explicit Buttons
        MultiTouchTrackpad(
            transport = transport,
            settings = settings,
            canSendInput = { canSendInput() },
            onOpenCommandDeck = { onSettingsClick() },
            modifier = Modifier.fillMaxSize()
        )
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
