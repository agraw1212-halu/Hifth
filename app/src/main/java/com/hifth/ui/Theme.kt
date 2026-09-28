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
            primary = Color(0xFF3D9A82),
            onPrimary = Color(0xFF07110D),
            secondary = Color(0xFFD4AF57),
            onSecondary = Color(0xFF08120F),
            tertiary = Color(0xFF83B7A2),
            background = Color(0xFF08120F),
            onBackground = Color(0xFFEDE6D6),
            surface = Color(0xFF10211B),
            onSurface = Color(0xFFEDE6D6),
            surfaceVariant = Color(0xFF173028),
            onSurfaceVariant = Color(0xFF9AA89F),
            error = Color(0xFFE07A72)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF0F5C4C),
            onPrimary = Color(0xFFF7F1E4),
            secondary = Color(0xFFB8892D),
            onSecondary = Color(0xFF12241D),
            tertiary = Color(0xFF507965),
            background = Color(0xFFF3EEE3),
            onBackground = Color(0xFF12241D),
            surface = Color(0xFFFBF7EF),
            onSurface = Color(0xFF12241D),
            surfaceVariant = Color(0xFFEFE6D4),
            onSurfaceVariant = Color(0xFF5C6D65),
            outline = Color(0xFFDED7CA),
            error = Color(0xFF9B3A32)
        )
    }
    MaterialTheme(colorScheme = colors, content = content)
}
