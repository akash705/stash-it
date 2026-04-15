package com.stashed.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val StashedPrimary = Color(0xFF4A6CF7)
private val StashedSecondary = Color(0xFF6B7FD7)
private val StashedTertiary = Color(0xFF9B59B6)

private val LightColors = lightColorScheme(
    primary = StashedPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8EDFE),
    onPrimaryContainer = Color(0xFF1A2B6D),
    secondary = StashedSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDF0FB),
    onSecondaryContainer = Color(0xFF2A3A6B),
    tertiary = StashedTertiary,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF5E9FA),
    onTertiaryContainer = Color(0xFF4A1E5E),
    surface = Color(0xFFFCFCFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFF1F3F9),
    onSurfaceVariant = Color(0xFF6B7280),
    outline = Color(0xFF9CA3AF),
    outlineVariant = Color(0xFFE5E7EB),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FA8FF),
    onPrimary = Color(0xFF0D2878),
    primaryContainer = Color(0xFF2A4494),
    onPrimaryContainer = Color(0xFFD6DFFF),
    secondary = Color(0xFF9DAEF7),
    onSecondary = Color(0xFF162456),
    secondaryContainer = Color(0xFF2E4070),
    onSecondaryContainer = Color(0xFFD8E0FD),
    tertiary = Color(0xFFC89CDE),
    onTertiary = Color(0xFF3A1150),
    tertiaryContainer = Color(0xFF52296A),
    onTertiaryContainer = Color(0xFFEDD8F7),
    surface = Color(0xFF121316),
    onSurface = Color(0xFFE3E3E6),
    surfaceVariant = Color(0xFF1E2024),
    onSurfaceVariant = Color(0xFF9CA3AF),
    outline = Color(0xFF6B7280),
    outlineVariant = Color(0xFF374151),
)

private val StashedShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun StashedTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
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
        shapes = StashedShapes,
        content = content,
    )
}
