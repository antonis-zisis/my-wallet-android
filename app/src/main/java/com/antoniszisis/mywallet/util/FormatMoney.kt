package com.antoniszisis.mywallet.util

import java.text.NumberFormat
import java.util.Locale

private val formatter = NumberFormat.getNumberInstance(Locale.GERMANY).apply {
    minimumFractionDigits = 2
    maximumFractionDigits = 2
}

private val compactFormatter = NumberFormat.getNumberInstance(Locale.GERMANY).apply {
    minimumFractionDigits = 0
    maximumFractionDigits = 1
}

fun formatMoney(amount: Double): String = "${formatter.format(amount)} €"

fun formatMoneyCompact(amount: Double): String = when {
    amount >= 1_000_000 -> "${compactFormatter.format(amount / 1_000_000)}M €"
    amount >= 1_000 -> "${compactFormatter.format(amount / 1_000)}K €"
    else -> formatMoney(amount)
}
