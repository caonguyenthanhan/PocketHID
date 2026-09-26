package dev.aleian.pockethid.mapping

import dev.aleian.pockethid.model.HidConstants
import org.junit.Assert.*
import org.junit.Test

class KeyMapperTest {

    @Test
    fun testLowercaseLetters() {
        val strokeA = KeyMapper.mapCharToStroke('a')
        assertNotNull(strokeA)
        assertEquals(HidConstants.KEY_A, strokeA!!.keyCode)
        assertEquals(0.toByte(), strokeA.modifiers)

        val strokeZ = KeyMapper.mapCharToStroke('z')
        assertNotNull(strokeZ)
        assertEquals(HidConstants.KEY_Z, strokeZ!!.keyCode)
        assertEquals(0.toByte(), strokeZ.modifiers)
    }

    @Test
    fun testUppercaseLettersRequireShift() {
        val strokeA = KeyMapper.mapCharToStroke('A')
        assertNotNull(strokeA)
        assertEquals(HidConstants.KEY_A, strokeA!!.keyCode)
        assertEquals(HidConstants.MOD_LEFT_SHIFT, strokeA.modifiers)
    }

    @Test
    fun testNumberRowKeys() {
        val stroke1 = KeyMapper.mapCharToStroke('1')
        assertNotNull(stroke1)
        assertEquals(HidConstants.KEY_1, stroke1!!.keyCode)
        assertEquals(0.toByte(), stroke1.modifiers)

        val strokeExclamation = KeyMapper.mapCharToStroke('!')
        assertNotNull(strokeExclamation)
        assertEquals(HidConstants.KEY_1, strokeExclamation!!.keyCode)
        assertEquals(HidConstants.MOD_LEFT_SHIFT, strokeExclamation.modifiers)
    }

    @Test
    fun testSpecialSymbols() {
        val strokeMinus = KeyMapper.mapCharToStroke('-')
        assertNotNull(strokeMinus)
        assertEquals(HidConstants.KEY_MINUS, strokeMinus!!.keyCode)

        val strokeUnderscore = KeyMapper.mapCharToStroke('_')
        assertNotNull(strokeUnderscore)
        assertEquals(HidConstants.KEY_MINUS, strokeUnderscore!!.keyCode)
        assertEquals(HidConstants.MOD_LEFT_SHIFT, strokeUnderscore.modifiers)
    }
}
