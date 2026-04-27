package com.antoniszisis.mywallet.ui.theme

import androidx.compose.ui.graphics.Color

// Cross-platform canonical palette — keep in sync with web/categoryColors.ts

object CategoryColors {
    val expense: Map<String, Color> = mapOf(
        "Dining Out" to Color(0xFFFB923C),
        "Entertainment" to Color(0xFFA855F7),
        "Groceries" to Color(0xFFF97316),
        "Health" to Color(0xFF14B8A6),
        "Insurance" to Color(0xFF60A5FA),
        "Investment" to Color(0xFF10B981),
        "Loan" to Color(0xFF1E3A8A),
        "Other" to Color(0xFF9CA3AF),
        "Rent" to Color(0xFF1D4ED8),
        "Shopping" to Color(0xFFEC4899),
        "Transport" to Color(0xFF0891B2),
        "Utilities" to Color(0xFF3B82F6),
    )

    val budgetBucket: Map<String, Color> = mapOf(
        "Invest" to Color(0xFF10B981),
        "Needs" to Color(0xFF3B82F6),
        "Wants" to Color(0xFFF59E0B),
    )

    val asset: Map<String, Color> = mapOf(
        "Brokerage" to Color(0xFF06B6D4),
        "Crypto" to Color(0xFFF59E0B),
        "Investments" to Color(0xFF10B981),
        "Other" to Color(0xFF9CA3AF),
        "Real Estate" to Color(0xFFEF4444),
        "Retirement" to Color(0xFF8B5CF6),
        "Savings" to Color(0xFF3B82F6),
        "Vehicle" to Color(0xFF6366F1),
    )

    val liability: Map<String, Color> = mapOf(
        "Car Loan" to Color(0xFFF97316),
        "Credit Card" to Color(0xFFEC4899),
        "Mortgage" to Color(0xFFDC2626),
        "Other" to Color(0xFF9CA3AF),
        "Personal Loan" to Color(0xFF8B5CF6),
        "Student Loan" to Color(0xFFF59E0B),
    )

    val fallback = Color(0xFF9CA3AF)

    fun forExpense(category: String) = expense[category] ?: fallback
    fun forAsset(category: String) = asset[category] ?: fallback
    fun forLiability(category: String) = liability[category] ?: fallback
    fun forBudgetBucket(bucket: String) = budgetBucket[bucket] ?: fallback
}
