package com.buertang.keyboardchanger.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.buertang.keyboardchanger.MainActivity
import com.buertang.keyboardchanger.R
import com.buertang.keyboardchanger.data.AppPreferences
import com.buertang.keyboardchanger.keyboard.KeyboardLaunchReceiver
import com.buertang.keyboardchanger.keyboard.KeyboardManagerActivity
import com.buertang.keyboardchanger.ui.language.AppLanguageManager
import kotlin.math.abs
import kotlin.math.max

class KeyboardSwitcherService : Service(), View.OnTouchListener {
    private enum class EdgeAnchor {
        LEFT,
        RIGHT
    }

    private lateinit var windowManager: WindowManager
    private lateinit var appPreferences: AppPreferences

    private var floatingButton: FrameLayout? = null
    private var floatingIcon: ImageView? = null
    private var floatingLayoutParams: WindowManager.LayoutParams? = null

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var moved = false
    private var isFloatingButtonIdle = false
    private var isFloatingButtonPartiallyHidden = false

    private val idleFadeRunnable = Runnable {
        val button = floatingButton ?: return@Runnable
        isFloatingButtonIdle = true
        applyFloatingButtonAlpha(button, animate = true)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        appPreferences = AppPreferences.from(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_NOTIFICATION -> ensureForeground()
            ACTION_REFRESH_LANGUAGE -> {
                createNotificationChannel()
                if (appPreferences.notificationEnabled || isFloatingButtonVisible()) {
                    ensureForeground()
                }
            }
            ACTION_STOP_NOTIFICATION -> {
                if (!isFloatingButtonVisible()) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
            ACTION_START_OVERLAY -> {
                ensureForeground()
                showFloatingButton()
            }
            ACTION_STOP_OVERLAY -> {
                removeFloatingButton()
                if (!appPreferences.notificationEnabled) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
            ACTION_UPDATE_OVERLAY_SIZE -> {
                updateFloatingButtonSize()
            }
            else -> stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun ensureForeground() {
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    private fun localizedContext(): Context {
        return AppLanguageManager.wrap(this, appPreferences.appLanguage)
    }

    private fun buildNotification(): android.app.Notification {
        val localizedContext = localizedContext()

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(localizedContext.getString(R.string.notification_title))
            .setContentText(localizedContext.getString(R.string.notification_content))
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(createPendingIntent())
            .build()
    }

    private fun createPendingIntent(): PendingIntent {
        return KeyboardLaunchReceiver.createPendingIntent(
            context = this,
            requestCode = REQUEST_CODE_NOTIFICATION_LAUNCH,
            firstDelayMs = NOTIFICATION_DELAY_MS
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val localizedContext = localizedContext()
            val channel = NotificationChannel(
                CHANNEL_ID,
                localizedContext.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun calculateFloatingButtonSizePx(): Int {
        val baseSizeDp = 56
        val density = resources.displayMetrics.density
        return (baseSizeDp * density * appPreferences.floatingButtonSize / 100f).toInt()
    }

    private fun calculateFloatingIconSizePx(buttonSizePx: Int): Int {
        return max(dpToPx(20f), (buttonSizePx * 0.7f).toInt())
    }

    private fun applyFloatingButtonAppearance(button: FrameLayout, icon: ImageView, sizePx: Int) {
        val topColor = blendWithWhite(appPreferences.themeColorRgb, 0.18f)
        val bottomColor = blendWithBlack(appPreferences.themeColorRgb, 0.20f)
        val strokeColor = blendWithBlack(appPreferences.themeColorRgb, 0.32f)

        button.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(topColor, bottomColor)
        ).apply {
            shape = GradientDrawable.OVAL
            setStroke(dpToPx(1f), strokeColor)
        }

        val iconSizePx = calculateFloatingIconSizePx(sizePx)
        val iconLayoutParams = FrameLayout.LayoutParams(iconSizePx, iconSizePx, Gravity.CENTER)
        icon.layoutParams = iconLayoutParams
        icon.setImageResource(R.drawable.ic_keyboard_tile)
        icon.scaleType = ImageView.ScaleType.FIT_CENTER
        icon.setPadding(0, 0, 0, 0)
    }

    private fun showFloatingButton() {
        if (floatingButton != null) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !Settings.canDrawOverlays(this)
        ) {
            return
        }

        @Suppress("DEPRECATION")
        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }

        val sizePx = calculateFloatingButtonSizePx()

        floatingLayoutParams = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = appPreferences.floatingButtonX
            y = appPreferences.floatingButtonY
        }

        val iconView = ImageView(this)
        floatingIcon = iconView

        floatingButton = FrameLayout(this).apply {
            isClickable = true
            isFocusable = false
            setOnTouchListener(this@KeyboardSwitcherService)
            setOnClickListener {
                val launchIntent = if (MainActivity.isUiVisible()) {
                    KeyboardManagerActivity.createIntent(this@KeyboardSwitcherService)
                } else {
                    KeyboardManagerActivity.createExternalIntent(this@KeyboardSwitcherService)
                }
                startActivity(launchIntent)
            }
            applyFloatingButtonAppearance(this, iconView, sizePx)
            addView(iconView)
        }

        windowManager.addView(floatingButton, floatingLayoutParams)
        floatingButton?.let { button ->
            floatingLayoutParams?.let { params ->
                applyFreeState(button, params)
            }
        }
        scheduleIdleFade()
    }

    private fun updateFloatingButtonSize() {
        val button = floatingButton ?: return
        val icon = floatingIcon ?: return
        val params = floatingLayoutParams ?: return

        val sizePx = calculateFloatingButtonSizePx()
        params.width = sizePx
        params.height = sizePx
        applyFloatingButtonAppearance(button, icon, sizePx)
        val anchor = edgeAnchorForX(params.x, maxXForButton(button, params))
        if (anchor != null) {
            applyAnchoredState(button, params, anchor, isFloatingButtonPartiallyHidden)
        } else {
            applyFreeState(button, params)
        }
        scheduleIdleFade()
    }

    private fun removeFloatingButton() {
        cancelIdleFade()
        floatingButton?.let {
            windowManager.removeView(it)
        }
        floatingButton = null
        floatingIcon = null
        floatingLayoutParams = null
        isFloatingButtonPartiallyHidden = false
    }

    private fun isFloatingButtonVisible(): Boolean = floatingButton != null

    private fun edgeAnchorForX(x: Int, maxX: Int): EdgeAnchor? {
        val dockThresholdPx = dpToPx(DOCK_THRESHOLD_DP)
        return when {
            x <= dockThresholdPx -> EdgeAnchor.LEFT
            x >= maxX - dockThresholdPx -> EdgeAnchor.RIGHT
            else -> null
        }
    }

    private fun snapToNearestEdge(view: View, params: WindowManager.LayoutParams) {
        val buttonWidth = view.width.takeIf { it > 0 } ?: params.width
        val centerX = params.x + buttonWidth / 2
        val anchor = if (centerX < resources.displayMetrics.widthPixels / 2) {
            EdgeAnchor.LEFT
        } else {
            EdgeAnchor.RIGHT
        }
        applyAnchoredState(view, params, anchor, partiallyHidden = false)
    }

    private fun applyAnchoredState(
        view: View,
        params: WindowManager.LayoutParams,
        anchor: EdgeAnchor,
        partiallyHidden: Boolean
    ) {
        val buttonWidth = view.width.takeIf { it > 0 } ?: params.width
        val hiddenOffset = hiddenOffsetPx(buttonWidth).toFloat()
        params.x = when (anchor) {
            EdgeAnchor.LEFT -> 0
            EdgeAnchor.RIGHT -> maxXForButton(view, params)
        }
        params.y = params.y.coerceIn(0, maxYForButton(view, params))
        isFloatingButtonPartiallyHidden = partiallyHidden
        view.translationX = when {
            !partiallyHidden -> 0f
            anchor == EdgeAnchor.LEFT -> -hiddenOffset
            else -> hiddenOffset
        }
        windowManager.updateViewLayout(view, params)
        applyFloatingButtonAlpha(view, animate = false)
    }

    private fun applyFreeState(view: View, params: WindowManager.LayoutParams) {
        params.x = params.x.coerceIn(0, maxXForButton(view, params))
        params.y = params.y.coerceIn(0, maxYForButton(view, params))
        isFloatingButtonPartiallyHidden = false
        view.translationX = 0f
        windowManager.updateViewLayout(view, params)
        applyFloatingButtonAlpha(view, animate = false)
    }

    private fun maxXForButton(view: View, params: WindowManager.LayoutParams): Int {
        val buttonWidth = view.width.takeIf { it > 0 } ?: params.width
        return (resources.displayMetrics.widthPixels - buttonWidth).coerceAtLeast(0)
    }

    private fun maxYForButton(view: View, params: WindowManager.LayoutParams): Int {
        val buttonHeight = view.height.takeIf { it > 0 } ?: params.height
        return (resources.displayMetrics.heightPixels - buttonHeight).coerceAtLeast(0)
    }

    private fun visibleWidthPx(buttonWidth: Int): Int {
        return (buttonWidth * DOCK_VISIBLE_RATIO).toInt()
            .coerceIn(dpToPx(MIN_VISIBLE_WIDTH_DP), buttonWidth)
    }

    private fun hiddenOffsetPx(buttonWidth: Int): Int {
        return (buttonWidth - visibleWidthPx(buttonWidth)).coerceAtLeast(0)
    }

    private fun applyFloatingButtonAlpha(view: View, animate: Boolean) {
        val targetAlpha = when {
            isFloatingButtonIdle && isFloatingButtonPartiallyHidden -> IDLE_DOCKED_ALPHA
            isFloatingButtonIdle -> IDLE_ALPHA
            else -> ACTIVE_ALPHA
        }

        if (animate) {
            view.animate()
                .alpha(targetAlpha)
                .setDuration(ALPHA_ANIMATION_DURATION_MS)
                .start()
        } else {
            view.animate().cancel()
            view.alpha = targetAlpha
        }
    }

    private fun cancelIdleFade() {
        floatingButton?.removeCallbacks(idleFadeRunnable)
    }

    private fun scheduleIdleFade() {
        val button = floatingButton ?: return
        cancelIdleFade()
        isFloatingButtonIdle = false
        applyFloatingButtonAlpha(button, animate = true)
        button.postDelayed(idleFadeRunnable, IDLE_FADE_DELAY_MS)
    }

    private fun dpToPx(value: Float): Int {
        return (value * resources.displayMetrics.density).toInt().coerceAtLeast(1)
    }

    private fun blendWithWhite(rgb: Int, amount: Float): Int {
        return blendColor(rgb, 0xFFFFFF, amount)
    }

    private fun blendWithBlack(rgb: Int, amount: Float): Int {
        return blendColor(rgb, 0x000000, amount)
    }

    private fun blendColor(baseRgb: Int, targetRgb: Int, amount: Float): Int {
        val ratio = amount.coerceIn(0f, 1f)
        val baseRed = (baseRgb shr 16) and 0xFF
        val baseGreen = (baseRgb shr 8) and 0xFF
        val baseBlue = baseRgb and 0xFF
        val targetRed = (targetRgb shr 16) and 0xFF
        val targetGreen = (targetRgb shr 8) and 0xFF
        val targetBlue = targetRgb and 0xFF

        val red = (baseRed + ((targetRed - baseRed) * ratio)).toInt().coerceIn(0, 255)
        val green = (baseGreen + ((targetGreen - baseGreen) * ratio)).toInt().coerceIn(0, 255)
        val blue = (baseBlue + ((targetBlue - baseBlue) * ratio)).toInt().coerceIn(0, 255)

        return Color.rgb(red, green, blue)
    }

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        val params = floatingLayoutParams ?: return false
        val locked = appPreferences.floatingButtonLocked

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                cancelIdleFade()
                isFloatingButtonIdle = false
                applyFloatingButtonAlpha(view, animate = true)

                if (locked) {
                    return false
                }

                moved = false
                initialX = params.x
                initialY = params.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                return false
            }

            MotionEvent.ACTION_MOVE -> {
                if (locked) {
                    return false
                }

                val dx = (event.rawX - initialTouchX).toInt()
                val dy = (event.rawY - initialTouchY).toInt()

                if (!moved && (abs(dx) > 8 || abs(dy) > 8)) {
                    moved = true
                }

                if (moved) {
                    val maxX = maxXForButton(view, params)
                    val startAnchor = edgeAnchorForX(initialX, maxX)
                    val toggleThresholdPx = dpToPx(EDGE_TOGGLE_THRESHOLD_DP)

                    params.x = (initialX + dx).coerceIn(0, maxX)
                    params.y = initialY + dy
                    params.y = params.y.coerceIn(0, maxYForButton(view, params))
                    isFloatingButtonIdle = false

                    val shouldHideOnEdge = when (startAnchor) {
                        EdgeAnchor.LEFT -> dx <= -toggleThresholdPx
                        EdgeAnchor.RIGHT -> dx >= toggleThresholdPx
                        null -> false
                    }

                    if (startAnchor != null && shouldHideOnEdge) {
                        applyAnchoredState(view, params, startAnchor, partiallyHidden = true)
                    } else {
                        applyFreeState(view, params)
                    }
                    return true
                }
            }

            MotionEvent.ACTION_UP -> {
                if (locked) {
                    scheduleIdleFade()
                    return false
                }

                if (moved) {
                    val maxX = maxXForButton(view, params)
                    val currentAnchor = edgeAnchorForX(params.x, maxX)
                    if (isFloatingButtonPartiallyHidden && currentAnchor != null) {
                        applyAnchoredState(view, params, currentAnchor, partiallyHidden = true)
                    } else {
                        snapToNearestEdge(view, params)
                    }
                    appPreferences.floatingButtonX = params.x
                    appPreferences.floatingButtonY = params.y
                    scheduleIdleFade()
                    return true
                }
                val currentAnchor = edgeAnchorForX(params.x, maxXForButton(view, params))
                if (currentAnchor != null) {
                    applyAnchoredState(view, params, currentAnchor, isFloatingButtonPartiallyHidden)
                }
                scheduleIdleFade()
            }

            MotionEvent.ACTION_CANCEL -> {
                val currentAnchor = edgeAnchorForX(params.x, maxXForButton(view, params))
                if (currentAnchor != null) {
                    applyAnchoredState(view, params, currentAnchor, isFloatingButtonPartiallyHidden)
                }
                scheduleIdleFade()
            }
        }

        return false
    }

    override fun onDestroy() {
        removeFloatingButton()
        super.onDestroy()
    }

    companion object {
        private const val ACTIVE_ALPHA = 1f
        private const val IDLE_ALPHA = 0.28f
        private const val IDLE_DOCKED_ALPHA = 0.18f
        private const val DOCK_VISIBLE_RATIO = 0.5f
        private const val EDGE_TOGGLE_THRESHOLD_DP = 12f
        private const val DOCK_THRESHOLD_DP = 12f
        private const val MIN_VISIBLE_WIDTH_DP = 24f
        private const val IDLE_FADE_DELAY_MS = 1800L
        private const val ALPHA_ANIMATION_DURATION_MS = 220L
        private const val CHANNEL_ID = "keyboard_switcher_channel"
        private const val NOTIFICATION_ID = 45
        private const val NOTIFICATION_DELAY_MS = 250L
        private const val REQUEST_CODE_NOTIFICATION_LAUNCH = 201

        private const val ACTION_START_NOTIFICATION =
            "com.buertang.keyboardchanger.action.START_NOTIFICATION"
        private const val ACTION_REFRESH_LANGUAGE =
            "com.buertang.keyboardchanger.action.REFRESH_LANGUAGE"
        private const val ACTION_STOP_NOTIFICATION =
            "com.buertang.keyboardchanger.action.STOP_NOTIFICATION"
        private const val ACTION_START_OVERLAY =
            "com.buertang.keyboardchanger.action.START_OVERLAY"
        private const val ACTION_STOP_OVERLAY =
            "com.buertang.keyboardchanger.action.STOP_OVERLAY"

        private const val ACTION_UPDATE_OVERLAY_SIZE =
            "com.buertang.keyboardchanger.action.UPDATE_OVERLAY_SIZE"

        fun startNotification(context: Context) {
            val intent = Intent(context, KeyboardSwitcherService::class.java).apply {
                action = ACTION_START_NOTIFICATION
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stopNotification(context: Context) {
            val intent = Intent(context, KeyboardSwitcherService::class.java).apply {
                action = ACTION_STOP_NOTIFICATION
            }
            context.startService(intent)
        }

        fun refreshLanguage(context: Context) {
            val intent = Intent(context, KeyboardSwitcherService::class.java).apply {
                action = ACTION_REFRESH_LANGUAGE
            }
            context.startService(intent)
        }

        fun startOverlay(context: Context) {
            val intent = Intent(context, KeyboardSwitcherService::class.java).apply {
                action = ACTION_START_OVERLAY
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stopOverlay(context: Context) {
            val intent = Intent(context, KeyboardSwitcherService::class.java).apply {
                action = ACTION_STOP_OVERLAY
            }
            context.startService(intent)
        }

        fun updateOverlaySize(context: Context) {
            val intent = Intent(context, KeyboardSwitcherService::class.java).apply {
                action = ACTION_UPDATE_OVERLAY_SIZE
            }
            context.startService(intent)
        }
    }
}
