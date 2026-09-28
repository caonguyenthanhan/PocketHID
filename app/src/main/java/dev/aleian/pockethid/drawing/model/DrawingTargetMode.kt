package dev.aleian.pockethid.drawing.model

/**
 * Operating mode of the Graphics Tablet surface.
 */
enum class DrawingTargetMode {
    /**
     * Mode 2: True Absolute HID Tablet / Digitizer (Report ID 5).
     * Touch position maps directly to normalized PC tablet coordinates.
     */
    ABSOLUTE_TABLET,

    /**
     * Mode 1: Relative Mouse Drawing (Report ID 2 fallback).
     * Precision trackpad-like continuous drawing with defined reference point.
     */
    RELATIVE_MOUSE
}
