package com.mywallet.android.util

import java.text.NumberFormat
import java.util.Locale

private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)

fun formatMoney(amount: Double): String {
    return currencyFormatter.format(amount)
}

fun formatMoneyCompact(amount: Double): String {
    return when {
        amount >= 1_000_000 -> "${currencyFormatter.format(amount / 1_000_000)}M"
        amount >= 1_000 -> "${currencyFormatter.format(amount / 1_000)}K"
        else -> currencyFormatter.format(amount)
    }
}
