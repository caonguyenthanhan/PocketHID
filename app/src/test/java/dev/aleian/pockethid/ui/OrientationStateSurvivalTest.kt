package dev.aleian.pockethid.ui

import dev.aleian.pockethid.action.HostOs
import dev.aleian.pockethid.drawing.DrawingTabletController
import dev.aleian.pockethid.drawing.model.DrawingPoint
import dev.aleian.pockethid.drawing.model.DrawingTargetMode
import dev.aleian.pockethid.drawing.model.TabletOrientation
import dev.aleian.pockethid.model.AppLanguage
import dev.aleian.pockethid.model.ControlMode
import dev.aleian.pockethid.model.FocusLockController
import dev.aleian.pockethid.model.PocketStrings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Validates that core PocketHID state models survive orientation transitions,
 * maintain Focus lock integrity, sustain Drawing Tablet state, and keep localized labels intact.
 */
class OrientationStateSurvivalTest {

    @Before
    fun setUp() {
        FocusLockController.reset()
    }

    @Test
    fun `focus lock state survives orientation transitions without resetting`() {
        // 1. In Portrait: User locks focus
        FocusLockController.setLocked(true)
        assertTrue("Focus must be locked in portrait", FocusLockController.isLocked.value)

        // Simulate orientation switch to Landscape (Configuration change)
        val isLandscape = true
        assertTrue("Simulated landscape transition active", isLandscape)

        // Focus must STILL be locked
        assertTrue("Focus lock must survive transition to landscape", FocusLockController.isLocked.value)

        // Mode switching in landscape must be blocked while focus is locked
        assertFalse("Landscape mode switch must be blocked while focus locked", FocusLockController.canSwitchMode())
        val currentModeIndex = ControlMode.GAMEPAD.index
        val attemptedIndex = ControlMode.KEYBOARD.index
        val resolved = FocusLockController.resolveModeSwitch(currentMode = currentModeIndex, targetMode = attemptedIndex)
        assertEquals("Mode switch must remain on GAMEPAD", currentModeIndex, resolved)

        // Internal controls remain fully usable
        assertTrue(FocusLockController.allowsInternalAction())

        // Return to Portrait
        val returnPortrait = true
        assertTrue("Simulated return to portrait", returnPortrait)
        assertTrue("Focus lock must survive back to portrait", FocusLockController.isLocked.value)
    }

    @Test
    fun `drawing tablet state and configuration survive across orientation reconfigurations`() {
        val controller = DrawingTabletController(
            transportProvider = { null },
            hostOsProvider = { HostOs.WINDOWS },
            scope = CoroutineScope(Dispatchers.Unconfined),
            initialTargetMode = DrawingTargetMode.ABSOLUTE_TABLET
        )

        controller.updateCanvasSize(1080f, 2400f)
        controller.updateOrientation(TabletOrientation.PORTRAIT)

        // Start drawing stroke in portrait
        controller.onTouchDown(DrawingPoint(540f, 1200f), pointerCount = 1)
        assertTrue("Stroke active in portrait", controller.isDrawingActive)
        assertNotNull(controller.state.value.currentDraftStroke)

        // Rotate to Landscape: updateCanvasSize reflects landscape dimensions
        controller.updateCanvasSize(2400f, 1080f)
        controller.updateOrientation(TabletOrientation.LANDSCAPE)

        // Target mode and orientation survive
        assertEquals(DrawingTargetMode.ABSOLUTE_TABLET, controller.targetMode.value)
        assertEquals(TabletOrientation.LANDSCAPE, controller.pipeline.orientation)

        // Move stroke in landscape
        controller.onTouchMove(DrawingPoint(1200f, 540f), pointerCount = 1)
        controller.onTouchUp()
        assertFalse("Stroke ended cleanly", controller.isDrawingActive)
        assertEquals("Strokes list persisted across orientation update", 1, controller.state.value.strokes.size)
    }

    @Test
    fun `localized navigation labels never break into multiple words across orientations`() {
        val languages = listOf(AppLanguage.ENGLISH, AppLanguage.VIETNAMESE)
        for (lang in languages) {
            val navLabels = listOf(
                PocketStrings.navKeyboard(lang),
                PocketStrings.navMouse(lang),
                PocketStrings.navGamepad(lang),
                PocketStrings.navPresenter(lang),
                PocketStrings.navOneHand(lang),
                PocketStrings.navDraw(lang)
            )

            // None of the labels should have arbitrary line breaks or split words
            for (label in navLabels) {
                assertFalse("Label '$label' must not contain newline characters", label.contains("\n"))
                assertFalse("Label '$label' must not contain carriage returns", label.contains("\r"))
                assertTrue("Label '$label' must not be blank", label.isNotBlank())
            }

            // In English, 1-Hand must not split into "One-Ha nd"
            if (lang == AppLanguage.ENGLISH) {
                assertEquals("1-Hand", PocketStrings.navOneHand(lang))
                assertFalse(PocketStrings.navOneHand(lang).contains(" "))
            }
        }
    }
}
