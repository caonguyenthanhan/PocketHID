package dev.aleian.pockethid.mapping

import android.view.KeyEvent
import dev.aleian.pockethid.model.HidConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TextInputResolverTest {

    @Test
    fun testResolveHelloWorld() {
        val strokes = TextInputResolver.resolveText("hello world")
        assertEquals(11, strokes.size)

        // 'h'
        assertEquals((HidConstants.KEY_A + ('h' - 'a')).toByte(), strokes[0].keyCode)
        assertEquals(0.toByte(), strokes[0].modifiers)

        // ' ' (space)
        assertEquals(HidConstants.KEY_SPACE, strokes[5].keyCode)
        assertEquals(0.toByte(), strokes[5].modifiers)

        // 'd'
        assertEquals((HidConstants.KEY_A + ('d' - 'a')).toByte(), strokes[10].keyCode)
    }

    @Test
    fun testResolveHelloWorldWithUppercaseAndSymbol() {
        val strokes = TextInputResolver.resolveText("Hello World!")
        assertEquals(12, strokes.size)

        // 'H' (Uppercase)
        assertEquals((HidConstants.KEY_A + ('h' - 'a')).toByte(), strokes[0].keyCode)
        assertEquals(HidConstants.MOD_LEFT_SHIFT, strokes[0].modifiers)

        // 'e' (Lowercase)
        assertEquals((HidConstants.KEY_A + ('e' - 'a')).toByte(), strokes[1].keyCode)
        assertEquals(0.toByte(), strokes[1].modifiers)

        // 'W' (Uppercase)
        assertEquals((HidConstants.KEY_A + ('w' - 'a')).toByte(), strokes[6].keyCode)
        assertEquals(HidConstants.MOD_LEFT_SHIFT, strokes[6].modifiers)

        // '!' (Shifted '1')
        assertEquals(HidConstants.KEY_1, strokes[11].keyCode)
        assertEquals(HidConstants.MOD_LEFT_SHIFT, strokes[11].modifiers)
    }

    @Test
    fun testResolveDigits() {
        val strokes = TextInputResolver.resolveText("1234567890")
        assertEquals(10, strokes.size)

        assertEquals(HidConstants.KEY_1, strokes[0].keyCode)
        assertEquals(0.toByte(), strokes[0].modifiers)

        assertEquals(HidConstants.KEY_5, strokes[4].keyCode)
        assertEquals(HidConstants.KEY_0, strokes[9].keyCode)
    }

    @Test
    fun testResolveTestAt123() {
        val strokes = TextInputResolver.resolveText("Test@123")
        assertEquals(8, strokes.size)

        // 'T'
        assertEquals((HidConstants.KEY_A + ('t' - 'a')).toByte(), strokes[0].keyCode)
        assertEquals(HidConstants.MOD_LEFT_SHIFT, strokes[0].modifiers)

        // '@' (Shifted '2')
        assertEquals(HidConstants.KEY_2, strokes[4].keyCode)
        assertEquals(HidConstants.MOD_LEFT_SHIFT, strokes[4].modifiers)

        // '1', '2', '3'
        assertEquals(HidConstants.KEY_1, strokes[5].keyCode)
        assertEquals(HidConstants.KEY_2, strokes[6].keyCode)
        assertEquals(HidConstants.KEY_3, strokes[7].keyCode)
    }

    @Test
    fun testResolveSpecialPunctuation() {
        val symbols = "-_=+[]{}|;:'\",<.>/?`~"
        val strokes = TextInputResolver.resolveText(symbols)
        assertEquals(symbols.length, strokes.size)

        // '-' vs '_'
        assertEquals(HidConstants.KEY_MINUS, strokes[0].keyCode)
        assertEquals(0.toByte(), strokes[0].modifiers)

        assertEquals(HidConstants.KEY_MINUS, strokes[1].keyCode)
        assertEquals(HidConstants.MOD_LEFT_SHIFT, strokes[1].modifiers)

        // '=' vs '+'
        assertEquals(HidConstants.KEY_EQUAL, strokes[2].keyCode)
        assertEquals(0.toByte(), strokes[2].modifiers)

        assertEquals(HidConstants.KEY_EQUAL, strokes[3].keyCode)
        assertEquals(HidConstants.MOD_LEFT_SHIFT, strokes[3].modifiers)
    }

    @Test
    fun testResolveSpecialKeyEvents() {
        val delStroke = TextInputResolver.resolveKeyCode(KeyEvent.KEYCODE_DEL)
        assertNotNull(delStroke)
        assertEquals(HidConstants.KEY_BACKSPACE, delStroke!!.keyCode)

        val enterStroke = TextInputResolver.resolveKeyCode(KeyEvent.KEYCODE_ENTER)
        assertNotNull(enterStroke)
        assertEquals(HidConstants.KEY_ENTER, enterStroke!!.keyCode)

        val tabStroke = TextInputResolver.resolveKeyCode(KeyEvent.KEYCODE_TAB)
        assertNotNull(tabStroke)
        assertEquals(HidConstants.KEY_TAB, tabStroke!!.keyCode)
    }
}
