package com.buertang.keyboardchanger.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.buertang.keyboardchanger.data.AppPreferences
import com.buertang.keyboardchanger.service.KeyboardSwitcherService

class BootUpReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val appPreferences = AppPreferences.from(context)
        if (!appPreferences.launchOnStartup) return

        val overlayAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
            Settings.canDrawOverlays(context)

        when {
            appPreferences.floatingButtonEnabled && overlayAllowed -> {
                KeyboardSwitcherService.startOverlay(context)
            }

            appPreferences.notificationEnabled -> {
                KeyboardSwitcherService.startNotification(context)
            }
        }
    }
}
