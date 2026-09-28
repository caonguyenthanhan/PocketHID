package dev.aleian.pockethid.drawing.model

/**
 * Aspect ratio correction policies between smartphone drawing canvas and host PC screen.
 */
enum class AspectRatioPolicy {
    /**
     * Preserves 1:1 circular/geometric proportions with host display.
     * Prevents non-uniform stretching or squashing by applying symmetric letterboxing.
     */
    FIT,

    /**
     * Stretches phone canvas to map completely across the PC tablet logical range.
     */
    STRETCH
}
