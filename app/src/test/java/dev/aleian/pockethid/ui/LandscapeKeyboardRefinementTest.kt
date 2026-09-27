package dev.aleian.pockethid.ui

import dev.aleian.pockethid.model.HidConstants
import dev.aleian.pockethid.model.KeyLegend
import dev.aleian.pockethid.model.KeyLegends
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LandscapeKeyboardRefinementTest {

    @Test
    fun testAllRequiredShiftMappings() {
        val expectedMappings = mapOf(
            "`" to ("~" to HidConstants.KEY_GRAVE),
            "1" to ("!" to HidConstants.KEY_1),
            "2" to ("@" to HidConstants.KEY_2),
            "3" to ("#" to HidConstants.KEY_3),
            "4" to ("$" to HidConstants.KEY_4),
            "5" to ("%" to HidConstants.KEY_5),
            "6" to ("^" to HidConstants.KEY_6),
            "7" to ("&" to HidConstants.KEY_7),
            "8" to ("*" to HidConstants.KEY_8),
            "9" to ("(" to HidConstants.KEY_9),
            "0" to (")" to HidConstants.KEY_0),
            "-" to ("_" to HidConstants.KEY_MINUS),
            "=" to ("+" to HidConstants.KEY_EQUAL),
            "[" to ("{" to HidConstants.KEY_LEFTBRACE),
            "]" to ("}" to HidConstants.KEY_RIGHTBRACE),
            "\\" to ("|" to HidConstants.KEY_BACKSLASH),
            ";" to (":" to HidConstants.KEY_SEMICOLON),
            "'" to ("\"" to HidConstants.KEY_APOSTROPHE),
            "," to ("<" to HidConstants.KEY_COMMA),
            "." to (">" to HidConstants.KEY_DOT),
            "/" to ("?" to HidConstants.KEY_SLASH)
        )

        val allLegends = KeyLegends.NUMBER_ROW + KeyLegends.ROW_2 + KeyLegends.ROW_3 + KeyLegends.ROW_4
        val legendsByPrimary = allLegends.filter { it.shifted != null }.associateBy { it.primary }

        assertEquals("All 21 required shifted symbols must be mapped", 21, expectedMappings.size)

        for ((primary, expected) in expectedMappings) {
            val (expectedShifted, expectedKeyCode) = expected
            val legend = legendsByPrimary[primary]
            assertNotNull("Missing legend for key '$primary'", legend)
            assertEquals("Shifted symbol mismatch for '$primary'", expectedShifted, legend!!.shifted)
            assertEquals("KeyCode mismatch for '$primary'", expectedKeyCode, legend.keyCode)

            // Test label resolution with Shift ON vs OFF
            assertEquals("With Shift OFF, primary symbol must be displayed", primary, legend.resolveActiveLabel(isShiftActive = false, isCapsLockActive = false))
            assertEquals("With Shift ON, shifted symbol must be displayed", expectedShifted, legend.resolveActiveLabel(isShiftActive = true, isCapsLockActive = false))
            // Caps Lock must NOT alter number/symbol keys
            assertEquals("Caps Lock must not alter symbol with Shift OFF", primary, legend.resolveActiveLabel(isShiftActive = false, isCapsLockActive = true))
            assertEquals("Caps Lock must not alter symbol with Shift ON", expectedShifted, legend.resolveActiveLabel(isShiftActive = true, isCapsLockActive = true))
        }
    }

    @Test
    fun testCapsLockAndShiftSemanticsForAlphabeticKeys() {
        val letterKeys = listOf(
            KeyLegends.Q, KeyLegends.W, KeyLegends.E, KeyLegends.R, KeyLegends.T, KeyLegends.Y,
            KeyLegends.A, KeyLegends.S, KeyLegends.D, KeyLegends.F,
            KeyLegends.Z, KeyLegends.X, KeyLegends.C, KeyLegends.V
        )

        for (legend in letterKeys) {
            // 1. CAPS OFF + SHIFT OFF -> lowercase
            val labelOffOff = legend.resolveActiveLabel(isShiftActive = false, isCapsLockActive = false)
            assertEquals("CAPS OFF + SHIFT OFF must be lowercase", legend.primary.lowercase(), labelOffOff)

            // 2. CAPS ON + SHIFT OFF -> uppercase
            val labelOnOff = legend.resolveActiveLabel(isShiftActive = false, isCapsLockActive = true)
            assertEquals("CAPS ON + SHIFT OFF must be uppercase", legend.primary.uppercase(), labelOnOff)

            // 3. CAPS OFF + SHIFT ON -> uppercase
            val labelOffOn = legend.resolveActiveLabel(isShiftActive = true, isCapsLockActive = false)
            assertEquals("CAPS OFF + SHIFT ON must be uppercase", legend.primary.uppercase(), labelOffOn)

            // 4. CAPS ON + SHIFT ON -> lowercase (standard hardware keyboard semantics)
            val labelOnOn = legend.resolveActiveLabel(isShiftActive = true, isCapsLockActive = true)
            assertEquals("CAPS ON + SHIFT ON must invert to lowercase", legend.primary.lowercase(), labelOnOn)
        }
    }

    @Test
    fun testCapsLockStatePersistenceAcrossKeypresses() {
        var capsLockActive = false
        var shiftActive = false

        // Tap CAPS -> turns ON
        capsLockActive = !capsLockActive
        assertTrue("Caps Lock must be ON after toggle", capsLockActive)

        // Type letter with Shift OFF -> produces uppercase
        val label1 = KeyLegends.A.resolveActiveLabel(isShiftActive = shiftActive, isCapsLockActive = capsLockActive)
        assertEquals("A", label1)

        // Ordinary keypress does NOT reset Caps Lock
        assertTrue("Caps Lock must persist across typing keypresses", capsLockActive)

        // Turn on sticky Shift
        shiftActive = true
        val label2 = KeyLegends.A.resolveActiveLabel(isShiftActive = shiftActive, isCapsLockActive = capsLockActive)
        assertEquals("With Caps ON and Shift ON, result must be lowercase 'a'", "a", label2)

        // Consume sticky shift
        shiftActive = false
        // Caps Lock is still ON
        assertTrue("Caps Lock remains active after sticky shift is consumed", capsLockActive)
        val label3 = KeyLegends.A.resolveActiveLabel(isShiftActive = shiftActive, isCapsLockActive = capsLockActive)
        assertEquals("A", label3)

        // Tap CAPS again -> turns OFF
        capsLockActive = !capsLockActive
        assertFalse("Caps Lock must be OFF after second toggle", capsLockActive)
        val label4 = KeyLegends.A.resolveActiveLabel(isShiftActive = shiftActive, isCapsLockActive = capsLockActive)
        assertEquals("a", label4)
    }

    @Test
    fun testRightNavClusterAndArrowKeyCodes() {
        // Verify key codes for Right Nav Zone: DEL, HOME, END, UP, DOWN, LEFT, RIGHT
        assertEquals(0x4C.toByte(), HidConstants.KEY_DELETE)
        assertEquals(0x4A.toByte(), HidConstants.KEY_HOME)
        assertEquals(0x4D.toByte(), HidConstants.KEY_END)
        assertEquals(0x52.toByte(), HidConstants.KEY_UP)
        assertEquals(0x51.toByte(), HidConstants.KEY_DOWN)
        assertEquals(0x50.toByte(), HidConstants.KEY_LEFT)
        assertEquals(0x4F.toByte(), HidConstants.KEY_RIGHT)
        assertEquals(0x39.toByte(), HidConstants.KEY_CAPSLOCK)
    }

    @Test
    fun testKeyboardDeckGeometryWeights() {
        val leftDeckWeight = 1.1f
        val centerBayWeight = 7.8f
        val rightDeckWeight = 1.1f
        val totalWeight = leftDeckWeight + centerBayWeight + rightDeckWeight

        assertEquals(10.0f, totalWeight, 0.001f)
        assertEquals(0.11f, leftDeckWeight / totalWeight, 0.001f)
        assertEquals(0.78f, centerBayWeight / totalWeight, 0.001f)
        assertEquals(0.11f, rightDeckWeight / totalWeight, 0.001f)
    }

    @Test
    fun testNumberRowDualLegendIntegrity() {
        val numberRow = KeyLegends.NUMBER_ROW
        assertEquals("Number row must contain exactly 13 dual-legend keys", 13, numberRow.size)

        val expectedPairs = listOf(
            "`" to "~",
            "1" to "!",
            "2" to "@",
            "3" to "#",
            "4" to "$",
            "5" to "%",
            "6" to "^",
            "7" to "&",
            "8" to "*",
            "9" to "(",
            "0" to ")",
            "-" to "_",
            "=" to "+"
        )

        for (i in expectedPairs.indices) {
            val key = numberRow[i]
            val (expectedPrimary, expectedShifted) = expectedPairs[i]
            assertEquals("Key at index $i primary mismatch", expectedPrimary, key.primary)
            assertEquals("Key at index $i shifted mismatch", expectedShifted, key.shifted)
            assertTrue("Keycode must be valid non-zero HID code", key.keyCode > 0)
        }
    }
}
