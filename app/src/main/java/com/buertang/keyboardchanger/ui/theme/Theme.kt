package com.buertang.keyboardchanger.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightBaseColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

private val DarkBaseColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

@Composable
fun KeyboardChangerTheme(
    themeColorRgb: Int = DEFAULT_THEME_COLOR_RGB,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val seedColor = themeColorFromRgb(themeColorRgb)
    val colorScheme = if (darkTheme) {
        DarkBaseColorScheme.copy(
            primary = blendToward(seedColor, Color.White, 0.20f),
            onPrimary = contentColorFor(blendToward(seedColor, Color.White, 0.20f)),
            primaryContainer = blendToward(seedColor, Color.Black, 0.35f),
            onPrimaryContainer = contentColorFor(blendToward(seedColor, Color.Black, 0.35f)),
            secondary = blendToward(seedColor, Color.White, 0.35f),
            onSecondary = contentColorFor(blendToward(seedColor, Color.White, 0.35f)),
            secondaryContainer = blendToward(seedColor, Color.Black, 0.45f),
            onSecondaryContainer = contentColorFor(blendToward(seedColor, Color.Black, 0.45f)),
            tertiary = blendToward(seedColor, Color.White, 0.45f),
            onTertiary = contentColorFor(blendToward(seedColor, Color.White, 0.45f)),
            tertiaryContainer = blendToward(seedColor, Color.Black, 0.25f),
            onTertiaryContainer = contentColorFor(blendToward(seedColor, Color.Black, 0.25f))
        )
    } else {
        LightBaseColorScheme.copy(
            primary = seedColor,
            onPrimary = contentColorFor(seedColor),
            primaryContainer = blendToward(seedColor, Color.White, 0.78f),
            onPrimaryContainer = contentColorFor(blendToward(seedColor, Color.White, 0.78f)),
            secondary = blendToward(seedColor, Color.Black, 0.12f),
            onSecondary = contentColorFor(blendToward(seedColor, Color.Black, 0.12f)),
            secondaryContainer = blendToward(seedColor, Color.White, 0.68f),
            onSecondaryContainer = contentColorFor(blendToward(seedColor, Color.White, 0.68f)),
            tertiary = blendToward(seedColor, Color.White, 0.32f),
            onTertiary = contentColorFor(blendToward(seedColor, Color.White, 0.32f)),
            tertiaryContainer = blendToward(seedColor, Color.White, 0.82f),
            onTertiaryContainer = contentColorFor(blendToward(seedColor, Color.White, 0.82f))
        )
    }
    val view = LocalView.current

    SideEffect {
        val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
        val insetsController = WindowCompat.getInsetsController(window, view)
        val useDarkIcons = !darkTheme
        insetsController.isAppearanceLightStatusBars = useDarkIcons
        insetsController.isAppearanceLightNavigationBars = useDarkIcons
        window.statusBarColor = Color.Transparent.toArgb()
        window.navigationBarColor = colorScheme.surface.toArgb()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            content = content
        )
    }
}

private fun blendToward(color: Color, target: Color, amount: Float): Color {
    return lerp(color, target, amount.coerceIn(0f, 1f))
}

private fun contentColorFor(color: Color): Color {
    return if (color.luminance() > 0.5f) Color.Black else Color.White
}
