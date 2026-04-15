package com.stashed.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val StashedPrimary = Color(0xFF4A6CF7)
private val StashedSecondary = Color(0xFF6B7FD7)
private val StashedTertiary = Color(0xFF9B59B6)

private val LightColors = lightColorScheme(
    primary = StashedPrimary,
    secondary = StashedSecondary,
    tertiary = StashedTertiary,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FA8FF),
    secondary = Color(0xFF9DAEF7),
    tertiary = Color(0xFFC89CDE),
)

@Composable
fun StashedTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        // Material You — dynamic colors on Android 12+
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
