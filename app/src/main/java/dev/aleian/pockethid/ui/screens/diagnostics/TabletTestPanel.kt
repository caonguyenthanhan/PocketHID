package dev.aleian.pockethid.ui.screens.diagnostics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aleian.pockethid.drawing.TabletDiagnosticsHub
import dev.aleian.pockethid.model.HidConstants
import dev.aleian.pockethid.ui.theme.DarkBorder
import dev.aleian.pockethid.ui.theme.DarkSurfaceVariant
import dev.aleian.pockethid.ui.theme.PrimaryBlue
import dev.aleian.pockethid.ui.theme.StatusConnected
import dev.aleian.pockethid.ui.theme.StatusDisconnected
import dev.aleian.pockethid.ui.theme.TextMuted
import dev.aleian.pockethid.ui.theme.TextPrimary

private val CyanAccent = Color(0xFF00E5FF)

/**
 * Developer Diagnostics Panel for Graphics Tablet & Coordinate Mapping.
 *
 * Implements Section 18 of the specification:
 * - RAW (x, y)
 * - CANVAS (x, y)
 * - NORMALIZED (x, y)
 * - HID (x, y)
 * - ORIENTATION (LANDSCAPE / PORTRAIT)
 * - CANVAS SIZE (W x H)
 * - HID RANGE (X 0..32767, Y 0..32767)
 */
@Composable
fun TabletTestPanel(
    modifier: Modifier = Modifier
) {
    val diag by TabletDiagnosticsHub.telemetry.collectAsState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "GRAPHICS TABLET TELEMETRY",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = CyanAccent,
                letterSpacing = 1.sp
            )
            Text(
                text = "REPORTS: ${diag.reportsSent}",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = TextMuted
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Target Mode & Orientation
        DiagnosticRow(
            label = "TARGET MODE",
            value = diag.targetMode,
            valueColor = if (diag.targetMode == "ABSOLUTE_TABLET") CyanAccent else Color(0xFFFFB74D)
        )

        DiagnosticRow(
            label = "ORIENTATION",
            value = diag.orientation,
            valueColor = TextPrimary
        )

        DiagnosticRow(
            label = "CANVAS SIZE",
            value = if (diag.canvasWidth > 0f) "${diag.canvasWidth.toInt()} × ${diag.canvasHeight.toInt()} px" else "AWAITING MEASURE",
            valueColor = TextPrimary
        )

        DiagnosticRow(
            label = "SAFE AREA INSETS",
            value = diag.insetsDesc,
            valueColor = TextMuted
        )

        DiagnosticRow(
            label = "ASPECT POLICY",
            value = diag.aspectPolicy,
            valueColor = TextPrimary
        )

        DiagnosticRow(
            label = "HID RANGE",
            value = "X: ${diag.hidRangeX}, Y: ${diag.hidRangeY}",
            valueColor = PrimaryBlue
        )

        // Contact State
        DiagnosticRow(
            label = "TIP SWITCH (TOUCH)",
            value = if (diag.isTipDown) "DOWN (CONTACT)" else "UP (RELEASED)",
            valueColor = if (diag.isTipDown) StatusConnected else StatusDisconnected
        )

        DiagnosticRow(
            label = "IN RANGE (STYLUS)",
            value = if (diag.isInRange) "IN SENSING RANGE" else "OUT OF RANGE",
            valueColor = if (diag.isInRange) StatusConnected else TextMuted
        )

        DiagnosticRow(
            label = "ACTIVE TOOL",
            value = diag.activeTool,
            valueColor = CyanAccent
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "COORDINATE PIPELINE (ACTIVE STROKE)",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = TextMuted
        )

        DiagnosticRow(
            label = "RAW TOUCH",
            value = "(${String.format("%.1f", diag.rawX)}, ${String.format("%.1f", diag.rawY)})",
            valueColor = TextPrimary
        )

        DiagnosticRow(
            label = "CANVAS LOCAL",
            value = "(${String.format("%.1f", diag.canvasX)}, ${String.format("%.1f", diag.canvasY)})",
            valueColor = TextPrimary
        )

        DiagnosticRow(
            label = "NORMALIZED [0..1]",
            value = "(${String.format("%.4f", diag.normalizedX)}, ${String.format("%.4f", diag.normalizedY)})",
            valueColor = CyanAccent
        )

        DiagnosticRow(
            label = "HID REPORT (ID 5)",
            value = "X=${diag.hidX}, Y=${diag.hidY}",
            valueColor = StatusConnected
        )

        DiagnosticRow(
            label = "LAST REPORT HEX",
            value = diag.lastReportHex,
            valueColor = Color(0xFFFFD54F)
        )
    }
}

@Composable
private fun DiagnosticRow(
    label: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = TextMuted
        )
        Text(
            text = value,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}
