package com.buertang.keyboardchanger.ui.theme

import androidx.compose.ui.graphics.Color

const val DEFAULT_THEME_COLOR_RGB = 0x6E90A9

fun themeColorFromRgb(rgb: Int): Color {
    return Color(
        red = (rgb shr 16) and 0xFF,
        green = (rgb shr 8) and 0xFF,
        blue = rgb and 0xFF,
        alpha = 0xFF
    )
}

fun themeColorHex(rgb: Int): String {
    return String.format("#%06X", rgb and 0xFFFFFF)
}

