package com.buertang.keyboardchanger.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.buertang.keyboardchanger.ui.theme.DEFAULT_THEME_COLOR_RGB
import com.buertang.keyboardchanger.ui.language.AppLanguage
import com.buertang.keyboardchanger.ui.theme.ThemeMode

class AppPreferences private constructor(
    private val preferences: SharedPreferences
) {
    var themeColorRgb: Int
        get() = preferences.getInt(PreferenceKeys.THEME_COLOR_RGB, DEFAULT_THEME_COLOR_RGB)
        set(value) = preferences.edit {
            putInt(PreferenceKeys.THEME_COLOR_RGB, value)
        }

    var themeMode: ThemeMode
        get() = ThemeMode.fromStorageValue(
            preferences.getString(PreferenceKeys.THEME_MODE, ThemeMode.SYSTEM.storageValue)
        )
        set(value) = preferences.edit {
            putString(PreferenceKeys.THEME_MODE, value.storageValue)
        }

    var appLanguage: AppLanguage
        get() = AppLanguage.fromStorageValue(
            preferences.getString(
                PreferenceKeys.APP_LANGUAGE,
                AppLanguage.SIMPLIFIED_CHINESE.storageValue
            )
        )
        set(value) = preferences.edit {
            putString(PreferenceKeys.APP_LANGUAGE, value.storageValue)
        }

    var notificationEnabled: Boolean
        get() = preferences.getBoolean(PreferenceKeys.NOTIFICATION_ENABLED, false)
        set(value) = preferences.edit {
            putBoolean(PreferenceKeys.NOTIFICATION_ENABLED, value)
        }

    var floatingButtonEnabled: Boolean
        get() = preferences.getBoolean(PreferenceKeys.FLOATING_BUTTON_ENABLED, false)
        set(value) = preferences.edit {
            putBoolean(PreferenceKeys.FLOATING_BUTTON_ENABLED, value)
        }

    var launchOnStartup: Boolean
        get() = preferences.getBoolean(PreferenceKeys.LAUNCH_ON_STARTUP, false)
        set(value) = preferences.edit {
            putBoolean(PreferenceKeys.LAUNCH_ON_STARTUP, value)
        }

    var floatingButtonLocked: Boolean
        get() = preferences.getBoolean(PreferenceKeys.FLOATING_BUTTON_LOCKED, false)
        set(value) = preferences.edit {
            putBoolean(PreferenceKeys.FLOATING_BUTTON_LOCKED, value)
        }

    var floatingButtonSize: Int
        get() = preferences.getInt(PreferenceKeys.FLOATING_BUTTON_SIZE, 100)
        set(value) = preferences.edit {
            putInt(PreferenceKeys.FLOATING_BUTTON_SIZE, value)
        }

    var floatingButtonX: Int
        get() = preferences.getInt(PreferenceKeys.FLOATING_BUTTON_X, 0)
        set(value) = preferences.edit {
            putInt(PreferenceKeys.FLOATING_BUTTON_X, value)
        }

    var floatingButtonY: Int
        get() = preferences.getInt(PreferenceKeys.FLOATING_BUTTON_Y, 300)
        set(value) = preferences.edit {
            putInt(PreferenceKeys.FLOATING_BUTTON_Y, value)
        }

    companion object {
        fun from(context: Context): AppPreferences {
            return AppPreferences(
                context.applicationContext.getSharedPreferences(
                    PreferenceKeys.PREFS_NAME,
                    Context.MODE_PRIVATE
                )
            )
        }
    }
}
