package com.buertang.keyboardchanger.shortcut

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.buertang.keyboardchanger.data.AppPreferences
import com.buertang.keyboardchanger.ui.language.AppLanguageManager

class CreateShortcutActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        val appLanguage = AppPreferences.from(newBase).appLanguage
        super.attachBaseContext(AppLanguageManager.wrap(newBase, appLanguage))
    }
}
