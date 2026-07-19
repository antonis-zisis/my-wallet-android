package com.antoniszisis.mywallet.util

import android.net.Uri

private fun isSafeUrl(url: String): Boolean {
    val scheme = Uri.parse(url).scheme
    return scheme == "http" || scheme == "https"
}

fun getSubscriptionLogoUrl(url: String?, token: String, isDarkTheme: Boolean): String? {
    if (token.isBlank() || url.isNullOrBlank() || !isSafeUrl(url)) return null

    val domain = Uri.parse(url).host?.removePrefix("www.")
    if (domain.isNullOrBlank()) return null

    return Uri.parse("https://img.logo.dev/$domain")
        .buildUpon()
        .appendQueryParameter("token", token)
        .appendQueryParameter("size", "128")
        .appendQueryParameter("format", "png")
        .appendQueryParameter("theme", if (isDarkTheme) "dark" else "light")
        .build()
        .toString()
}
