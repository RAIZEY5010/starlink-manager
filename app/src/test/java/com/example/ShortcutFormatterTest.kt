package com.example

import com.example.util.ShortcutFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ShortcutFormatterTest {
    private val fixedNow = Calendar.getInstance().apply {
        set(2026, Calendar.JULY, 27, 10, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }

    @Test
    fun `supports decimal hour offsets`() {
        val result = ShortcutFormatter.applyTokens("%time+1.5h%", now = fixedNow)
        assertTrue(result.text.contains("11:30") || result.text.contains("11:30 ص"))
    }

    @Test
    fun `inserts clipboard text and cursor position`() {
        val result = ShortcutFormatter.applyTokens(
            phrase = "مرحبا %clipboard% %cursor%جاهز",
            clipboardText = "أحمد",
            now = fixedNow
        )

        assertEquals("مرحبا أحمد جاهز", result.text)
        assertEquals("مرحبا أحمد ".length, result.cursorOffset)
    }

    @Test
    fun `builds decimal time token`() {
        assertEquals("%time+1.3h%", ShortcutFormatter.buildTimeToken("1.3"))
        assertEquals("%time+2h%", ShortcutFormatter.buildTimeToken("2"))
    }
}
