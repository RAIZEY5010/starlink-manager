package com.example

import com.example.util.ShortcutFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale
import java.util.TimeZone

class ShortcutFormatterTest {

    @Test
    fun `normalize decimal hours keeps useful formats`() {
        assertEquals("1.5", ShortcutFormatter.normalizeDecimalHoursInput("1.5"))
        assertEquals("1.3", ShortcutFormatter.normalizeDecimalHoursInput("1,3"))
        assertEquals("2", ShortcutFormatter.normalizeDecimalHoursInput("2.0"))
        assertNull(ShortcutFormatter.normalizeDecimalHoursInput("0"))
    }

    @Test
    fun `apply tokens expands decimal time tags`() {
        val previousTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        try {
            val result = ShortcutFormatter.applyTokens(
                phrase = "موعدي %date% %time+1.5h%",
                nowMillis = 0L,
                locale = Locale.US
            )

            assertEquals("موعدي 1970-01-01 01:30 AM", result)
        } finally {
            TimeZone.setDefault(previousTimeZone)
        }
    }
}
