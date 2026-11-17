package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max

object DateUtils {
    private val isoFormats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd"
    )

    fun nowIsoString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    fun todayYmd(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    fun parseIsoMillis(iso: String?): Long? {
        if (iso.isNullOrBlank()) return null
        for (pattern in isoFormats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                if (pattern.endsWith("'Z'")) {
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                }
                val parsed = sdf.parse(iso.trim())
                if (parsed != null) return parsed.time
            } catch (_: Exception) {
            }
        }
        return null
    }

    fun formatShortDate(iso: String?): String {
        val millis = parseIsoMillis(iso) ?: return iso.orEmpty().ifBlank { "Recently" }
        val sdf = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    fun formatLongDate(iso: String?): String {
        val millis = parseIsoMillis(iso) ?: return iso.orEmpty()
        val sdf = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    fun formatRelativeOrDate(iso: String?): String {
        val millis = parseIsoMillis(iso) ?: return iso.orEmpty().ifBlank { "Just now" }
        val diffMs = max(0L, System.currentTimeMillis() - millis)
        val mins = diffMs / 60_000L
        val hours = mins / 60L
        val days = hours / 24L
        return when {
            mins < 1 -> "Just now"
            mins < 60 -> "${mins}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7 -> "${days}d ago"
            else -> formatShortDate(iso)
        }
    }

    fun computeDurationDisplay(startStr: String, endStr: String): String {
        val sMin = parseMinutesOfDay(startStr) ?: return ""
        val eMin = parseMinutesOfDay(endStr) ?: return ""
        var diff = eMin - sMin
        if (diff < 0) diff += 24 * 60
        val hours = diff / 60
        val mins = diff % 60
        return when {
            hours > 0 && mins > 0 -> "${hours}h ${mins}m"
            hours > 0 -> "${hours}h 00m"
            else -> "${mins}m"
        }
    }

    private fun parseMinutesOfDay(t: String): Int? {
        val clean = t.trim().uppercase(Locale.US)
        if (clean.isBlank()) return null
        val isPm = clean.contains("PM")
        val isAm = clean.contains("AM")
        val digits = clean.replace("AM", "").replace("PM", "").trim().split(":")
        if (digits.size < 2) return null
        var h = digits[0].trim().toIntOrNull() ?: return null
        val m = digits[1].trim().toIntOrNull() ?: return null
        if (isPm && h < 12) h += 12
        if (isAm && h == 12) h = 0
        return h * 60 + m
    }
}
