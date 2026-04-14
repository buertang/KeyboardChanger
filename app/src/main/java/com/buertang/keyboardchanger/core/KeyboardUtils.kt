package com.buertang.keyboardchanger.core

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.buertang.keyboardchanger.R

object KeyboardUtils {

    fun openAvailableKeyboards(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                context,
                context.getString(R.string.error_open_keyboard_settings),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun chooseKeyboard(context: Context) {
        val inputMethodManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager

        if (inputMethodManager != null) {
            inputMethodManager.showInputMethodPicker()
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.error_open_keyboard_picker),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun openOverlaySettings(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return

        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                context,
                context.getString(R.string.open_overlay_settings_failed),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
