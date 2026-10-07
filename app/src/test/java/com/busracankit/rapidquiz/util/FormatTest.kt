package com.busracankit.rapidquiz.util

import com.busracankit.rapidquiz.ui.components.secondsLeft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatTest {
    @Test
    fun `süre virgülle ve tek ondalıkla yazılır`() {
        assertEquals("41,2", formatSeconds(41230))
        assertEquals("0,0", formatSeconds(0))
        assertEquals("100,0", formatSeconds(100_000))
    }

    @Test
    fun `isim sadeleştirilir`() {
        assertEquals("Büşra Cankit", PlayerName.normalize("  Büşra    Cankit  "))
        assertEquals("a b", PlayerName.normalize("a\t\nb"))
    }

    @Test
    fun `isim uzunluğu 2-20`() {
        assertFalse(PlayerName.isLengthValid("A"))
        assertTrue(PlayerName.isLengthValid("Ay"))
        assertTrue(PlayerName.isLengthValid("Ç".repeat(20)))
        assertFalse(PlayerName.isLengthValid("a".repeat(21)))
    }

    @Test
    fun `sayaç saniyesi yukarı yuvarlanır`() {
        assertEquals(5, secondsLeft(5000))
        assertEquals(5, secondsLeft(4001))
        assertEquals(1, secondsLeft(1))
        assertEquals(0, secondsLeft(0))
        assertEquals(0, secondsLeft(-20))
    }
}
