package com.example.util

import android.content.Context

/**
 * تخزين بسيط لإعدادات التطبيق (بدون قاعدة بيانات) عن طريق SharedPreferences.
 */
object AppPrefs {
    private const val PREFS_NAME = "starlink_prefs"
    private const val KEY_OVERLAY_ENABLED = "overlay_enabled"
    private const val KEY_QUICK_TIMES = "quick_times"
    private const val KEY_SCAN_INTERVAL = "scan_interval_seconds"

    private const val DEFAULT_QUICK_TIMES = "1,1.5,2,3,5"
    private const val DEFAULT_SCAN_INTERVAL = 60

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isOverlayEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_OVERLAY_ENABLED, true)

    fun setOverlayEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_OVERLAY_ENABLED, enabled).apply()
    }

    /** يرجّع قائمة الأوقات السريعة (تدعم الكسور مثل 1.5) بالترتيب اللي حفظها المستخدم. */
    fun getQuickTimes(context: Context): List<Double> {
        val raw = prefs(context).getString(KEY_QUICK_TIMES, DEFAULT_QUICK_TIMES) ?: DEFAULT_QUICK_TIMES
        val parsed = raw.split(",")
            .mapNotNull { it.trim().replace(",", ".").toDoubleOrNull() }
            .filter { it > 0 }
        return parsed.ifEmpty { listOf(1.0, 2.0, 3.0) }
    }

    fun getQuickTimesRaw(context: Context): String =
        prefs(context).getString(KEY_QUICK_TIMES, DEFAULT_QUICK_TIMES) ?: DEFAULT_QUICK_TIMES

    fun setQuickTimesRaw(context: Context, raw: String) {
        prefs(context).edit().putString(KEY_QUICK_TIMES, raw).apply()
    }

    fun getScanIntervalSeconds(context: Context): Int =
        prefs(context).getInt(KEY_SCAN_INTERVAL, DEFAULT_SCAN_INTERVAL)

    fun setScanIntervalSeconds(context: Context, seconds: Int) {
        prefs(context).edit().putInt(KEY_SCAN_INTERVAL, seconds.coerceAtLeast(10)).apply()
    }

    /** يهيئ نص وقت للعرض على الأزرار: 1 -> "1س"، 1.5 -> "1.5س" */
    fun formatHoursLabel(hours: Double): String {
        val asLong = hours.toLong()
        return if (hours == asLong.toDouble()) "+${asLong}س" else "+${hours}س"
    }
}
