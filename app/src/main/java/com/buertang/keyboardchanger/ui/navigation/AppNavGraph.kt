package com.buertang.keyboardchanger.ui.navigation

import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.buertang.keyboardchanger.R
import com.buertang.keyboardchanger.core.KeyboardUtils
import com.buertang.keyboardchanger.data.AppPreferences
import com.buertang.keyboardchanger.feature.about.AboutScreen
import com.buertang.keyboardchanger.feature.settings.SettingsScreen
import com.buertang.keyboardchanger.keyboard.KeyboardManagerActivity
import com.buertang.keyboardchanger.service.KeyboardSwitcherService
import com.buertang.keyboardchanger.ui.language.AppLanguage
import com.buertang.keyboardchanger.ui.theme.ThemeMode

private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_ABOUT = "about"
private const val NAV_ANIMATION_DURATION_MS = 140

@Composable
fun AppNavGraph(
    themeColorRgb: Int,
    themeMode: ThemeMode,
    appLanguage: AppLanguage,
    onThemeColorChanged: (Int) -> Unit,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onAppLanguageChanged: (AppLanguage) -> Unit
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val appPreferences = remember { AppPreferences.from(context) }

    val launchOnStartup = remember { mutableStateOf(appPreferences.launchOnStartup) }
    val notificationEnabled = remember { mutableStateOf(appPreferences.notificationEnabled) }
    val floatingButtonEnabled = remember { mutableStateOf(appPreferences.floatingButtonEnabled) }
    val floatingButtonLocked = remember { mutableStateOf(appPreferences.floatingButtonLocked) }
    val floatingButtonSize = remember { mutableStateOf(appPreferences.floatingButtonSize) }

    fun syncNotificationState() {
        notificationEnabled.value =
            appPreferences.notificationEnabled || floatingButtonEnabled.value
    }

    fun syncSettingsState() {
        launchOnStartup.value = appPreferences.launchOnStartup
        floatingButtonLocked.value = appPreferences.floatingButtonLocked
        floatingButtonSize.value = appPreferences.floatingButtonSize


        val overlayAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                Settings.canDrawOverlays(context)

        if (appPreferences.floatingButtonEnabled && overlayAllowed) {
            floatingButtonEnabled.value = true
            KeyboardSwitcherService.startOverlay(context)
        } else {
            floatingButtonEnabled.value = false
            if (!overlayAllowed) {
                appPreferences.floatingButtonEnabled = false
                KeyboardSwitcherService.stopOverlay(context)
            }
        }

        syncNotificationState()

        if (!floatingButtonEnabled.value && notificationEnabled.value) {
            KeyboardSwitcherService.startNotification(context)
        } else if (!floatingButtonEnabled.value) {
            KeyboardSwitcherService.stopNotification(context)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                syncSettingsState()
            }
        }

        syncSettingsState()
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    NavHost(
        navController = navController,
        startDestination = ROUTE_SETTINGS
    ) {
        composable(
            route = ROUTE_SETTINGS,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(NAV_ANIMATION_DURATION_MS)
                )
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(NAV_ANIMATION_DURATION_MS)
                )
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(NAV_ANIMATION_DURATION_MS)
                )
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(NAV_ANIMATION_DURATION_MS)
                )
            }
        ) {
            SettingsScreen(
                launchOnStartup = launchOnStartup.value,
                notificationEnabled = notificationEnabled.value,
                themeColorRgb = themeColorRgb,
                themeMode = themeMode,
                appLanguage = appLanguage,
                floatingButtonEnabled = floatingButtonEnabled.value,
                floatingButtonLocked = floatingButtonLocked.value,
                floatingButtonSize = floatingButtonSize.value,
                onLaunchOnStartupChanged = { value ->
                    launchOnStartup.value = value
                    appPreferences.launchOnStartup = value
                },
                onNotificationEnabledChanged = { value ->
                    if (!floatingButtonEnabled.value) {
                        notificationEnabled.value = value
                        appPreferences.notificationEnabled = value
                        if (value) {
                            KeyboardSwitcherService.startNotification(context)
                        } else {
                            KeyboardSwitcherService.stopNotification(context)
                        }
                    }
                },
                onThemeColorChanged = { value ->
                    onThemeColorChanged(value)
                    if (floatingButtonEnabled.value) {
                        KeyboardSwitcherService.updateOverlaySize(context)
                    }
                },
                onThemeModeChanged = onThemeModeChanged,
                onAppLanguageChanged = onAppLanguageChanged,
                onFloatingButtonEnabledChanged = { value ->
                    if (value) {
                        val overlayAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                                Settings.canDrawOverlays(context)

                        if (overlayAllowed) {
                            floatingButtonEnabled.value = true
                            appPreferences.floatingButtonEnabled = true
                            syncNotificationState()
                            KeyboardSwitcherService.startOverlay(context)
                        } else {
                            floatingButtonEnabled.value = false
                            appPreferences.floatingButtonEnabled = true
                            Toast.makeText(
                                context,
                                context.getString(R.string.overlay_permission_required),
                                Toast.LENGTH_SHORT
                            ).show()
                            KeyboardUtils.openOverlaySettings(context)
                        }
                    } else {
                        floatingButtonEnabled.value = false
                        appPreferences.floatingButtonEnabled = false
                        syncNotificationState()
                        KeyboardSwitcherService.stopOverlay(context)
                    }
                },
                onFloatingButtonLockedChanged = { value ->
                    floatingButtonLocked.value = value
                    appPreferences.floatingButtonLocked = value
                },
                onFloatingButtonSizeChanged = { value ->
                    floatingButtonSize.value = value
                    appPreferences.floatingButtonSize = value
                    if (floatingButtonEnabled.value) {
                        KeyboardSwitcherService.updateOverlaySize(context)
                    }
                },
                onFloatingButtonSizeChangeFinished = { _ ->
                    // no-op
                },
                onOpenKeyboardSettings = {
                    KeyboardUtils.openAvailableKeyboards(context)
                },
                onChooseKeyboard = {
                    context.startActivity(
                        KeyboardManagerActivity.createIntent(context)
                    )
                },
                onNavigateToAbout = {
                    navController.navigate(ROUTE_ABOUT)
                }
            )
        }

        composable(
            route = ROUTE_ABOUT,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(NAV_ANIMATION_DURATION_MS)
                )
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(NAV_ANIMATION_DURATION_MS)
                )
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(NAV_ANIMATION_DURATION_MS)
                )
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(NAV_ANIMATION_DURATION_MS)
                )
            }
        ) {
            AboutScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
