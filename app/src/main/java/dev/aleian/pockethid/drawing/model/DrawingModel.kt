package dev.aleian.pockethid.drawing.model

import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Coordinate point on the drawing canvas.
 * Pure Kotlin data class with zero Android View/Compose dependencies.
 */
data class DrawingPoint(
    val x: Float,
    val y: Float
) {
    fun distanceTo(other: DrawingPoint): Float {
        return hypot(x - other.x, y - other.y)
    }
}

/**
 * Supported drawing tools on the Electronic Drawing Board.
 */
enum class DrawingTool {
    PEN,
    ERASER
}

/**
 * Curated professional color palette matching the PocketHID Command Deck design language.
 */
enum class DrawingColor(val argb: Long, val displayName: String) {
    WHITE(0xFFFFFFFF, "White"),
    CYAN(0xFF00E5FF, "Cyan"),
    YELLOW(0xFFFFEA00, "Yellow"),
    RED(0xFFFF3D00, "Red"),
    BLUE(0xFF2979FF, "Blue")
}

/**
 * An individual drawing stroke formed by a sequence of points.
 */
data class DrawingStroke(
    val points: List<DrawingPoint>,
    val color: DrawingColor = DrawingColor.WHITE,
    val width: Float = 4f,
    val tool: DrawingTool = DrawingTool.PEN,
    val opacity: Float = 1.0f
) {
    /**
     * Checks whether any point in the stroke lies within [radius] of [target].
     */
    fun intersects(target: DrawingPoint, radius: Float): Boolean {
        if (points.isEmpty()) return false
        val threshold = radius + (width / 2f)

        // Bounding box pre-check
        var minX = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var minY = Float.MAX_VALUE
        var maxY = Float.MIN_VALUE

        for (p in points) {
            if (p.x < minX) minX = p.x
            if (p.x > maxX) maxX = p.x
            if (p.y < minY) minY = p.y
            if (p.y > maxY) maxY = p.y
        }

        if (target.x < minX - threshold || target.x > maxX + threshold ||
            target.y < minY - threshold || target.y > maxY + threshold
        ) {
            return false
        }

        // Detailed distance check
        for (i in points.indices) {
            if (points[i].distanceTo(target) <= threshold) {
                return true
            }
            if (i > 0) {
                if (distanceToSegment(target, points[i - 1], points[i]) <= threshold) {
                    return true
                }
            }
        }
        return false
    }

    private fun distanceToSegment(p: DrawingPoint, a: DrawingPoint, b: DrawingPoint): Float {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val lenSq = dx * dx + dy * dy
        if (lenSq == 0f) return p.distanceTo(a)

        val t = max(0f, min(1f, ((p.x - a.x) * dx + (p.y - a.y) * dy) / lenSq))
        val projX = a.x + t * dx
        val projY = a.y + t * dy
        return hypot(p.x - projX, p.y - projY)
    }
}

/**
 * Immutable state of the Drawing Board.
 */
data class DrawingState(
    val strokes: List<DrawingStroke> = emptyList(),
    val redoStack: List<DrawingStroke> = emptyList(),
    val activeTool: DrawingTool = DrawingTool.PEN,
    val selectedColor: DrawingColor = DrawingColor.WHITE,
    val strokeWidth: Float = 4f,
    val isClearConfirmationPending: Boolean = false,
    val currentDraftStroke: DrawingStroke? = null
)
