package com.mywallet.android.util

import java.time.Instant
import java.time.ZoneId

private fun parseToInstant(raw: String): Instant {
    if (raw.all { it.isDigit() }) {
        return Instant.ofEpochMilli(raw.toLong())
    }
    return try {
        Instant.parse(raw)
    } catch (_: Exception) {
        java.time.LocalDate.parse(raw.take(10))
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
    }
}

fun formatRelativeTime(raw: String): String {
    val target = try {
        parseToInstant(raw)
    } catch (_: Exception) {
        return formatDate(raw)
    }
    val now = Instant.now()
    val diffMs = now.toEpochMilli() - target.toEpochMilli()
    val diffMinutes = diffMs / (1000 * 60)
    val diffHours = diffMs / (1000 * 60 * 60)
    val diffDays = diffMs / (1000 * 60 * 60 * 24)

    return when {
        diffMinutes < 1 -> "just now"
        diffMinutes < 60 -> if (diffMinutes == 1L) "1 minute ago" else "$diffMinutes minutes ago"
        diffHours < 24 -> if (diffHours == 1L) "1 hour ago" else "$diffHours hours ago"
        diffDays == 1L -> "yesterday"
        diffDays < 30 -> "$diffDays days ago"
        else -> formatDate(raw)
    }
}
