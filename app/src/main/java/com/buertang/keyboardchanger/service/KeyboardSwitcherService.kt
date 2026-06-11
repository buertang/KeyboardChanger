package com.buertang.keyboardchanger.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
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
import kotlin.math.roundToInt

class KeyboardSwitcherService : Service(), View.OnTouchListener {
    private enum class EdgeAnchor(val storageValue: String) {
        LEFT("LEFT"),
        RIGHT("RIGHT");

        companion object {
            fun fromStorageValue(value: String?): EdgeAnchor? {
                return values().firstOrNull { it.storageValue == value }
            }
        }
    }

    private lateinit var windowManager: WindowManager
    private lateinit var displayManager: DisplayManager
    private lateinit var appPreferences: AppPreferences

    private var floatingButton: FrameLayout? = null
    private var floatingIcon: ImageView? = null
    private var floatingLayoutParams: WindowManager.LayoutParams? = null

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var moved = false
    private var currentEdgeAnchor: EdgeAnchor? = null
    private var dragStartAnchor: EdgeAnchor? = null
    private var isFloatingButtonIdle = false
    private var isFloatingButtonPartiallyHidden = false

    private val idleFadeRunnable = Runnable {
        val button = floatingButton ?: return@Runnable
        isFloatingButtonIdle = true
        applyFloatingButtonAlpha(button, animate = true)
    }

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    private val overlayRelayoutRunnable = Runnable {
        repositionFloatingButton()
    }

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit

        override fun onDisplayRemoved(displayId: Int) = Unit

        override fun onDisplayChanged(displayId: Int) {
            scheduleOverlayRelayout()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        appPreferences = AppPreferences.from(this)
        displayManager.registerDisplayListener(displayListener, mainHandler)
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
            .setSmallIcon(R.mipmap.ic_launcher)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
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
        button.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                Color.rgb(255, 250, 236),
                Color.rgb(255, 241, 207)
            )
        ).apply {
            shape = GradientDrawable.OVAL
            setStroke(dpToPx(1f), Color.argb(40, 24, 35, 44))
        }

        val iconSizePx = calculateFloatingIconSizePx(sizePx)
        val iconLayoutParams = FrameLayout.LayoutParams(iconSizePx, iconSizePx, Gravity.CENTER)
        icon.layoutParams = iconLayoutParams
        icon.setImageResource(R.drawable.ic_launcher_foreground)
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
        val restoredAnchor = restoredEdgeAnchor(sizePx)

        floatingLayoutParams = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            y = restoredFloatingButtonY(sizePx)
            if (restoredAnchor != null) {
                gravity = gravityForAnchor(restoredAnchor)
                x = 0
            } else {
                gravity = Gravity.TOP or Gravity.START
                x = appPreferences.floatingButtonX
            }
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
                if (restoredAnchor != null) {
                    applyAnchoredState(button, params, restoredAnchor, partiallyHidden = false)
                    persistFloatingButtonPosition(params, restoredAnchor)
                } else {
                    applyFreeState(button, params)
                    persistFloatingButtonPosition(params, anchor = null)
                }
            }
        }
        scheduleIdleFade()
    }

    private fun updateFloatingButtonSize() {
        val button = floatingButton ?: return
        val icon = floatingIcon ?: return
        val params = floatingLayoutParams ?: return
        val currentButtonHeight = button.height.takeIf { it > 0 } ?: params.height
        val yRatio = yRatioForButton(params.y, currentButtonHeight)

        val sizePx = calculateFloatingButtonSizePx()
        params.width = sizePx
        params.height = sizePx
        params.y = yPositionForButton(sizePx, yRatio, params.y)
        applyFloatingButtonAppearance(button, icon, sizePx)
        val anchor = currentEdgeAnchor
        if (anchor != null) {
            applyAnchoredState(button, params, anchor, isFloatingButtonPartiallyHidden)
        } else {
            applyFreeState(button, params)
        }
        persistFloatingButtonPosition(params, currentEdgeAnchor)
        scheduleIdleFade()
    }

    private fun removeFloatingButton() {
        cancelIdleFade()
        cancelOverlayRelayout()
        floatingButton?.let {
            windowManager.removeView(it)
        }
        floatingButton = null
        floatingIcon = null
        floatingLayoutParams = null
        currentEdgeAnchor = null
        dragStartAnchor = null
        isFloatingButtonPartiallyHidden = false
    }

    private fun isFloatingButtonVisible(): Boolean = floatingButton != null

    private fun restoredEdgeAnchor(buttonSizePx: Int): EdgeAnchor? {
        val storedAnchor = EdgeAnchor.fromStorageValue(appPreferences.floatingButtonAnchor)
        if (storedAnchor != null) {
            return storedAnchor
        }
        val maxX = (availableScreenWidthPx() - buttonSizePx).coerceAtLeast(0)
        return edgeAnchorForX(appPreferences.floatingButtonX, maxX)
    }

    private fun restoredFloatingButtonY(buttonHeightPx: Int): Int {
        return yPositionForButton(
            buttonHeightPx,
            appPreferences.floatingButtonYRatio,
            appPreferences.floatingButtonY
        )
    }

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
        val anchor = if (centerX < availableScreenWidthPx() / 2) {
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
        params.gravity = gravityForAnchor(anchor)
        params.x = 0
        params.y = params.y.coerceIn(0, maxYForButton(view, params))
        currentEdgeAnchor = anchor
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
        params.gravity = Gravity.TOP or Gravity.START
        params.x = params.x.coerceIn(0, maxXForButton(view, params))
        params.y = params.y.coerceIn(0, maxYForButton(view, params))
        currentEdgeAnchor = null
        isFloatingButtonPartiallyHidden = false
        view.translationX = 0f
        windowManager.updateViewLayout(view, params)
        applyFloatingButtonAlpha(view, animate = false)
    }

    private fun prepareForDragging(view: View, params: WindowManager.LayoutParams) {
        val absoluteX = currentAbsoluteX(view, params)
        params.gravity = Gravity.TOP or Gravity.START
        params.x = absoluteX.coerceIn(0, maxXForButton(view, params))
        params.y = params.y.coerceIn(0, maxYForButton(view, params))
        view.translationX = 0f
        windowManager.updateViewLayout(view, params)
    }

    private fun currentAbsoluteX(view: View, params: WindowManager.LayoutParams): Int {
        return when (currentEdgeAnchor) {
            EdgeAnchor.LEFT -> 0
            EdgeAnchor.RIGHT -> maxXForButton(view, params)
            null -> params.x
        }
    }

    private fun gravityForAnchor(anchor: EdgeAnchor): Int {
        return Gravity.TOP or when (anchor) {
            EdgeAnchor.LEFT -> Gravity.START
            EdgeAnchor.RIGHT -> Gravity.END
        }
    }

    private fun persistFloatingButtonPosition(
        params: WindowManager.LayoutParams,
        anchor: EdgeAnchor?
    ) {
        appPreferences.floatingButtonY = params.y
        appPreferences.floatingButtonYRatio = yRatioForButton(params.y, params.height)
        appPreferences.floatingButtonAnchor = anchor?.storageValue
        if (anchor == null) {
            appPreferences.floatingButtonX = params.x
        } else {
            appPreferences.floatingButtonX = 0
        }
    }

    private fun maxXForButton(view: View, params: WindowManager.LayoutParams): Int {
        val buttonWidth = view.width.takeIf { it > 0 } ?: params.width
        return (availableScreenWidthPx() - buttonWidth).coerceAtLeast(0)
    }

    private fun maxYForButton(view: View, params: WindowManager.LayoutParams): Int {
        val buttonHeight = view.height.takeIf { it > 0 } ?: params.height
        return maxYForButtonHeight(buttonHeight)
    }

    private fun maxYForButtonHeight(buttonHeight: Int): Int {
        return (availableScreenHeightPx() - buttonHeight).coerceAtLeast(0)
    }

    private fun availableScreenWidthPx(): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = windowManager.currentWindowMetrics
            val insets = metrics.windowInsets.getInsetsIgnoringVisibility(
                WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
            )
            return (metrics.bounds.width() - insets.left - insets.right).coerceAtLeast(0)
        }
        return resources.displayMetrics.widthPixels
    }

    private fun availableScreenHeightPx(): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = windowManager.currentWindowMetrics
            val insets = metrics.windowInsets.getInsetsIgnoringVisibility(
                WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
            )
            return (metrics.bounds.height() - insets.top - insets.bottom).coerceAtLeast(0)
        }
        return resources.displayMetrics.heightPixels
    }

    private fun yRatioForButton(y: Int, buttonHeight: Int): Float {
        val maxY = maxYForButtonHeight(buttonHeight)
        if (maxY <= 0) {
            return 0f
        }
        return (y.coerceIn(0, maxY).toFloat() / maxY.toFloat()).coerceIn(0f, 1f)
    }

    private fun yPositionForButton(buttonHeight: Int, yRatio: Float?, fallbackY: Int): Int {
        val maxY = maxYForButtonHeight(buttonHeight)
        if (maxY <= 0) {
            return 0
        }
        return if (yRatio != null) {
            (maxY * yRatio.coerceIn(0f, 1f)).roundToInt()
        } else {
            fallbackY.coerceIn(0, maxY)
        }
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

    private fun cancelOverlayRelayout() {
        floatingButton?.removeCallbacks(overlayRelayoutRunnable)
    }

    private fun scheduleIdleFade() {
        val button = floatingButton ?: return
        cancelIdleFade()
        isFloatingButtonIdle = false
        applyFloatingButtonAlpha(button, animate = true)
        button.postDelayed(idleFadeRunnable, IDLE_FADE_DELAY_MS)
    }

    private fun scheduleOverlayRelayout() {
        val button = floatingButton ?: return
        cancelOverlayRelayout()
        button.postOnAnimation(overlayRelayoutRunnable)
    }

    private fun repositionFloatingButton() {
        val button = floatingButton ?: return
        val params = floatingLayoutParams ?: return
        val buttonHeight = button.height.takeIf { it > 0 } ?: params.height
        params.y = restoredFloatingButtonY(buttonHeight)
        val anchor = currentEdgeAnchor
        if (anchor != null) {
            applyAnchoredState(button, params, anchor, isFloatingButtonPartiallyHidden)
        } else {
            applyFreeState(button, params)
        }
    }

    private fun dpToPx(value: Float): Int {
        return (value * resources.displayMetrics.density).toInt().coerceAtLeast(1)
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
                dragStartAnchor = currentEdgeAnchor
                if (dragStartAnchor != null) {
                    prepareForDragging(view, params)
                }
                initialX = currentAbsoluteX(view, params)
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
                    val startAnchor = dragStartAnchor
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
                    val currentAnchor = currentEdgeAnchor
                        ?: edgeAnchorForX(params.x, maxXForButton(view, params))
                    if (isFloatingButtonPartiallyHidden && currentAnchor != null) {
                        applyAnchoredState(view, params, currentAnchor, partiallyHidden = true)
                    } else {
                        snapToNearestEdge(view, params)
                    }
                    persistFloatingButtonPosition(params, currentEdgeAnchor)
                    dragStartAnchor = null
                    scheduleIdleFade()
                    return true
                }
                val currentAnchor = dragStartAnchor
                if (currentAnchor != null) {
                    applyAnchoredState(view, params, currentAnchor, isFloatingButtonPartiallyHidden)
                }
                persistFloatingButtonPosition(params, currentEdgeAnchor)
                dragStartAnchor = null
                scheduleIdleFade()
            }

            MotionEvent.ACTION_CANCEL -> {
                val currentAnchor = dragStartAnchor
                if (currentAnchor != null) {
                    applyAnchoredState(view, params, currentAnchor, isFloatingButtonPartiallyHidden)
                }
                persistFloatingButtonPosition(params, currentEdgeAnchor)
                dragStartAnchor = null
                scheduleIdleFade()
            }
        }

        return false
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        scheduleOverlayRelayout()
    }

    override fun onDestroy() {
        displayManager.unregisterDisplayListener(displayListener)
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
