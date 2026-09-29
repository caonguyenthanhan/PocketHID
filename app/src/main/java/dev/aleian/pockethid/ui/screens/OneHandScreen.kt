package dev.aleian.pockethid.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aleian.pockethid.action.ActionDispatcher
import dev.aleian.pockethid.action.PocketAction
import dev.aleian.pockethid.model.ConnectionState
import dev.aleian.pockethid.model.HidConstants
import dev.aleian.pockethid.model.PocketStrings
import dev.aleian.pockethid.model.SettingsRepository
import dev.aleian.pockethid.transport.InputTransport
import dev.aleian.pockethid.ui.theme.AccentAmber
import dev.aleian.pockethid.ui.theme.DarkBg
import dev.aleian.pockethid.ui.theme.DarkBorder
import dev.aleian.pockethid.ui.theme.DarkSurface
import dev.aleian.pockethid.ui.theme.DarkSurfaceVariant
import dev.aleian.pockethid.ui.theme.OnPrimaryContainer
import dev.aleian.pockethid.ui.theme.PrimaryBlue
import dev.aleian.pockethid.ui.theme.PrimaryContainer
import dev.aleian.pockethid.ui.theme.StatusConnected
import dev.aleian.pockethid.ui.theme.SurfaceCard
import dev.aleian.pockethid.ui.theme.SurfaceContainer
import dev.aleian.pockethid.ui.theme.SurfaceContainerHigh
import dev.aleian.pockethid.ui.theme.SurfaceContainerLowest
import dev.aleian.pockethid.ui.theme.TextMuted
import dev.aleian.pockethid.ui.theme.TextPrimary
import dev.aleian.pockethid.ui.theme.TextSecondary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class OneHandSubMode {
    WEB,
    VIDEO
}

/**
 * Dedicated One-Hand Thumb Interaction Mode for PocketHID.
 * Allows effortless browsing and video playback control using primarily one thumb.
 */
@Composable
fun OneHandScreen(
    transport: InputTransport?,
    connectionState: ConnectionState,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val settings by SettingsRepository.settings.collectAsState()

    var subMode by rememberSaveable { mutableStateOf(OneHandSubMode.WEB) }
    var handedness by rememberSaveable { mutableStateOf(settings.oneHandHandedness) }

    // Transient HUD Banner feedback (e.g. "Scroll Down ↓", "Seek +10s ⏩")
    var feedbackText by remember { mutableStateOf("") }
    var feedbackTime by remember { mutableLongStateOf(0L) }

    fun triggerFeedback(text: String, isLightHaptic: Boolean = true) {
        feedbackText = text
        feedbackTime = System.currentTimeMillis()
        if (isLightHaptic && settings.keyboardHaptics) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    // Auto-dismiss HUD feedback after 1.4 seconds
    var isFeedbackVisible by remember { mutableStateOf(false) }
    isFeedbackVisible = (System.currentTimeMillis() - feedbackTime) < 1400L && feedbackText.isNotEmpty()

    fun dispatchAction(action: PocketAction, feedback: String = "", isContinuous: Boolean = false) {
        if (connectionState !is ConnectionState.Connected && transport?.isConnected != true) {
            triggerFeedback(PocketStrings.noHostConnectedPrompt(settings.language), isLightHaptic = true)
            return
        }
        if (feedback.isNotEmpty()) {
            triggerFeedback(feedback)
        }
        scope.launch {
            ActionDispatcher.dispatch(
                action = action,
                transport = transport,
                hostOs = settings.hostOs,
                zoomMode = settings.zoomMode,
                playFeedback = !isContinuous
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // TOP CONTROL DECK: Segmented Mode Selector & Handedness Switcher
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceContainerLowest)
                    .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Segmented Selector: [ WEB ] [ VIDEO ]
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceVariant)
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (subMode == OneHandSubMode.WEB) PrimaryContainer else Color.Transparent)
                            .clickable {
                                subMode = OneHandSubMode.WEB
                                triggerFeedback(PocketStrings.oneHandWebMode(settings.language))
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = if (subMode == OneHandSubMode.WEB) OnPrimaryContainer else TextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "WEB",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (subMode == OneHandSubMode.WEB) OnPrimaryContainer else TextSecondary
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (subMode == OneHandSubMode.VIDEO) PrimaryContainer else Color.Transparent)
                            .clickable {
                                subMode = OneHandSubMode.VIDEO
                                triggerFeedback(PocketStrings.oneHandVideoMode(settings.language))
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                imageVector = Icons.Default.Tv,
                                contentDescription = null,
                                tint = if (subMode == OneHandSubMode.VIDEO) OnPrimaryContainer else TextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "VIDEO",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (subMode == OneHandSubMode.VIDEO) OnPrimaryContainer else TextSecondary
                            )
                        }
                    }
                }

                // Quick Handedness Toggle (Left vs Right Hand)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceCard)
                        .clickable {
                            val newHand = if (handedness.equals("Right", ignoreCase = true)) "Left" else "Right"
                            handedness = newHand
                            SettingsRepository.updateSettings(settings.copy(oneHandHandedness = newHand))
                            triggerFeedback(if (newHand == "Right") PocketStrings.oneHandRightHand(settings.language) else PocketStrings.oneHandLeftHand(settings.language))
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            imageVector = Icons.Default.PanTool,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (handedness.equals("Right", ignoreCase = true)) "${PocketStrings.oneHandRightHand(settings.language)} ✋" else "✋ ${PocketStrings.oneHandLeftHand(settings.language)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                    }
                }
            }

            // HUD Feedback Bar (Host is in TopCommandBar; feedback pill displays prominently when active)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedVisibility(
                    visible = isFeedbackVisible,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(PrimaryContainer)
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = feedbackText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnPrimaryContainer
                        )
                    }
                }
            }
        }

        // CENTER: ERGONOMIC THUMB INTERACTION SURFACE
        // Positioned in the lower-middle with handedness padding bias
        val isRightHanded = handedness.equals("Right", ignoreCase = true)
        val surfacePaddingStart = if (isRightHanded) 16.dp else 4.dp
        val surfacePaddingEnd = if (isRightHanded) 4.dp else 16.dp

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(start = surfacePaddingStart, end = surfacePaddingEnd, top = 6.dp, bottom = 6.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            SurfaceContainerHigh,
                            SurfaceContainerLowest
                        )
                    )
                )
                .border(1.5.dp, PrimaryBlue.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                .pointerInput(subMode, isRightHanded, settings) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startPos = down.position
                        val startTime = System.currentTimeMillis()
                        var currentPos = startPos
                        var totalDx = 0f
                        var totalDy = 0f
                        var gestureLocked = false
                        var isScrollMode = false
                        var isSwipeXMode = false
                        var isVolumeMode = false
                        var isSeekMode = false
                        var accumulatedWheel = 0f
                        var lastWheelTime = 0L

                        var holdJob: Job? = null

                        // Hold-to-repeat for Video mode
                        if (subMode == OneHandSubMode.VIDEO) {
                            holdJob = scope.launch {
                                delay(450L)
                                // If still holding without large drag, start repeated seek
                                val isLeftSide = startPos.x < (size.width / 2f)
                                while (true) {
                                    if (isLeftSide) {
                                        dispatchAction(PocketAction.VideoAction.SeekBackward, PocketStrings.oneHandSeekBack(settings.language), isContinuous = true)
                                    } else {
                                        dispatchAction(PocketAction.VideoAction.SeekForward, PocketStrings.oneHandSeekForward(settings.language), isContinuous = true)
                                    }
                                    delay(220L)
                                }
                            }
                        }

                        do {
                            val event = awaitPointerEvent()
                            val pointerChange = event.changes.firstOrNull { it.id == down.id } ?: break

                            if (pointerChange.positionChange() != androidx.compose.ui.geometry.Offset.Zero) {
                                val change = pointerChange.positionChange()
                                currentPos = pointerChange.position
                                totalDx += change.x
                                totalDy += change.y

                                val dist = kotlin.math.sqrt(totalDx * totalDx + totalDy * totalDy)

                                if (dist > 18f && !gestureLocked) {
                                    holdJob?.cancel()
                                    holdJob = null

                                    if (subMode == OneHandSubMode.WEB) {
                                        if (abs(totalDy) > abs(totalDx)) {
                                            isScrollMode = true
                                            gestureLocked = true
                                        } else {
                                            isSwipeXMode = true
                                            gestureLocked = true
                                        }
                                    } else {
                                        // VIDEO mode
                                        if (abs(totalDy) > abs(totalDx)) {
                                            isVolumeMode = true
                                            gestureLocked = true
                                        } else {
                                            isSeekMode = true
                                            gestureLocked = true
                                        }
                                    }
                                }

                                // Handle locked gesture movements
                                if (isScrollMode && subMode == OneHandSubMode.WEB) {
                                    pointerChange.consume()
                                    val now = System.currentTimeMillis()
                                    // Smooth real mouse wheel HID
                                    val scrollFactor = if (settings.naturalScroll) 1f else -1f
                                    accumulatedWheel += change.y * settings.oneHandScrollSensitivity * scrollFactor

                                    if (abs(accumulatedWheel) >= 16f && (now - lastWheelTime > 18L)) {
                                        val wheelStep = if (accumulatedWheel > 0) 1 else -1
                                        transport?.sendMouseMove(0, 0, HidConstants.MOUSE_BUTTON_NONE, wheelStep)
                                        accumulatedWheel = 0f
                                        lastWheelTime = now
                                        feedbackText = if (wheelStep < 0) PocketStrings.oneHandFeedbackScrollDown(settings.language) else PocketStrings.oneHandFeedbackScrollUp(settings.language)
                                        feedbackTime = now
                                    }
                                } else if (isSwipeXMode && subMode == OneHandSubMode.WEB) {
                                    if (abs(totalDx) > 55f) {
                                        pointerChange.consume()
                                        if (totalDx < 0) {
                                            dispatchAction(PocketAction.WebAction.Back, PocketStrings.oneHandFeedbackBack(settings.language))
                                        } else {
                                            dispatchAction(PocketAction.WebAction.Forward, PocketStrings.oneHandFeedbackForward(settings.language))
                                        }
                                        // Once emitted, lock out further emits for this swipe
                                        isSwipeXMode = false
                                    }
                                } else if (isVolumeMode && subMode == OneHandSubMode.VIDEO) {
                                    if (abs(totalDy) > 42f) {
                                        pointerChange.consume()
                                        if (totalDy < 0) {
                                            dispatchAction(PocketAction.VideoAction.VolumeUp, PocketStrings.oneHandFeedbackVolumeUp(settings.language), isContinuous = true)
                                        } else {
                                            dispatchAction(PocketAction.VideoAction.VolumeDown, PocketStrings.oneHandFeedbackVolumeDown(settings.language), isContinuous = true)
                                        }
                                        totalDy = 0f // Reset for stepped volume control
                                    }
                                } else if (isSeekMode && subMode == OneHandSubMode.VIDEO) {
                                    if (abs(totalDx) > 48f) {
                                        pointerChange.consume()
                                        if (totalDx < 0) {
                                            dispatchAction(PocketAction.VideoAction.SeekBackward, PocketStrings.oneHandSeekBack(settings.language), isContinuous = true)
                                        } else {
                                            dispatchAction(PocketAction.VideoAction.SeekForward, PocketStrings.oneHandSeekForward(settings.language), isContinuous = true)
                                        }
                                        totalDx = 0f // Reset for stepped seek control
                                    }
                                }
                            }
                        } while (event.changes.any { it.id == down.id && it.pressed })

                        holdJob?.cancel()

                        val duration = System.currentTimeMillis() - startTime
                        val totalDist = kotlin.math.sqrt(totalDx * totalDx + totalDy * totalDy)

                        // Tap classification (quick, non-drag)
                        if (!gestureLocked && duration < 280L && totalDist < 20f) {
                            if (subMode == OneHandSubMode.WEB) {
                                dispatchAction(PocketAction.PointerAction.LeftClick, PocketStrings.oneHandFeedbackLeftClick(settings.language))
                            } else {
                                dispatchAction(PocketAction.VideoAction.PlayPause, PocketStrings.oneHandFeedbackPlayPause(settings.language))
                            }
                        }
                    }
                }
        ) {
            // Visual guidance inside Thumb Surface
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Action Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (subMode == OneHandSubMode.WEB) PocketStrings.oneHandSwipeUpScroll(settings.language) else PocketStrings.oneHandSwipeUpVolume(settings.language),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted
                    )
                }

                // Middle Indicators (Left / Center / Right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (subMode == OneHandSubMode.WEB) PocketStrings.oneHandBack(settings.language) else PocketStrings.oneHandSeekBack(settings.language),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted
                    )

                    // Center Touch Target Icon
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(SurfaceCard.copy(alpha = 0.6f))
                            .border(1.dp, PrimaryBlue.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (subMode == OneHandSubMode.WEB) Icons.Default.PanTool else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = if (subMode == OneHandSubMode.WEB) PocketStrings.oneHandTapClick(settings.language) else PocketStrings.oneHandTapPlay(settings.language),
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                lineHeight = 10.sp
                            )
                        }
                    }

                    Text(
                        text = if (subMode == OneHandSubMode.WEB) PocketStrings.oneHandForward(settings.language) else PocketStrings.oneHandSeekForward(settings.language),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted
                    )
                }

                // Bottom Action Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (subMode == OneHandSubMode.WEB) PocketStrings.oneHandSwipeDownScroll(settings.language) else PocketStrings.oneHandSwipeDownVolume(settings.language),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted
                    )
                }
            }
        }

        // BOTTOM: QUICK ACTION CONTROLLER (Comfortably within thumb arc)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceContainerLowest)
                .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
                .padding(6.dp)
        ) {
            if (subMode == OneHandSubMode.WEB) {
                // WEB QUICK ACTIONS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OneHandQuickButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        label = PocketStrings.oneHandBack(settings.language),
                        onClick = { dispatchAction(PocketAction.WebAction.Back, PocketStrings.oneHandFeedbackBack(settings.language)) }
                    )
                    OneHandQuickButton(
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        label = PocketStrings.oneHandForward(settings.language),
                        onClick = { dispatchAction(PocketAction.WebAction.Forward, PocketStrings.oneHandFeedbackForward(settings.language)) }
                    )
                    OneHandQuickButton(
                        icon = Icons.Default.Home,
                        label = PocketStrings.oneHandWebHome(settings.language),
                        onClick = { dispatchAction(PocketAction.WebAction.Home, PocketStrings.oneHandWebHome(settings.language)) }
                    )
                    OneHandQuickButton(
                        icon = Icons.Default.Refresh,
                        label = PocketStrings.oneHandWebReload(settings.language),
                        onClick = { dispatchAction(PocketAction.WebAction.Refresh, PocketStrings.oneHandWebReload(settings.language)) }
                    )
                    OneHandQuickButton(
                        icon = Icons.Default.Add,
                        label = "+ Tab",
                        onClick = { dispatchAction(PocketAction.WebAction.NewTab, "New Tab") }
                    )
                }
            } else {
                // VIDEO QUICK ACTIONS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OneHandQuickButton(
                        icon = Icons.Default.SkipPrevious,
                        label = PocketStrings.oneHandVideoPrev(settings.language),
                        onClick = { dispatchAction(PocketAction.VideoAction.PrevTrack, PocketStrings.oneHandVideoPrev(settings.language)) }
                    )
                    OneHandQuickButton(
                        icon = Icons.Default.FastRewind,
                        label = "-10s",
                        onClick = { dispatchAction(PocketAction.VideoAction.SeekBackward, PocketStrings.oneHandSeekBack(settings.language)) }
                    )
                    OneHandQuickButton(
                        icon = Icons.Default.PlayArrow,
                        label = PocketStrings.oneHandVideoPlayPause(settings.language),
                        isAccent = true,
                        onClick = { dispatchAction(PocketAction.VideoAction.PlayPause, PocketStrings.oneHandFeedbackPlayPause(settings.language)) }
                    )
                    OneHandQuickButton(
                        icon = Icons.Default.FastForward,
                        label = "+10s",
                        onClick = { dispatchAction(PocketAction.VideoAction.SeekForward, PocketStrings.oneHandSeekForward(settings.language)) }
                    )
                    OneHandQuickButton(
                        icon = Icons.Default.SkipNext,
                        label = PocketStrings.oneHandVideoNext(settings.language),
                        onClick = { dispatchAction(PocketAction.VideoAction.NextTrack, PocketStrings.oneHandVideoNext(settings.language)) }
                    )
                    OneHandQuickButton(
                        icon = Icons.AutoMirrored.Filled.VolumeMute,
                        label = PocketStrings.oneHandVideoMute(settings.language),
                        onClick = { dispatchAction(PocketAction.VideoAction.Mute, PocketStrings.oneHandVideoMute(settings.language)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun OneHandQuickButton(
    icon: ImageVector,
    label: String,
    isAccent: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isAccent) PrimaryContainer else SurfaceCard)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isAccent) OnPrimaryContainer else TextPrimary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = if (isAccent) FontWeight.Bold else FontWeight.Medium,
                color = if (isAccent) OnPrimaryContainer else TextSecondary
            )
        }
    }
}
