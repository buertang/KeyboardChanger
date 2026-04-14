package com.buertang.keyboardchanger.ui.language

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection

@Composable
fun LocalizedAppContent(
    appLanguage: AppLanguage,
    content: @Composable () -> Unit
) {
    val baseContext = LocalContext.current
    val baseConfiguration = LocalConfiguration.current

    val localizedContext = remember(baseContext, baseConfiguration, appLanguage) {
        AppLanguageManager.wrap(baseContext, appLanguage)
    }

    val providedConfiguration = remember(localizedContext) {
        Configuration(localizedContext.resources.configuration)
    }
    val layoutDirection = remember(providedConfiguration) {
        when (providedConfiguration.layoutDirection) {
            android.util.LayoutDirection.RTL -> LayoutDirection.Rtl
            else -> LayoutDirection.Ltr
        }
    }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides providedConfiguration,
        LocalLayoutDirection provides layoutDirection,
        content = content
    )
}
