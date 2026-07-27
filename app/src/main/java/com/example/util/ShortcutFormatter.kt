package com.example.util

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

data class FormattedShortcut(
    val text: String,
    val cursorOffset: Int? = null
)

object ShortcutFormatter {
    private val arabicLocale = Locale("ar")
    private val timeRegex = Regex("%time(?:\\+([0-9]+(?:\\.[0-9]+)?)h)?%")

    fun applyTokens(
        phrase: String,
        clipboardText: String = "",
        now: Calendar = Calendar.getInstance()
    ): FormattedShortcut {
        val cursorToken = "%cursor%"
        return if (phrase.contains(cursorToken)) {
            val parts = phrase.split(cursorToken, limit = 2)
            val beforeCursor = replaceDynamicTokens(parts[0], clipboardText, now)
            val afterCursor = replaceDynamicTokens(parts.getOrElse(1) { "" }, clipboardText, now)
            FormattedShortcut(
                text = beforeCursor + afterCursor,
                cursorOffset = beforeCursor.length
            )
        } else {
            FormattedShortcut(text = replaceDynamicTokens(phrase, clipboardText, now))
        }
    }

    fun buildTimeToken(hoursText: String): String {
        val normalized = hoursText.trim().replace(',', '.')
        if (normalized.isEmpty()) return "%time%"
        val amount = normalized.toDoubleOrNull() ?: return "%time%"
        if (amount == 0.0) return "%time%"

        val rendered = if (amount % 1.0 == 0.0) {
            amount.roundToInt().toString()
        } else {
            DecimalFormat("0.##").format(amount)
        }
        return "%time+${rendered}h%"
    }

    private fun formatTimeWithOffset(now: Calendar, hoursToAdd: Double): String {
        val timeCal = now.clone() as Calendar
        val minutesToAdd = (hoursToAdd * 60.0).roundToInt()
        timeCal.add(Calendar.MINUTE, minutesToAdd)
        val timeFormat = SimpleDateFormat("hh:mm a", arabicLocale)
        return timeFormat.format(timeCal.time)
    }

    private fun replaceDynamicTokens(
        input: String,
        clipboardText: String,
        now: Calendar
    ): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", arabicLocale)
        val dayFormat = SimpleDateFormat("EEEE", arabicLocale)
        var working = input
        working = working.replace("%date%", dateFormat.format(now.time))
        working = working.replace("%day%", dayFormat.format(now.time))
        working = working.replace("%clipboard%", clipboardText)
        return timeRegex.replace(working) { match ->
            val hoursValue = match.groups[1]?.value?.toDoubleOrNull() ?: 0.0
            formatTimeWithOffset(now, hoursValue)
        }
    }
}
