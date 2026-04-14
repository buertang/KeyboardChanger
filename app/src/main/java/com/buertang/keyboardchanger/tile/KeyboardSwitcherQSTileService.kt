package com.buertang.keyboardchanger.tile

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresPermission
import com.buertang.keyboardchanger.MainActivity
import com.buertang.keyboardchanger.R
import com.buertang.keyboardchanger.data.AppPreferences
import com.buertang.keyboardchanger.keyboard.KeyboardManagerActivity
import com.buertang.keyboardchanger.ui.language.AppLanguageManager

class KeyboardSwitcherQSTileService : TileService() {
    override fun onTileAdded() {
        super.onTileAdded()
        qsTile?.state = Tile.STATE_ACTIVE
        qsTile?.updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()

        val localizedContext = AppLanguageManager.wrap(
            this,
            AppPreferences.from(this).appLanguage
        )

        qsTile?.apply {
            label = localizedContext.getString(R.string.notification_title)
            icon = Icon.createWithResource(this@KeyboardSwitcherQSTileService, R.drawable.ic_keyboard_tile)
            state = Tile.STATE_ACTIVE
            updateTile()
        }
    }

    @RequiresPermission("android.permission.BROADCAST_CLOSE_SYSTEM_DIALOGS")
    @SuppressLint("StartActivityAndCollapseDeprecated")
    override fun onClick() {
        super.onClick()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            @Suppress("DEPRECATION")
            sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
        }

        val external = !MainActivity.isUiVisible()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                KeyboardManagerActivity.createTilePendingIntent(this, external)
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(
                KeyboardManagerActivity.createTileIntent(this, external)
            )
        }
    }
}
