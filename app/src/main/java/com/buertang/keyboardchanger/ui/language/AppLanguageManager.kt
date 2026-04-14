package com.buertang.keyboardchanger.ui.language

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import java.util.Locale

object AppLanguageManager {
    fun wrap(base: Context, language: AppLanguage): Context {
        val locale = Locale.forLanguageTag(language.localeTag)
        val overrideConfiguration = Configuration(base.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        val localizedContext = base.createConfigurationContext(overrideConfiguration)
        return LocalizedContextWrapper(base, localizedContext)
    }

    fun apply(language: AppLanguage) {
        // No-op. Locale is applied by wrapping component contexts and Compose locals.
    }
}

private class LocalizedContextWrapper(
    base: Context,
    private val localizedContext: Context
) : ContextWrapper(base) {
    override fun getResources() = localizedContext.resources

    override fun getAssets() = localizedContext.assets

    override fun getTheme() = localizedContext.theme

    override fun getSystemService(name: String): Any? {
        return when (name) {
            Context.LAYOUT_INFLATER_SERVICE -> localizedContext.getSystemService(name)
            else -> super.getSystemService(name)
        }
    }
}
