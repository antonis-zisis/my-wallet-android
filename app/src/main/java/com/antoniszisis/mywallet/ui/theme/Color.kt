package com.antoniszisis.mywallet.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Indigo (brand — replaces Blue as primary)
val Indigo50 = Color(0xFFEEF2FF)
val Indigo100 = Color(0xFFE0E7FF)
val Indigo400 = Color(0xFF818CF8)
val Indigo600 = Color(0xFF4F46E5)
val Indigo700 = Color(0xFF4338CA)
val Indigo800 = Color(0xFF3730A3)

// Blue (kept for call-sites not yet migrated)
val Blue500 = Color(0xFF3B82F6)
val Blue600 = Color(0xFF2563EB)
val Blue700 = Color(0xFF1D4ED8)
val Blue100 = Color(0xFFDBEAFE)
val Blue50 = Color(0xFFEFF6FF)

val Green400 = Color(0xFF34D399)
val Green500 = Color(0xFF10B981)
val Green600 = Color(0xFF059669)
val Green100 = Color(0xFFD1FAE5)
val Green50 = Color(0xFFECFDF5)
val Green900 = Color(0xFF064E3B)

val Red400 = Color(0xFFF87171)
val Red500 = Color(0xFFEF4444)
val Red600 = Color(0xFFDC2626)
val Red100 = Color(0xFFFEE2E2)
val Red50 = Color(0xFFFEF2F2)
val Red900 = Color(0xFF7F1D1D)

val Amber400 = Color(0xFFFBBF24)
val Amber500 = Color(0xFFF59E0B)
val Amber100 = Color(0xFFFDE68A)

val Gray50 = Color(0xFFF9FAFB)
val Gray100 = Color(0xFFF3F4F6)
val Gray200 = Color(0xFFE5E7EB)
val Gray300 = Color(0xFFD1D5DB)
val Gray400 = Color(0xFF9CA3AF)
val Gray500 = Color(0xFF6B7280)
val Gray600 = Color(0xFF4B5563)
val Gray700 = Color(0xFF374151)
val Gray800 = Color(0xFF1F2937)
val Gray900 = Color(0xFF111827)

// Light theme
val LightPrimary = Indigo600
val LightOnPrimary = Color.White
val LightPrimaryContainer = Indigo100
val LightOnPrimaryContainer = Indigo800
val LightSecondary = Gray600
val LightOnSecondary = Color.White
val LightSecondaryContainer = Gray100
val LightOnSecondaryContainer = Gray700
val LightBackground = Gray50
val LightOnBackground = Gray900
val LightSurface = Color.White
val LightOnSurface = Gray900
val LightSurfaceVariant = Gray100
val LightOnSurfaceVariant = Gray600
val LightError = Red500
val LightOnError = Color.White
val LightErrorContainer = Red100
val LightOutline = Gray300

// Dark theme
val DarkPrimary = Indigo600
val DarkOnPrimary = Color.White
val DarkPrimaryContainer = Indigo800
val DarkOnPrimaryContainer = Indigo100
val DarkSecondary = Gray400
val DarkOnSecondary = Gray900
val DarkSecondaryContainer = Gray700
val DarkOnSecondaryContainer = Gray100
val DarkBackground = Gray900
val DarkOnBackground = Gray100
val DarkSurface = Gray800
val DarkOnSurface = Gray100
val DarkSurfaceVariant = Gray700
val DarkOnSurfaceVariant = Gray400
val DarkError = Red500
val DarkOnError = Color.White
val DarkErrorContainer = Color(0xFF7F1D1D)
val DarkOutline = Gray600

// Semantic color getters — resolve to dark-lift variants in dark mode
@Composable fun incomeColor() = if (isSystemInDarkTheme()) Green400 else Green500
@Composable fun expenseColor() = if (isSystemInDarkTheme()) Red400 else Red500
@Composable fun netWorthColor(positive: Boolean) = if (positive) incomeColor() else expenseColor()

// Badge color pairs (background to foreground) — dark mode uses deep containers with lifted text
@Composable fun monthlyBadgeColors() = if (isSystemInDarkTheme()) Green900 to Green400 else Green100 to Green600
@Composable fun yearlyBadgeColors() = if (isSystemInDarkTheme()) Indigo800 to Indigo400 else Indigo100 to Indigo600
@Composable fun cancelledBadgeColors() = if (isSystemInDarkTheme()) Red900 to Red400 else Red100 to Red600
