package dev.aleian.pockethid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aleian.pockethid.model.HidConstants
import dev.aleian.pockethid.ui.theme.DarkBorder
import dev.aleian.pockethid.ui.theme.DarkSurface
import dev.aleian.pockethid.ui.theme.DarkSurfaceVariant

/**
 * Diamond Face Button Cluster:
 *        [ Y ]
 *   [ X ]     [ B ]
 *        [ A ]
 * Supports simultaneous multi-finger touches with immediate down/up callbacks.
 */
@Composable
fun VirtualFaceButtons(
    size: Dp = 150.dp,
    buttonSize: Dp = 46.dp,
    onPress: (Int) -> Unit,
    onRelease: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val offsetDistance = (size - buttonSize) / 2

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Y - Top (Amber / Gold)
        GamepadFaceButton(
            label = "Y",
            accentColor = Color(0xFFFFB300),
            buttonMask = HidConstants.GAMEPAD_BTN_Y,
            size = buttonSize,
            modifier = Modifier.offset(y = -offsetDistance),
            onPress = onPress,
            onRelease = onRelease
        )

        // X - Left (Cyan / Blue)
        GamepadFaceButton(
            label = "X",
            accentColor = Color(0xFF29B6F6),
            buttonMask = HidConstants.GAMEPAD_BTN_X,
            size = buttonSize,
            modifier = Modifier.offset(x = -offsetDistance),
            onPress = onPress,
            onRelease = onRelease
        )

        // B - Right (Crimson / Red)
        GamepadFaceButton(
            label = "B",
            accentColor = Color(0xFFEF5350),
            buttonMask = HidConstants.GAMEPAD_BTN_B,
            size = buttonSize,
            modifier = Modifier.offset(x = offsetDistance),
            onPress = onPress,
            onRelease = onRelease
        )

        // A - Bottom (Emerald / Green)
        GamepadFaceButton(
            label = "A",
            accentColor = Color(0xFF66BB6A),
            buttonMask = HidConstants.GAMEPAD_BTN_A,
            size = buttonSize,
            modifier = Modifier.offset(y = offsetDistance),
            onPress = onPress,
            onRelease = onRelease
        )
    }
}

@Composable
fun GamepadFaceButton(
    label: String,
    accentColor: Color,
    buttonMask: Int,
    size: Dp = 46.dp,
    modifier: Modifier = Modifier,
    onPress: (Int) -> Unit,
    onRelease: (Int) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (isPressed) accentColor.copy(alpha = 0.28f) else DarkSurfaceVariant)
            .border(
                width = if (isPressed) 2.dp else 1.5.dp,
                color = if (isPressed) accentColor else DarkBorder,
                shape = CircleShape
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
            fontSize = 17.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Black,
            color = if (isPressed) accentColor else accentColor.copy(alpha = 0.85f)
        )
    }
}
