package dev.aleian.pockethid.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.ArrowLeft
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import android.view.MotionEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aleian.pockethid.model.ConnectionState
import dev.aleian.pockethid.ui.theme.DarkBorder
import dev.aleian.pockethid.ui.theme.DarkSurface
import dev.aleian.pockethid.ui.theme.DarkSurfaceVariant
import dev.aleian.pockethid.ui.theme.PrimaryBlue
import dev.aleian.pockethid.ui.theme.StatusConnected
import dev.aleian.pockethid.ui.theme.StatusConnecting
import dev.aleian.pockethid.ui.theme.StatusDisconnected
import dev.aleian.pockethid.ui.theme.SurfaceCard
import dev.aleian.pockethid.ui.theme.SurfaceContainer
import dev.aleian.pockethid.ui.theme.SurfaceRaised
import dev.aleian.pockethid.ui.theme.TextMuted
import dev.aleian.pockethid.ui.theme.TextPrimary
import dev.aleian.pockethid.ui.theme.TextSecondary

enum class ModifierToggleState {
    OFF,
    STICKY,
    LOCKED
}

/**
 * Top Command Bar: Global Application Shell Header
 */
@Composable
fun TopCommandBar(
    connectionState: ConnectionState,
    hapticEnabled: Boolean,
    onRotateClick: () -> Unit,
    onHostClick: () -> Unit,
    onToggleHaptic: () -> Unit,
    onDiagnosticsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSearchClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(DarkSurface)
            .border(1.dp, DarkBorder)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: Clean Rotate Icon (Compact, professional, non-text)
        IconButton(
            onClick = onRotateClick,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.RotateRight,
                contentDescription = "Rotate Layout",
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }

        // Center: Connection Status Pill
        val (statusDotColor, hostTitle) = when (connectionState) {
            is ConnectionState.Connected -> Pair(StatusConnected, connectionState.device.name ?: "Connected Host")
            is ConnectionState.Connecting -> Pair(StatusConnecting, "Connecting…")
            is ConnectionState.Disconnecting -> Pair(StatusConnecting, "Disconnecting…")
            is ConnectionState.Error -> Pair(Color(0xFFEF4444), "Connection Error")
            is ConnectionState.Disconnected -> Pair(StatusDisconnected, "Disconnected (Tap to Connect)")
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(DarkSurfaceVariant)
                .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                .clickable(onClick = onHostClick)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(statusDotColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = hostTitle,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1
                )
            }
        }

        // Right Utilities: Search/Palette + Haptic Toggle + Telemetry/HUD + Settings
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            // Command Palette / Search Icon
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Open Command Palette",
                    tint = PrimaryBlue,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Haptic toggle indicator
            IconButton(
                onClick = onToggleHaptic,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = "Toggle Haptic Feedback",
                    tint = if (hapticEnabled) PrimaryBlue else TextMuted,
                    modifier = Modifier.size(17.dp)
                )
            }

            // Diagnostics HUD Icon
            IconButton(
                onClick = onDiagnosticsClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.QueryStats,
                    contentDescription = "Open Diagnostics HUD",
                    tint = TextSecondary,
                    modifier = Modifier.size(17.dp)
                )
            }

            // Settings Icon
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Open Settings",
                    tint = TextSecondary,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

/**
 * Main 4-Mode Global Navigation Switcher: KEYBOARD | MOUSE | GAMEPAD | PRESENTER
 */
@Composable
fun DeckModeSwitcher(
    selectedMode: Int, // 0: Keyboard, 1: Mouse, 2: Gamepad, 3: Presenter
    onSelectMode: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = listOf("KEYBOARD", "MOUSE", "GAMEPAD", "PRESENTER", "ONE-HAND")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .background(DarkSurfaceVariant)
            .border(1.dp, DarkBorder)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        modes.forEachIndexed { index, title ->
            val isSelected = selectedMode == index
            val animBg by animateColorAsState(
                targetValue = if (isSelected) DarkSurface else Color.Transparent,
                label = "ModeBgAnim"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) PrimaryBlue else TextMuted,
                label = "ModeTextAnim"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(animBg)
                    .clickable { onSelectMode(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = textColor
                )
            }
        }
    }
}

/**
 * Sub-mode Selector inside Keyboard: TYPE | SHORTCUTS | MEDIA | SYSTEM | F-KEYS | NUMPAD
 */
@Composable
fun SubModeSelector(
    selectedSubMode: Int, // 0: Type, 1: Shortcuts, 2: Media, 3: System, 4: F-Keys, 5: Numpad
    onSelectSubMode: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val subModes = listOf("TYPE", "SHORTCUTS", "MEDIA", "SYSTEM", "F-KEYS", "NUMPAD")

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, DarkBorder, RoundedCornerShape(6.dp))
            .padding(2.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        subModes.forEachIndexed { index, name ->
            val isSelected = selectedSubMode == index
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isSelected) DarkSurface else Color.Transparent)
                    .clickable { onSelectSubMode(index) }
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) PrimaryBlue else TextMuted
                )
            }
        }
    }
}

/**
 * Ergonomic Touch Keycap Button with pressed micro-elevation
 */
@Composable
fun DeckKey(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = SurfaceCard,
    textColor: Color = TextPrimary,
    fontSize: TextUnit = 11.sp,
    hapticFeedback: Boolean = true,
    onClick: () -> Unit
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val actualBg = if (isPressed) containerColor.copy(alpha = 0.7f) else containerColor
    val actualBorder = if (isPressed) PrimaryBlue else DarkBorder

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(actualBg)
            .border(1.dp, actualBorder, RoundedCornerShape(5.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    if (hapticFeedback) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    }
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

/**
 * Ergonomic Touch Keycap Button with Tap + Hold-to-Repeat capability (ideal for Volume Up/Down, Backspace)
 */
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun DeckRepeatKey(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = SurfaceCard,
    textColor: Color = TextPrimary,
    fontSize: TextUnit = 11.sp,
    hapticFeedback: Boolean = true,
    repeatInitialDelayMs: Long = 350L,
    repeatIntervalMs: Long = 100L,
    enableRepeat: Boolean = true,
    onTrigger: () -> Unit,
    onRelease: () -> Unit = {}
) {
    val view = LocalView.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var isPressed by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var repeatJob by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<kotlinx.coroutines.Job?>(null) }

    val actualBg = if (isPressed) containerColor.copy(alpha = 0.7f) else containerColor
    val actualBorder = if (isPressed) PrimaryBlue else DarkBorder

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(actualBg)
            .border(1.dp, actualBorder, RoundedCornerShape(5.dp))
            .pointerInteropFilter { ev ->
                when (ev.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        isPressed = true
                        if (hapticFeedback) {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        }
                        onTrigger()

                        if (enableRepeat) {
                            repeatJob?.cancel()
                            repeatJob = scope.launch {
                                delay(repeatInitialDelayMs)
                                while (isActive) {
                                    if (hapticFeedback) {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    }
                                    onTrigger()
                                    delay(repeatIntervalMs)
                                }
                            }
                        }
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        isPressed = false
                        repeatJob?.cancel()
                        repeatJob = null
                        onRelease()
                        true
                    }
                    else -> true
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}


/**
 * Sticky Modifier Key for Thumb Zone (CTRL, ALT, SUPER, SHIFT)
 * Clear active indicator, no "OFF" text.
 */
@Composable
fun ThumbModifierKey(
    label: String,
    state: ModifierToggleState,
    modifier: Modifier = Modifier,
    hapticFeedback: Boolean = true,
    onClick: () -> Unit
) {
    val view = LocalView.current
    val (bgColor, borderColor, textColor, hasIndicator) = when (state) {
        ModifierToggleState.OFF -> listOf(SurfaceCard, DarkBorder, TextSecondary, false)
        ModifierToggleState.STICKY -> listOf(DarkSurfaceVariant, PrimaryBlue, PrimaryBlue, true)
        ModifierToggleState.LOCKED -> listOf(PrimaryBlue.copy(alpha = 0.25f), PrimaryBlue, PrimaryBlue, true)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(bgColor as Color)
            .border(1.dp, borderColor as Color, RoundedCornerShape(5.dp))
            .clickable {
                if (hapticFeedback) {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                }
                onClick()
            }
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = textColor as Color
            )
            if (hasIndicator as Boolean) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlue)
                )
            }
        }
    }
}

/**
 * Natural 4-Way Thumb Arrow Pad:
 *       [ ▲ ]
 * [ ◀ ] [ ▼ ] [ ▶ ]
 */
@Composable
fun NaturalThumbArrowPad(
    modifier: Modifier = Modifier,
    hapticFeedback: Boolean = true,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit
) {
    val view = LocalView.current
    fun tap(action: () -> Unit) {
        if (hapticFeedback) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        action()
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // UP
        ArrowPadKey(icon = Icons.Default.ArrowDropUp) { tap(onUp) }

        // LEFT, DOWN, RIGHT
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            ArrowPadKey(icon = Icons.Default.ArrowLeft) { tap(onLeft) }
            ArrowPadKey(icon = Icons.Default.ArrowDropDown) { tap(onDown) }
            ArrowPadKey(icon = Icons.Default.ArrowRight) { tap(onRight) }
        }
    }
}

@Composable
private fun ArrowPadKey(
    icon: ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(36.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(SurfaceCard)
            .border(1.dp, DarkBorder, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextPrimary,
            modifier = Modifier.size(18.dp)
        )
    }
}
