package dev.aleian.pockethid.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aleian.pockethid.model.HidConstants
import dev.aleian.pockethid.model.SettingsRepository
import dev.aleian.pockethid.transport.InputTransport
import dev.aleian.pockethid.ui.theme.DarkBorder
import dev.aleian.pockethid.ui.theme.DarkSurface
import dev.aleian.pockethid.ui.theme.DarkSurfaceVariant
import dev.aleian.pockethid.ui.theme.PrimaryBlue
import dev.aleian.pockethid.ui.theme.SurfaceCard
import dev.aleian.pockethid.ui.theme.TextMuted
import dev.aleian.pockethid.ui.theme.TextPrimary
import dev.aleian.pockethid.ui.theme.TextSecondary
import kotlinx.coroutines.launch

import android.view.MotionEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.pointerInteropFilter

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PresenterScreen(
    transport: InputTransport?,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val settings by SettingsRepository.settings.collectAsState()

    var showPointerPad by remember { mutableStateOf(false) }
    var lastPointerX by remember { mutableStateOf(0f) }
    var lastPointerY by remember { mutableStateOf(0f) }

    fun triggerHaptic() {
        if (settings.keyboardHaptics) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        }
    }

    fun sendKey(keyCode: Byte, modifiers: Byte = 0) {
        triggerHaptic()
        scope.launch {
            transport?.sendKeyClick(keyCode, modifiers)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Primary Slide Control Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PresenterUtilityButton(
                icon = Icons.Default.PlayArrow,
                label = "START F5",
                modifier = Modifier.weight(1f)
            ) {
                sendKey(HidConstants.KEY_F5)
            }

            PresenterUtilityButton(
                icon = Icons.Default.FastForward,
                label = "RESUME ⇧F5",
                modifier = Modifier.weight(1f)
            ) {
                sendKey(HidConstants.KEY_F5, HidConstants.MOD_LEFT_SHIFT)
            }

            PresenterUtilityButton(
                icon = Icons.Default.Close,
                label = "EXIT ESC",
                modifier = Modifier.weight(1f)
            ) {
                sendKey(HidConstants.KEY_ESC)
            }
        }

        // Secondary Utility Bar: Screen Blanking & Laser Pointer Mode Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PresenterUtilityButton(
                icon = Icons.Default.VisibilityOff,
                label = "BLACK (B)",
                modifier = Modifier.weight(1f)
            ) {
                sendKey(HidConstants.KEY_B)
            }

            PresenterUtilityButton(
                icon = Icons.Default.Lightbulb,
                label = "WHITE (W)",
                modifier = Modifier.weight(1f)
            ) {
                sendKey(HidConstants.KEY_W)
            }

            PresenterUtilityButton(
                icon = Icons.Default.Highlight,
                label = if (showPointerPad) "POINTER ON" else "POINTER",
                modifier = Modifier.weight(1f)
            ) {
                triggerHaptic()
                showPointerPad = !showPointerPad
            }
        }

        // Optional Laser Pointer Trackpad Zone
        AnimatedVisibility(visible = showPointerPad) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, PrimaryBlue.copy(alpha = 0.8f), RoundedCornerShape(10.dp))
                    .pointerInteropFilter { event ->
                        when (event.actionMasked) {
                            MotionEvent.ACTION_DOWN -> {
                                lastPointerX = event.x
                                lastPointerY = event.y
                                true
                            }
                            MotionEvent.ACTION_MOVE -> {
                                val dx = (event.x - lastPointerX).toInt()
                                val dy = (event.y - lastPointerY).toInt()
                                if (dx != 0 || dy != 0) {
                                    transport?.sendMouseMove(dx, dy, HidConstants.MOUSE_BUTTON_NONE, 0)
                                }
                                lastPointerX = event.x
                                lastPointerY = event.y
                                true
                            }
                            else -> true
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Highlight,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "LASER POINTER TRACKPAD",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                    Text(
                        text = "Glide thumb to guide laser cursor on slide",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted
                    )
                }
            }
        }

        // Primary Large Slides Navigation Deck (Two Massive Thumb Buttons)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Previous Slide Button (Left Half)
            PresenterSlideButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                title = "PREVIOUS",
                subtitle = "Page Up / Left Arrow",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                sendKey(HidConstants.KEY_LEFT)
            }

            // Next Slide Button (Right Half - Primary Highlight)
            PresenterSlideButton(
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                title = "NEXT",
                subtitle = "Space / Right Arrow",
                isPrimary = true,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                sendKey(HidConstants.KEY_RIGHT)
            }
        }

        // Bottom Quick Slides Utilities
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(DarkSurfaceVariant)
                .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DesktopWindows,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PRESENTER REMOTE MODE",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            Text(
                text = "Keynote / PPT / Slides",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun PresenterSlideButton(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    onClick: () -> Unit
) {
    val bgColor = if (isPrimary) DarkSurfaceVariant else DarkSurface
    val borderColor = if (isPrimary) PrimaryBlue.copy(alpha = 0.6f) else DarkBorder

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(if (isPrimary) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isPrimary) PrimaryBlue else TextPrimary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (isPrimary) PrimaryBlue else TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun PresenterUtilityButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
    }
}
