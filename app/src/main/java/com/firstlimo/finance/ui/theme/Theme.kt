package com.firstlimo.finance.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val DarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = DarkBackground,
    primaryContainer = DarkSurface,
    secondary = AccentGold,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkCardSurface,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun FirstLimoFinanceTheme(
    content: @Composable () -> Unit
) {
    // تطبيق الوضع الداكن الفاخر (Dark Mode) والاتجاه العربي RTL بشكل أصيل وشامل
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = Typography,
            content = content
        )
    }
}
