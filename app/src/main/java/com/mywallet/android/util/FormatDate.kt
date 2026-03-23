package com.mywallet.android.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val displayFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
private val shortFormatter = DateTimeFormatter.ofPattern("MMM d")
private val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

/** Parses any date string the backend may send: epoch-ms, ISO timestamp, or plain date. */
private fun parseToLocalDate(raw: String): LocalDate {
    // Epoch milliseconds (graphql-js serializes Prisma DateTime as epoch ms string)
    if (raw.all { it.isDigit() }) {
        return Instant.ofEpochMilli(raw.toLong())
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }
    return try {
        Instant.parse(raw).atZone(ZoneId.systemDefault()).toLocalDate()
    } catch (_: Exception) {
        LocalDate.parse(raw.take(10))
    }
}

fun formatDate(isoString: String): String {
    return try {
        parseToLocalDate(isoString).format(displayFormatter)
    } catch (e: Exception) {
        isoString
    }
}

fun formatDateShort(isoString: String): String {
    return try {
        parseToLocalDate(isoString).format(shortFormatter)
    } catch (e: Exception) {
        isoString
    }
}

fun LocalDate.toIsoString(): String = this.format(inputFormatter)

/** Converts any backend date (epoch-ms, ISO timestamp, or plain date) to yyyy-MM-dd. */
fun toInputDate(raw: String): String = try {
    parseToLocalDate(raw).format(inputFormatter)
} catch (_: Exception) {
    raw
}

fun today(): String = LocalDate.now().toIsoString()

fun getNextRenewalDate(startDate: String, billingCycle: String): LocalDate? {
    return try {
        val start = parseToLocalDate(startDate)
        val now = LocalDate.now()
        val increment = if (billingCycle == "MONTHLY") 1L else 12L
        var next = start
        while (!next.isAfter(now)) {
            next = next.plusMonths(increment)
        }
        next
    } catch (e: Exception) {
        null
    }
}

fun getInitials(fullName: String?): String {
    if (fullName.isNullOrBlank()) return "?"
    val parts = fullName.trim().split(" ")
    return when {
        parts.size >= 2 -> "${parts.first().first()}${parts.last().first()}".uppercase()
        parts.size == 1 -> parts.first().take(2).uppercase()
        else -> "?"
    }
}
