package com.buertang.keyboardchanger.keyboard

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.buertang.keyboardchanger.R
import com.buertang.keyboardchanger.core.KeyboardUtils
import com.buertang.keyboardchanger.data.AppPreferences
import com.buertang.keyboardchanger.ui.language.AppLanguageManager

class KeyboardManagerActivity : AppCompatActivity() {
    private var firstDelayMs = DEFAULT_FIRST_DELAY_MS
    private var dialogState = DialogState.NONE
    private var hasLaunchedPicker = false
    private var hasScheduledPicker = false
    private var removeTaskOnFinish = false

    private lateinit var rootView: View
    private lateinit var fallbackContainer: View
    private lateinit var openPickerRunnable: Runnable
    private lateinit var showFallbackRunnable: Runnable

    private enum class DialogState {
        NONE,
        PICKING,
        CHOSEN
    }

    override fun attachBaseContext(newBase: Context) {
        val appLanguage = AppPreferences.from(newBase).appLanguage
        super.attachBaseContext(AppLanguageManager.wrap(newBase, appLanguage))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_keyboard_manager)

        rootView = findViewById(R.id.root_view)
        fallbackContainer = findViewById(R.id.fallback_container)

        findViewById<View>(R.id.cancel_button).setOnClickListener {
            closeBridgeActivity()
        }
        findViewById<View>(R.id.open_button).setOnClickListener {
            launchKeyboardPicker()
            hideFallback()
            dialogState = DialogState.PICKING
        }

        initLauncher(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        initLauncher(intent)
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        if (::openPickerRunnable.isInitialized) {
            rootView.removeCallbacks(openPickerRunnable)
        }
        if (::showFallbackRunnable.isInitialized) {
            rootView.removeCallbacks(showFallbackRunnable)
        }
        if (!hasLaunchedPicker) {
            hasScheduledPicker = false
        }
        super.onPause()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (!hasFocus && dialogState == DialogState.PICKING) {
            dialogState = DialogState.CHOSEN
        } else if (hasFocus && dialogState == DialogState.CHOSEN) {
            closeBridgeActivity()
        } else if (hasFocus && !hasLaunchedPicker && !hasScheduledPicker) {
            hasScheduledPicker = true
            if (firstDelayMs <= IMMEDIATE_DELAY_MS) {
                rootView.post(openPickerRunnable)
            } else {
                rootView.postDelayed(openPickerRunnable, firstDelayMs)
            }
        }
    }

    private fun initLauncher(intent: Intent?) {
        if (::openPickerRunnable.isInitialized) {
            rootView.removeCallbacks(openPickerRunnable)
        }
        if (::showFallbackRunnable.isInitialized) {
            rootView.removeCallbacks(showFallbackRunnable)
        }

        dialogState = DialogState.NONE
        hasLaunchedPicker = false
        hasScheduledPicker = false
        removeTaskOnFinish = intent?.getBooleanExtra(EXTRA_REMOVE_TASK_ON_FINISH, false) == true
        hideFallback()
        firstDelayMs = intent?.getLongExtra(EXTRA_FIRST_DELAY_MS, DEFAULT_FIRST_DELAY_MS)
            ?: DEFAULT_FIRST_DELAY_MS

        openPickerRunnable = Runnable {
            tryLaunchKeyboardPicker()
        }
        showFallbackRunnable = Runnable {
            if (dialogState == DialogState.PICKING && hasWindowFocus()) {
                showFallback()
            }
        }
    }

    private fun tryLaunchKeyboardPicker() {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_INPUT_METHODS)) {
            Toast.makeText(this, R.string.error_device_no_ime, Toast.LENGTH_SHORT).show()
            closeBridgeActivity()
            return
        }

        if (hasLaunchedPicker) {
            return
        }

        hasLaunchedPicker = true
        launchKeyboardPicker()
        dialogState = DialogState.PICKING
        rootView.postDelayed(showFallbackRunnable, FALLBACK_DELAY_MS)
    }

    private fun launchKeyboardPicker() {
        KeyboardUtils.chooseKeyboard(this)
    }

    private fun showFallback() {
        rootView.visibility = View.VISIBLE
        fallbackContainer.visibility = View.VISIBLE
    }

    private fun hideFallback() {
        fallbackContainer.visibility = View.GONE
        rootView.visibility = View.GONE
    }

    private fun closeBridgeActivity() {
        if (removeTaskOnFinish && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask()
        } else {
            finish()
        }
    }

    companion object {
        private const val EXTRA_FIRST_DELAY_MS = "keyboard_picker_first_delay_ms"
        private const val EXTRA_REMOVE_TASK_ON_FINISH = "keyboard_picker_remove_task_on_finish"
        private const val IMMEDIATE_DELAY_MS = 0L
        private const val DEFAULT_FIRST_DELAY_MS = 150L
        private const val NOTIFICATION_DELAY_MS = 250L
        private const val TILE_DELAY_MS = 300L
        private const val FALLBACK_DELAY_MS = 900L

        private fun createIntent(
            context: Context,
            firstDelayMs: Long,
            clearTask: Boolean,
            removeTaskOnFinish: Boolean
        ): Intent {
            return Intent(context, KeyboardManagerActivity::class.java).apply {
                var flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                if (clearTask) {
                    flags = flags or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                addFlags(flags)
                putExtra(
                    EXTRA_FIRST_DELAY_MS,
                    if (clearTask) firstDelayMs else IMMEDIATE_DELAY_MS
                )
                putExtra(EXTRA_REMOVE_TASK_ON_FINISH, removeTaskOnFinish)
            }
        }

        fun createIntent(context: Context, firstDelayMs: Long = DEFAULT_FIRST_DELAY_MS): Intent {
            return createIntent(
                context = context,
                firstDelayMs = firstDelayMs,
                clearTask = false,
                removeTaskOnFinish = false
            )
        }

        private fun createPendingIntent(
            context: Context,
            firstDelayMs: Long,
            clearTask: Boolean,
            requestCode: Int,
            removeTaskOnFinish: Boolean
        ): PendingIntent {
            return PendingIntent.getActivity(
                context,
                requestCode,
                createIntent(context, firstDelayMs, clearTask, removeTaskOnFinish),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        fun createPendingIntent(context: Context): PendingIntent {
            return createPendingIntent(
                context = context,
                firstDelayMs = DEFAULT_FIRST_DELAY_MS,
                clearTask = false,
                requestCode = REQUEST_CODE_DEFAULT,
                removeTaskOnFinish = false
            )
        }

        fun createNotificationPendingIntent(context: Context): PendingIntent {
            return createPendingIntent(
                context = context,
                firstDelayMs = NOTIFICATION_DELAY_MS,
                clearTask = true,
                requestCode = REQUEST_CODE_NOTIFICATION,
                removeTaskOnFinish = true
            )
        }

        fun createTilePendingIntent(context: Context, external: Boolean): PendingIntent {
            return createPendingIntent(
                context = context,
                firstDelayMs = TILE_DELAY_MS,
                clearTask = external,
                requestCode = REQUEST_CODE_TILE,
                removeTaskOnFinish = external
            )
        }

        fun createTileIntent(context: Context, external: Boolean): Intent {
            return createIntent(
                context = context,
                firstDelayMs = TILE_DELAY_MS,
                clearTask = external,
                removeTaskOnFinish = external
            )
        }

        fun createExternalPendingIntent(
            context: Context,
            firstDelayMs: Long = DEFAULT_FIRST_DELAY_MS
        ): PendingIntent {
            return createPendingIntent(
                context = context,
                firstDelayMs = firstDelayMs,
                clearTask = true,
                requestCode = REQUEST_CODE_EXTERNAL,
                removeTaskOnFinish = true
            )
        }

        fun createExternalIntent(
            context: Context,
            firstDelayMs: Long = DEFAULT_FIRST_DELAY_MS
        ): Intent {
            return createIntent(
                context = context,
                firstDelayMs = firstDelayMs,
                clearTask = true,
                removeTaskOnFinish = true
            )
        }

        private const val REQUEST_CODE_DEFAULT = 100
        private const val REQUEST_CODE_NOTIFICATION = 101
        private const val REQUEST_CODE_TILE = 102
        private const val REQUEST_CODE_EXTERNAL = 103
    }
}
