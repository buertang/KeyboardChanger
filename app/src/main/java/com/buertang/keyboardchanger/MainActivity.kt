package com.buertang.keyboardchanger

import android.content.res.Configuration
import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.appcompat.app.AppCompatActivity
import com.buertang.keyboardchanger.data.AppPreferences
import com.buertang.keyboardchanger.service.KeyboardSwitcherService
import com.buertang.keyboardchanger.ui.language.AppLanguageManager
import com.buertang.keyboardchanger.ui.language.LocalizedAppContent
import com.buertang.keyboardchanger.ui.navigation.AppNavGraph
import com.buertang.keyboardchanger.ui.theme.KeyboardChangerTheme
import com.buertang.keyboardchanger.ui.theme.ThemeMode

class MainActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        val appLanguage = AppPreferences.from(newBase).appLanguage
        super.attachBaseContext(AppLanguageManager.wrap(newBase, appLanguage))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val appPreferences = AppPreferences.from(this)
        setTheme(resolveActivityTheme(appPreferences.themeMode))
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val rememberedPreferences = remember { appPreferences }
            var themeColorRgb by remember {
                mutableIntStateOf(rememberedPreferences.themeColorRgb)
            }
            var themeMode by remember {
                mutableStateOf(rememberedPreferences.themeMode)
            }
            var appLanguage by remember {
                mutableStateOf(rememberedPreferences.appLanguage)
            }

            LocalizedAppContent(appLanguage = appLanguage) {
                KeyboardChangerTheme(
                    themeColorRgb = themeColorRgb,
                    themeMode = themeMode
                ) {
                    AppNavGraph(
                        themeColorRgb = themeColorRgb,
                        themeMode = themeMode,
                        appLanguage = appLanguage,
                        onThemeColorChanged = { value ->
                            themeColorRgb = value
                            rememberedPreferences.themeColorRgb = value
                        },
                        onThemeModeChanged = { value ->
                            themeMode = value
                            rememberedPreferences.themeMode = value
                        },
                        onAppLanguageChanged = { value ->
                            appLanguage = value
                            rememberedPreferences.appLanguage = value
                            KeyboardSwitcherService.refreshLanguage(this@MainActivity)
                        }
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        isVisible = true
    }

    override fun onStop() {
        isVisible = false
        super.onStop()
    }

    private fun resolveActivityTheme(themeMode: ThemeMode): Int {
        val darkModeEnabled = when (themeMode) {
            ThemeMode.SYSTEM -> {
                val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                nightModeFlags == Configuration.UI_MODE_NIGHT_YES
            }

            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }

        return if (darkModeEnabled) {
            R.style.Theme_KeyboardChanger_Dark
        } else {
            R.style.Theme_KeyboardChanger_Light
        }
    }

    companion object {
        @Volatile
        private var isVisible: Boolean = false

        fun isUiVisible(): Boolean = isVisible
    }
}
