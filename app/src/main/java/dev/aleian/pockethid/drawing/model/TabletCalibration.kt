package dev.aleian.pockethid.drawing.model

/**
 * Linear calibration model for tablet coordinate mapping.
 *
 * Supports:
 *   X' = (X * scaleX + offsetX).coerceIn(0f, 1f)
 *   Y' = (Y * scaleY + offsetY).coerceIn(0f, 1f)
 */
data class TabletCalibration(
    val scaleX: Float = 1.0f,
    val scaleY: Float = 1.0f,
    val offsetX: Float = 0.0f,
    val offsetY: Float = 0.0f
) {
    fun apply(normalizedX: Float, normalizedY: Float): Pair<Float, Float> {
        val x = (normalizedX * scaleX + offsetX).coerceIn(0.0f, 1.0f)
        val y = (normalizedY * scaleY + offsetY).coerceIn(0.0f, 1.0f)
        return Pair(x, y)
    }

    companion object {
        val DEFAULT = TabletCalibration()

        /**
         * Derives scale and offset from 4 corner touch points on the canvas:
         * topLeft, topRight, bottomRight, bottomLeft.
         */
        fun fromCorners(
            topLeft: DrawingPoint,
            topRight: DrawingPoint,
            bottomRight: DrawingPoint,
            bottomLeft: DrawingPoint,
            canvasWidth: Float,
            canvasHeight: Float
        ): TabletCalibration {
            if (canvasWidth <= 0f || canvasHeight <= 0f) return DEFAULT

            val normTL_X = topLeft.x / canvasWidth
            val normTL_Y = topLeft.y / canvasHeight
            val normTR_X = topRight.x / canvasWidth
            val normBR_X = bottomRight.x / canvasWidth
            val normBR_Y = bottomRight.y / canvasHeight
            val normBL_Y = bottomLeft.y / canvasHeight

            val measuredWidth = maxOf(0.01f, ((normTR_X + normBR_X) / 2f) - ((normTL_X + (bottomLeft.x / canvasWidth)) / 2f))
            val measuredHeight = maxOf(0.01f, ((normBR_Y + normBL_Y) / 2f) - ((normTL_Y + (topRight.y / canvasHeight)) / 2f))

            val scaleX = 1.0f / measuredWidth
            val scaleY = 1.0f / measuredHeight
            val offsetX = -normTL_X * scaleX
            val offsetY = -normTL_Y * scaleY

            return TabletCalibration(scaleX, scaleY, offsetX, offsetY)
        }
    }
}
