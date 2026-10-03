package dev.aleian.pockethid.mapping

import android.view.KeyEvent
import dev.aleian.pockethid.model.HidConstants

/**
 * Resolves committed text and IME key events into concrete HID keystrokes.
 */
object TextInputResolver {

    /**
     * Converts a string of text into a sequence of [HidKeyStroke]s.
     * Preserves shift states and punctuation.
     */
    fun resolveText(text: String, applyTelex: Boolean = true): List<HidKeyStroke> {
        val list = mutableListOf<HidKeyStroke>()
        val processedText = if (applyTelex) TelexConverter.convert(text) else text
        for (char in processedText) {
            val stroke = KeyMapper.mapCharToStroke(char)
            if (stroke != null) {
                list.add(stroke)
            }
        }
        return list
    }

    /**
     * Resolves an Android keyCode and optional unicode character to [HidKeyStroke].
     * Can be tested without stubbed KeyEvent instantiation.
     */
    fun resolveKeyCode(keyCode: Int, unicodeChar: Int = 0): HidKeyStroke? {
        val mappedSpecial = KeyMapper.mapAndroidKeyEvent(keyCode)
        if (mappedSpecial != null) {
            return mappedSpecial
        }
        if (unicodeChar != 0) {
            val char = unicodeChar.toChar()
            return KeyMapper.mapCharToStroke(char)
        }
        return null
    }

    /**
     * Resolves an Android [KeyEvent] into a single [HidKeyStroke].
     */
    fun resolveKeyEvent(event: KeyEvent): HidKeyStroke? {
        return resolveKeyCode(event.keyCode, event.unicodeChar)
    }
}
