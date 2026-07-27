package com.example.util

import android.content.Context

object TextExpanderPreferences {
    private const val prefsName = "text_expander_prefs"
    private const val keyBlacklistedPackages = "blacklisted_packages"

    fun getBlacklistedPackages(context: Context): List<String> {
        return context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .getString(keyBlacklistedPackages, "")
            .orEmpty()
            .split('\n', ',', ';')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
    }

    fun getBlacklistedPackagesText(context: Context): String {
        return getBlacklistedPackages(context).joinToString(separator = "\n")
    }

    fun setBlacklistedPackages(context: Context, rawValue: String) {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .putString(keyBlacklistedPackages, rawValue)
            .apply()
    }

    fun isPackageBlocked(context: Context, packageName: CharSequence?): Boolean {
        val normalized = packageName?.toString()?.trim().orEmpty()
        if (normalized.isEmpty()) return false
        return getBlacklistedPackages(context).any { blocked ->
            normalized == blocked || normalized.startsWith("$blocked.")
        }
    }
}
