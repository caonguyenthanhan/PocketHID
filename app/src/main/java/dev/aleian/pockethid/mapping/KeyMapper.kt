package dev.aleian.pockethid.mapping

import android.view.KeyEvent
import dev.aleian.pockethid.model.HidConstants

data class HidKeyStroke(
    val keyCode: Byte,
    val modifiers: Byte = 0
)

object KeyMapper {

    fun mapCharToStroke(char: Char): HidKeyStroke? {
        return when (char) {
            in 'a'..'z' -> {
                val offset = char - 'a'
                HidKeyStroke((HidConstants.KEY_A + offset).toByte(), 0)
            }
            in 'A'..'Z' -> {
                val offset = char - 'A'
                HidKeyStroke((HidConstants.KEY_A + offset).toByte(), HidConstants.MOD_LEFT_SHIFT)
            }
            '1' -> HidKeyStroke(HidConstants.KEY_1, 0)
            '!' -> HidKeyStroke(HidConstants.KEY_1, HidConstants.MOD_LEFT_SHIFT)
            '2' -> HidKeyStroke(HidConstants.KEY_2, 0)
            '@' -> HidKeyStroke(HidConstants.KEY_2, HidConstants.MOD_LEFT_SHIFT)
            '3' -> HidKeyStroke(HidConstants.KEY_3, 0)
            '#' -> HidKeyStroke(HidConstants.KEY_3, HidConstants.MOD_LEFT_SHIFT)
            '4' -> HidKeyStroke(HidConstants.KEY_4, 0)
            '$' -> HidKeyStroke(HidConstants.KEY_4, HidConstants.MOD_LEFT_SHIFT)
            '5' -> HidKeyStroke(HidConstants.KEY_5, 0)
            '%' -> HidKeyStroke(HidConstants.KEY_5, HidConstants.MOD_LEFT_SHIFT)
            '6' -> HidKeyStroke(HidConstants.KEY_6, 0)
            '^' -> HidKeyStroke(HidConstants.KEY_6, HidConstants.MOD_LEFT_SHIFT)
            '7' -> HidKeyStroke(HidConstants.KEY_7, 0)
            '&' -> HidKeyStroke(HidConstants.KEY_7, HidConstants.MOD_LEFT_SHIFT)
            '8' -> HidKeyStroke(HidConstants.KEY_8, 0)
            '*' -> HidKeyStroke(HidConstants.KEY_8, HidConstants.MOD_LEFT_SHIFT)
            '9' -> HidKeyStroke(HidConstants.KEY_9, 0)
            '(' -> HidKeyStroke(HidConstants.KEY_9, HidConstants.MOD_LEFT_SHIFT)
            '0' -> HidKeyStroke(HidConstants.KEY_0, 0)
            ')' -> HidKeyStroke(HidConstants.KEY_0, HidConstants.MOD_LEFT_SHIFT)

            ' ' -> HidKeyStroke(HidConstants.KEY_SPACE, 0)
            '\n', '\r' -> HidKeyStroke(HidConstants.KEY_ENTER, 0)
            '\t' -> HidKeyStroke(HidConstants.KEY_TAB, 0)
            '\b' -> HidKeyStroke(HidConstants.KEY_BACKSPACE, 0)

            '-' -> HidKeyStroke(HidConstants.KEY_MINUS, 0)
            '_' -> HidKeyStroke(HidConstants.KEY_MINUS, HidConstants.MOD_LEFT_SHIFT)
            '=' -> HidKeyStroke(HidConstants.KEY_EQUAL, 0)
            '+' -> HidKeyStroke(HidConstants.KEY_EQUAL, HidConstants.MOD_LEFT_SHIFT)
            '[' -> HidKeyStroke(HidConstants.KEY_LEFTBRACE, 0)
            '{' -> HidKeyStroke(HidConstants.KEY_LEFTBRACE, HidConstants.MOD_LEFT_SHIFT)
            ']' -> HidKeyStroke(HidConstants.KEY_RIGHTBRACE, 0)
            '}' -> HidKeyStroke(HidConstants.KEY_RIGHTBRACE, HidConstants.MOD_LEFT_SHIFT)
            '\\' -> HidKeyStroke(HidConstants.KEY_BACKSLASH, 0)
            '|' -> HidKeyStroke(HidConstants.KEY_BACKSLASH, HidConstants.MOD_LEFT_SHIFT)
            ';' -> HidKeyStroke(HidConstants.KEY_SEMICOLON, 0)
            ':' -> HidKeyStroke(HidConstants.KEY_SEMICOLON, HidConstants.MOD_LEFT_SHIFT)
            '\'' -> HidKeyStroke(HidConstants.KEY_APOSTROPHE, 0)
            '"' -> HidKeyStroke(HidConstants.KEY_APOSTROPHE, HidConstants.MOD_LEFT_SHIFT)
            '`' -> HidKeyStroke(HidConstants.KEY_GRAVE, 0)
            '~' -> HidKeyStroke(HidConstants.KEY_GRAVE, HidConstants.MOD_LEFT_SHIFT)
            ',' -> HidKeyStroke(HidConstants.KEY_COMMA, 0)
            '<' -> HidKeyStroke(HidConstants.KEY_COMMA, HidConstants.MOD_LEFT_SHIFT)
            '.' -> HidKeyStroke(HidConstants.KEY_DOT, 0)
            '>' -> HidKeyStroke(HidConstants.KEY_DOT, HidConstants.MOD_LEFT_SHIFT)
            '/' -> HidKeyStroke(HidConstants.KEY_SLASH, 0)
            '?' -> HidKeyStroke(HidConstants.KEY_SLASH, HidConstants.MOD_LEFT_SHIFT)

            else -> null
        }
    }

    fun mapAndroidKeyEvent(androidKeyCode: Int): HidKeyStroke? {
        return when (androidKeyCode) {
            KeyEvent.KEYCODE_DEL -> HidKeyStroke(HidConstants.KEY_BACKSPACE, 0)
            KeyEvent.KEYCODE_FORWARD_DEL -> HidKeyStroke(HidConstants.KEY_DELETE, 0)
            KeyEvent.KEYCODE_ENTER -> HidKeyStroke(HidConstants.KEY_ENTER, 0)
            KeyEvent.KEYCODE_TAB -> HidKeyStroke(HidConstants.KEY_TAB, 0)
            KeyEvent.KEYCODE_ESCAPE -> HidKeyStroke(HidConstants.KEY_ESC, 0)
            KeyEvent.KEYCODE_SPACE -> HidKeyStroke(HidConstants.KEY_SPACE, 0)
            KeyEvent.KEYCODE_DPAD_UP -> HidKeyStroke(HidConstants.KEY_UP, 0)
            KeyEvent.KEYCODE_DPAD_DOWN -> HidKeyStroke(HidConstants.KEY_DOWN, 0)
            KeyEvent.KEYCODE_DPAD_LEFT -> HidKeyStroke(HidConstants.KEY_LEFT, 0)
            KeyEvent.KEYCODE_DPAD_RIGHT -> HidKeyStroke(HidConstants.KEY_RIGHT, 0)
            KeyEvent.KEYCODE_MOVE_HOME -> HidKeyStroke(HidConstants.KEY_HOME, 0)
            KeyEvent.KEYCODE_MOVE_END -> HidKeyStroke(HidConstants.KEY_END, 0)
            KeyEvent.KEYCODE_PAGE_UP -> HidKeyStroke(HidConstants.KEY_PAGEUP, 0)
            KeyEvent.KEYCODE_PAGE_DOWN -> HidKeyStroke(HidConstants.KEY_PAGEDOWN, 0)
            KeyEvent.KEYCODE_INSERT -> HidKeyStroke(HidConstants.KEY_INSERT, 0)
            else -> null
        }
    }
}
