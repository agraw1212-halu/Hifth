package com.hifth.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Emerald = Color(0xFF176B52)
private val Gold = Color(0xFFB58A3A)

@Composable
fun HifthTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) {
        darkColorScheme(
            primary = Color(0xFF84D6B2),
            secondary = Color(0xFFE7C77D),
            tertiary = Color(0xFFA9C9B7),
            background = Color(0xFF101713),
            surface = Color(0xFF18211C)
        )
    } else {
        lightColorScheme(
            primary = Emerald,
            secondary = Gold,
            tertiary = Color(0xFF507965),
            background = Color(0xFFF7F8F4),
            surface = Color(0xFFFFFFFF)
        )
    }
    MaterialTheme(colorScheme = colors, content = content)
}
