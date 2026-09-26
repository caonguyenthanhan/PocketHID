package dev.aleian.pockethid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aleian.pockethid.model.HidConstants
import dev.aleian.pockethid.ui.theme.DarkBorder
import dev.aleian.pockethid.ui.theme.DarkSurfaceVariant
import dev.aleian.pockethid.ui.theme.PrimaryBlue
import dev.aleian.pockethid.ui.theme.TextMuted
import dev.aleian.pockethid.ui.theme.TextPrimary

/**
 * System and Auxiliary Controls: [ SELECT ] [ GUIDE ] [ START ] and [ L3 ] [ R3 ]
 */
@Composable
fun VirtualSystemButtons(
    onPress: (Int) -> Unit,
    onRelease: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SystemButton(
            label = "SELECT",
            buttonMask = HidConstants.GAMEPAD_BTN_BACK,
            onPress = onPress,
            onRelease = onRelease
        )

        SystemButton(
            label = "L3",
            buttonMask = HidConstants.GAMEPAD_BTN_L3,
            width = 40,
            onPress = onPress,
            onRelease = onRelease
        )

        SystemButton(
            label = "GUIDE",
            buttonMask = HidConstants.GAMEPAD_BTN_GUIDE,
            onPress = onPress,
            onRelease = onRelease
        )

        SystemButton(
            label = "R3",
            buttonMask = HidConstants.GAMEPAD_BTN_R3,
            width = 40,
            onPress = onPress,
            onRelease = onRelease
        )

        SystemButton(
            label = "START",
            buttonMask = HidConstants.GAMEPAD_BTN_START,
            onPress = onPress,
            onRelease = onRelease
        )
    }
}

@Composable
fun SystemButton(
    label: String,
    buttonMask: Int,
    width: Int = 60,
    onPress: (Int) -> Unit,
    onRelease: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .width(width.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isPressed) PrimaryBlue.copy(alpha = 0.35f) else DarkSurfaceVariant)
            .border(
                width = 1.dp,
                color = if (isPressed) PrimaryBlue else DarkBorder,
                shape = RoundedCornerShape(14.dp)
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
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = if (isPressed) PrimaryBlue else TextMuted
        )
    }
}
