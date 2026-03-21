package com.mywallet.android.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val displayFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
private val shortFormatter = DateTimeFormatter.ofPattern("MMM d")
private val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

fun formatDate(isoString: String): String {
    return try {
        val instant = Instant.parse(isoString)
        val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
        localDate.format(displayFormatter)
    } catch (e: Exception) {
        try {
            val localDate = LocalDate.parse(isoString.take(10))
            localDate.format(displayFormatter)
        } catch (e2: Exception) {
            isoString
        }
    }
}

fun formatDateShort(isoString: String): String {
    return try {
        val instant = Instant.parse(isoString)
        val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
        localDate.format(shortFormatter)
    } catch (e: Exception) {
        try {
            val localDate = LocalDate.parse(isoString.take(10))
            localDate.format(shortFormatter)
        } catch (e2: Exception) {
            isoString
        }
    }
}

fun LocalDate.toIsoString(): String = this.format(inputFormatter)

fun today(): String = LocalDate.now().toIsoString()

fun getNextRenewalDate(startDate: String, billingCycle: String): LocalDate? {
    return try {
        val start = LocalDate.parse(startDate.take(10))
        val now = LocalDate.now()
        if (billingCycle == "MONTHLY") {
            var next = start
            while (!next.isAfter(now)) {
                next = next.plusMonths(1)
            }
            next
        } else {
            var next = start
            while (!next.isAfter(now)) {
                next = next.plusYears(1)
            }
            next
        }
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
