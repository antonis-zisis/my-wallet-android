package com.antoniszisis.mywallet.ui.subscriptions

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.antoniszisis.mywallet.BuildConfig
import com.antoniszisis.mywallet.util.getInitials
import com.antoniszisis.mywallet.util.getSubscriptionLogoUrl

@Composable
fun SubscriptionAvatar(
    name: String,
    url: String?,
    size: Dp = 32.dp,
) {
    var logoFailed by remember(url) { mutableStateOf(false) }
    val isDarkTheme = isSystemInDarkTheme()
    val logoUrl = remember(url, isDarkTheme) {
        getSubscriptionLogoUrl(url, BuildConfig.LOGO_DEV_TOKEN, isDarkTheme)
    }

    if (logoUrl != null && !logoFailed) {
        AsyncImage(
            model = logoUrl,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            onError = { logoFailed = true },
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(6.dp)),
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = getInitials(name),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}
