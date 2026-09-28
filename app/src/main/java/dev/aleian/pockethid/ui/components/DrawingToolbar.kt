package dev.aleian.pockethid.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aleian.pockethid.drawing.DrawingTabletController
import dev.aleian.pockethid.drawing.model.DrawingColor
import dev.aleian.pockethid.drawing.model.DrawingTargetMode
import dev.aleian.pockethid.drawing.model.DrawingTool
import dev.aleian.pockethid.ui.theme.DarkBorder
import dev.aleian.pockethid.ui.theme.DarkSurface
import dev.aleian.pockethid.ui.theme.DarkSurfaceVariant
import dev.aleian.pockethid.ui.theme.PrimaryBlue
import dev.aleian.pockethid.ui.theme.TextMuted
import dev.aleian.pockethid.ui.theme.TextPrimary
import dev.aleian.pockethid.ui.theme.TextSecondary

private val CyanAccent = Color(0xFF00E5FF)
private val CardBackground = Color(0xFF0D1524)

/**
 * Minimal Floating Toolbar for Graphics Tablet Mode.
 *
 * Replaces the heavy permanent horizontal bar with an elegant floating action button
 * that collapses by default to preserve 90-95% screen area for the drawing surface.
 */
@Composable
fun FloatingTabletToolbar(
    tabletController: DrawingTabletController,
    modifier: Modifier = Modifier
) {
    val state by tabletController.state.collectAsState()
    val isExpanded by tabletController.isToolbarExpanded.collectAsState()
    val view = LocalView.current

    fun performHaptic() {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomEnd
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // EXPANDED TOOL PANEL
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
            ) {
                Column(
                    modifier = Modifier
                        .width(170.dp)
                        .shadow(12.dp, RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp))
                        .background(CardBackground)
                        .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TABLET TOOLS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 1.sp
                        )
                        IconButton(
                            onClick = {
                                performHaptic()
                                tabletController.collapseToolbar()
                            },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    // 1. Tool Selection (PEN / ERASER)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CompactToolButton(
                            title = "PEN",
                            icon = Icons.Default.Edit,
                            selected = state.activeTool == DrawingTool.PEN,
                            onClick = {
                                performHaptic()
                                tabletController.setTool(DrawingTool.PEN)
                            },
                            modifier = Modifier.weight(1f)
                        )
                        CompactToolButton(
                            title = "ERASER",
                            icon = Icons.Default.LayersClear,
                            selected = state.activeTool == DrawingTool.ERASER,
                            onClick = {
                                performHaptic()
                                tabletController.setTool(DrawingTool.ERASER)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // 2. Undo / Redo Operations
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CompactActionButton(
                            title = "UNDO",
                            icon = Icons.AutoMirrored.Filled.Undo,
                            enabled = true,
                            onClick = {
                                performHaptic()
                                tabletController.undo()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        CompactActionButton(
                            title = "REDO",
                            icon = Icons.AutoMirrored.Filled.Redo,
                            enabled = true,
                            onClick = {
                                performHaptic()
                                tabletController.redo()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // 3. Compact Stroke Width Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceVariant)
                            .clickable {
                                performHaptic()
                                tabletController.cycleStrokeWidth()
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "STROKE SIZE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextMuted
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(state.strokeWidth.dp)
                                    .clip(CircleShape)
                                    .background(CyanAccent)
                            )
                            Text(
                                text = "${state.strokeWidth.toInt()}dp",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // 4. Color Palette
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "PREVIEW COLOR",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextMuted
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DrawingColor.entries.forEach { color ->
                                val isSelected = state.selectedColor == color
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color(color.argb))
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) CyanAccent else Color(0x33FFFFFF),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            performHaptic()
                                            tabletController.setColor(color)
                                        }
                                )
                            }
                        }
                    }

                    // 5. Target Mode Selection (Absolute Tablet / Relative Mouse)
                    val targetMode by tabletController.targetMode.collectAsState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceVariant)
                            .clickable {
                                performHaptic()
                                tabletController.toggleTargetMode()
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "MODE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextMuted
                        )
                        Text(
                            text = if (targetMode == DrawingTargetMode.ABSOLUTE_TABLET) "ABS TABLET" else "REL MOUSE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (targetMode == DrawingTargetMode.ABSOLUTE_TABLET) CyanAccent else Color(0xFFFFB74D),
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // 6. Clear Local Preview
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = state.strokes.isNotEmpty()) {
                                performHaptic()
                                tabletController.requestClear()
                            }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Clear",
                            tint = if (state.strokes.isNotEmpty()) Color(0xFFFF5252) else TextMuted.copy(alpha = 0.4f),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "CLEAR PREVIEW",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (state.strokes.isNotEmpty()) Color(0xFFFF5252) else TextMuted.copy(alpha = 0.4f)
                        )
                    }
                }
            }

            // COLLAPSED FLOATING ACTION BUTTON
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape)
                    .background(if (isExpanded) CyanAccent else CardBackground)
                    .border(1.5.dp, if (isExpanded) DarkSurface else CyanAccent, CircleShape)
                    .clickable {
                        performHaptic()
                        tabletController.toggleToolbar()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (state.activeTool == DrawingTool.PEN) Icons.Default.Edit else Icons.Default.LayersClear,
                    contentDescription = "Drawing Tools",
                    tint = if (isExpanded) DarkSurface else CyanAccent,
                    modifier = Modifier.size(20.dp)
                )

                // Current color pip indicator in corner
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 3.dp, end = 3.dp)
                        .clip(CircleShape)
                        .background(Color(state.selectedColor.argb))
                        .border(1.dp, DarkSurface, CircleShape)
                )
            }
        }
    }

    // CLEAR CONFIRMATION MODAL
    if (state.isClearConfirmationPending) {
        AlertDialog(
            onDismissRequest = { tabletController.cancelClear() },
            title = {
                Text(
                    text = "Clear Local Preview?",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This removes stroke trails from the phone display. Strokes drawn on the PC host remain preserved in your active PC app.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        performHaptic()
                        tabletController.confirmClear()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF5252))
                ) {
                    Text("Clear", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { tabletController.cancelClear() },
                    colors = ButtonDefaults.textButtonColors(contentColor = TextMuted)
                ) {
                    Text("Cancel")
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
private fun CompactToolButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) CyanAccent else DarkSurfaceVariant)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (selected) DarkSurface else TextSecondary,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) DarkSurface else TextSecondary
        )
    }
}

@Composable
private fun CompactActionButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceVariant)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (enabled) TextSecondary else TextMuted.copy(alpha = 0.3f),
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = title,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = if (enabled) TextSecondary else TextMuted.copy(alpha = 0.3f)
        )
    }
}
