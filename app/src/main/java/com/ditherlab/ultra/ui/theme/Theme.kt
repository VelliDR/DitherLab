package com.ditherlab.ultra.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryMatteGreen,
    onPrimary = OnPrimaryText,
    secondary = SecondaryMutedGreen,
    onSecondary = OnPrimaryText,
    tertiary = AccentOlive,
    onTertiary = OnPrimaryText,
    background = DeepCanvasBlack,
    onBackground = OnPrimaryText,
    surface = SurfaceDark,
    onSurface = OnPrimaryText,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceMutedText,
    error = WarningMutedRust,
    onError = OnPrimaryText
)

@Composable
fun DitherLabUltraTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DeepCanvasBlack.toArgb()
            window.navigationBarColor = DeepCanvasBlack.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
