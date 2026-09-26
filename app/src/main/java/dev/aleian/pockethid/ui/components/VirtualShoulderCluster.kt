package dev.aleian.pockethid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aleian.pockethid.gamepad.GamepadMath
import dev.aleian.pockethid.model.HidConstants
import dev.aleian.pockethid.ui.theme.DarkBorder
import dev.aleian.pockethid.ui.theme.DarkSurface
import dev.aleian.pockethid.ui.theme.DarkSurfaceVariant
import dev.aleian.pockethid.ui.theme.PrimaryBlue
import dev.aleian.pockethid.ui.theme.TextMuted
import dev.aleian.pockethid.ui.theme.TextPrimary
import dev.aleian.pockethid.ui.theme.TextSecondary

/**
 * Top Shoulder & Trigger Bar:
 * Left: [ LB ] [ LT (Analog Meter) ]
 * Right: [ RT (Analog Meter) ] [ RB ]
 */
@Composable
fun VirtualShoulderCluster(
    triggerSensitivity: Float = 1.0f,
    onPressButton: (Int) -> Unit,
    onReleaseButton: (Int) -> Unit,
    onLeftTriggerChange: (Byte) -> Unit,
    onRightTriggerChange: (Byte) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // LEFT SHOULDER PAIR (LB + LT)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DigitalBumper(
                label = "LB",
                buttonMask = HidConstants.GAMEPAD_BTN_LB,
                onPress = onPressButton,
                onRelease = onReleaseButton
            )
            AnalogTrigger(
                label = "LT",
                sensitivity = triggerSensitivity,
                onValueChange = onLeftTriggerChange
            )
        }

        // RIGHT SHOULDER PAIR (RT + RB)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AnalogTrigger(
                label = "RT",
                sensitivity = triggerSensitivity,
                onValueChange = onRightTriggerChange
            )
            DigitalBumper(
                label = "RB",
                buttonMask = HidConstants.GAMEPAD_BTN_RB,
                onPress = onPressButton,
                onRelease = onReleaseButton
            )
        }
    }
}

@Composable
fun DigitalBumper(
    label: String,
    buttonMask: Int,
    onPress: (Int) -> Unit,
    onRelease: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .width(68.dp)
            .height(34.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isPressed) PrimaryBlue.copy(alpha = 0.3f) else DarkSurfaceVariant)
            .border(
                width = 1.dp,
                color = if (isPressed) PrimaryBlue else DarkBorder,
                shape = RoundedCornerShape(6.dp)
            )
            .pointerInput(buttonMask) {
                awaitEachGesture {
                    val down: PointerInputChange = awaitFirstDown(requireUnconsumed = false)
                    val pointerId = down.id
                    isPressed = true
                    onPress(buttonMask)

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId }

                        if (change == null || !change.pressed) {
                            isPressed = false
                            onRelease(buttonMask)
                            break
                        }
                        change.consume()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = if (isPressed) PrimaryBlue else TextPrimary
        )
    }
}

@Composable
fun AnalogTrigger(
    label: String,
    sensitivity: Float = 1.0f,
    onValueChange: (Byte) -> Unit,
    modifier: Modifier = Modifier
) {
    var fraction by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .width(84.dp)
            .height(34.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(DarkSurfaceVariant)
            .border(
                width = 1.dp,
                color = if (fraction > 0f) PrimaryBlue else DarkBorder,
                shape = RoundedCornerShape(6.dp)
            )
            .pointerInput(sensitivity) {
                awaitEachGesture {
                    val down: PointerInputChange = awaitFirstDown(requireUnconsumed = false)
                    val pointerId = down.id
                    val h = size.height.toFloat()

                    // Touch down: immediate 0.5f base or touch fraction
                    var curFrac = 1.0f
                    fraction = curFrac
                    onValueChange(GamepadMath.processTriggerInput(curFrac, sensitivity))

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId }

                        if (change == null || !change.pressed) {
                            fraction = 0f
                            onValueChange(0)
                            break
                        }

                        // Allow vertical drag modulation: dragging slightly adjusts from 0.3 to 1.0
                        val relativeY = (change.position.y / h).coerceIn(0.2f, 1.0f)
                        curFrac = relativeY
                        fraction = curFrac
                        onValueChange(GamepadMath.processTriggerInput(curFrac, sensitivity))
                        change.consume()
                    }
                }
            }
    ) {
        // Progress fill meter
        if (fraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .background(PrimaryBlue.copy(alpha = 0.35f))
            )
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (fraction > 0f) PrimaryBlue else TextPrimary
            )
            Text(
                text = if (fraction > 0f) "${(fraction * 100).toInt()}%" else "0%",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = if (fraction > 0f) PrimaryBlue else TextMuted
            )
        }
    }
}
