package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object ShortcutFormatter {
    fun insertPlaceholder(current: String, placeholder: String): String {
        return if (current.isBlank()) placeholder else "$current $placeholder"
    }

    fun normalizeDecimalHoursInput(value: String): String? {
        val normalized = value.trim().replace(",", ".")
        val number = normalized.toDoubleOrNull() ?: return null
        if (number <= 0) return null
        return if (number % 1.0 == 0.0) {
            number.toInt().toString()
        } else {
            normalized.trimEnd('0').trimEnd('.')
        }
    }

    fun applyTokens(
        phrase: String,
        nowMillis: Long = System.currentTimeMillis(),
        locale: Locale = Locale("ar")
    ): String {
        if (phrase.isBlank()) return phrase

        val calendar = Calendar.getInstance().apply { timeInMillis = nowMillis }
        var result = phrase
        result = result.replace("%date%", SimpleDateFormat("yyyy-MM-dd", locale).format(calendar.time))
        result = result.replace("%day%", SimpleDateFormat("EEEE", locale).format(calendar.time))

        val timeRegex = Regex("%time(?:\\+(\\d+(?:\\.\\d+)?)h)?%")
        return timeRegex.replace(result) { match ->
            val hoursToAdd = match.groups[1]?.value?.toDoubleOrNull() ?: 0.0
            val timeCalendar = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                add(Calendar.MINUTE, (hoursToAdd * 60).toInt())
            }
            SimpleDateFormat("hh:mm a", locale).format(timeCalendar.time)
        }
    }
}
