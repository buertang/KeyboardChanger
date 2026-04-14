package com.buertang.keyboardchanger.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.buertang.keyboardchanger.R
import com.buertang.keyboardchanger.keyboard.KeyboardLaunchReceiver

class KeyboardWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { widgetId ->
            val remoteViews = RemoteViews(
                context.packageName,
                R.layout.widget_keyboard_switcher
            )

            val pendingIntent = KeyboardLaunchReceiver.createPendingIntent(
                context = context,
                requestCode = widgetId,
                firstDelayMs = 150L
            )
            remoteViews.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            remoteViews.setOnClickPendingIntent(R.id.widget_icon, pendingIntent)

            appWidgetManager.updateAppWidget(widgetId, remoteViews)
        }
    }
}
