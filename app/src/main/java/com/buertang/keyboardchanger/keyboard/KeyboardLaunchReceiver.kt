package com.buertang.keyboardchanger.keyboard

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.buertang.keyboardchanger.MainActivity

class KeyboardLaunchReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val firstDelayMs = intent.getLongExtra(EXTRA_FIRST_DELAY_MS, DEFAULT_FIRST_DELAY_MS)
        val launchIntent = if (MainActivity.isUiVisible()) {
            KeyboardManagerActivity.createIntent(context, firstDelayMs)
        } else {
            KeyboardManagerActivity.createExternalIntent(context, firstDelayMs)
        }
        context.startActivity(launchIntent)
    }

    companion object {
        private const val ACTION_LAUNCH_KEYBOARD_PICKER =
            "com.buertang.keyboardchanger.action.LAUNCH_KEYBOARD_PICKER"
        private const val EXTRA_FIRST_DELAY_MS = "keyboard_picker_first_delay_ms"
        private const val DEFAULT_FIRST_DELAY_MS = 150L

        fun createPendingIntent(
            context: Context,
            requestCode: Int,
            firstDelayMs: Long = DEFAULT_FIRST_DELAY_MS
        ): PendingIntent {
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                Intent(context, KeyboardLaunchReceiver::class.java).apply {
                    action = ACTION_LAUNCH_KEYBOARD_PICKER
                    putExtra(EXTRA_FIRST_DELAY_MS, firstDelayMs)
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }
}
