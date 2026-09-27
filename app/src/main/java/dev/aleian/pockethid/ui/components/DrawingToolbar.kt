package dev.aleian.pockethid.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aleian.pockethid.drawing.DrawingController
import dev.aleian.pockethid.drawing.model.DrawingColor
import dev.aleian.pockethid.drawing.model.DrawingState
import dev.aleian.pockethid.drawing.model.DrawingTool
import dev.aleian.pockethid.ui.theme.DarkBorder
import dev.aleian.pockethid.ui.theme.DarkSurface
import dev.aleian.pockethid.ui.theme.DarkSurfaceVariant
import dev.aleian.pockethid.ui.theme.PrimaryBlue
import dev.aleian.pockethid.ui.theme.TextMuted
import dev.aleian.pockethid.ui.theme.TextPrimary
import dev.aleian.pockethid.ui.theme.TextSecondary

private val CyanAccent = Color(0xFF00E5FF)
private val DarkCardBg = Color(0xFF101726)

/**
 * Compact Drawing Toolbar for PocketHID Electronic Drawing Board.
 * Features:
 * - Tool selection: [ PEN ] [ ERASER ]
 * - History control: [ UNDO ] [ REDO ]
 * - Clear action with confirmation modal: [ CLEAR ]
 * - Stroke size selector: 2, 4, 8, 12 dp
 * - 5-Color professional palette: White, Cyan, Yellow, Red, Blue
 */
@Composable
fun DrawingToolbar(
    state: DrawingState,
    controller: DrawingController,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current

    fun performHaptic() {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .border(1.dp, DarkBorder)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // ROW 1: Tools & History Operations
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Tool Switcher [ PEN ] [ ERASER ]
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, DarkBorder, RoundedCornerShape(6.dp))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                ToolButton(
                    label = "PEN",
                    isSelected = state.activeTool == DrawingTool.PEN,
                    onClick = {
                        performHaptic()
                        controller.setTool(DrawingTool.PEN)
                    }
                )
                ToolButton(
                    label = "ERASER",
                    isSelected = state.activeTool == DrawingTool.ERASER,
                    onClick = {
                        performHaptic()
                        controller.setTool(DrawingTool.ERASER)
                    }
                )
            }

            // Right: Operations [ UNDO ] [ REDO ] [ CLEAR ]
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Undo
                IconButton(
                    onClick = {
                        performHaptic()
                        controller.undo()
                    },
                    enabled = state.strokes.isNotEmpty(),
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = if (state.strokes.isNotEmpty()) TextPrimary else TextMuted.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Redo
                IconButton(
                    onClick = {
                        performHaptic()
                        controller.redo()
                    },
                    enabled = state.redoStack.isNotEmpty(),
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo",
                        tint = if (state.redoStack.isNotEmpty()) TextPrimary else TextMuted.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Clear button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (state.strokes.isNotEmpty()) DarkCardBg else DarkSurfaceVariant)
                        .border(1.dp, if (state.strokes.isNotEmpty()) Color(0xFFFF3D00).copy(alpha = 0.5f) else DarkBorder, RoundedCornerShape(6.dp))
                        .clickable(enabled = state.strokes.isNotEmpty()) {
                            performHaptic()
                            controller.requestClear()
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Clear",
                            tint = if (state.strokes.isNotEmpty()) Color(0xFFFF3D00) else TextMuted.copy(alpha = 0.4f),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "CLEAR",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = if (state.strokes.isNotEmpty()) Color(0xFFFF3D00) else TextMuted.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }

        // ROW 2: Stroke Sizes & Color Palette (Horizontal Scrollable)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Stroke Size Selector: 2, 4, 8, 12 dp
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "SIZE",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted,
                    modifier = Modifier.padding(end = 2.dp)
                )
                val sizes = listOf(2f, 4f, 8f, 12f)
                sizes.forEach { sizeDp ->
                    val isSelected = state.strokeWidth == sizeDp
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSelected) DarkCardBg else DarkSurfaceVariant)
                            .border(1.dp, if (isSelected) CyanAccent else DarkBorder, RoundedCornerShape(4.dp))
                            .clickable {
                                performHaptic()
                                controller.setStrokeWidth(sizeDp)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(sizeDp.dp.coerceAtMost(16.dp))
                                .clip(CircleShape)
                                .background(if (isSelected) CyanAccent else TextMuted)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Color Palette Selector
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "COLOR",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted,
                    modifier = Modifier.padding(end = 2.dp)
                )
                DrawingColor.values().forEach { drawingColor ->
                    val isSelected = state.selectedColor == drawingColor
                    val colorValue = Color(drawingColor.argb)

                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(colorValue)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Color.White else DarkBorder,
                                shape = CircleShape
                            )
                            .clickable {
                                performHaptic()
                                controller.setColor(drawingColor)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (drawingColor == DrawingColor.WHITE) Color.Black else Color.White)
                            )
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialog for CLEAR action
    if (state.isClearConfirmationPending) {
        AlertDialog(
            onDismissRequest = { controller.cancelClear() },
            containerColor = DarkCardBg,
            shape = RoundedCornerShape(12.dp),
            title = {
                Text(
                    text = "Clear Drawing Canvas?",
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "All drawing strokes will be permanently erased. Are you sure you want to clear?",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        performHaptic()
                        controller.confirmClear()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF3D00))
                ) {
                    Text(
                        text = "CLEAR ALL",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { controller.cancelClear() },
                    colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                ) {
                    Text(
                        text = "CANCEL",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        )
    }
}

@Composable
private fun ToolButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) DarkCardBg else Color.Transparent)
            .border(1.dp, if (isSelected) CyanAccent else Color.Transparent, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) CyanAccent else TextMuted
        )
    }
}
