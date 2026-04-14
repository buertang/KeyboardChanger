package com.buertang.keyboardchanger.feature.settings

import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.buertang.keyboardchanger.R
import com.buertang.keyboardchanger.ui.language.AppLanguage
import com.buertang.keyboardchanger.ui.theme.ThemeMode
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.roundToInt

private val SettingsRowMinHeight = 48.dp
private val SettingsRowVerticalPadding = 11.dp

@Composable
fun SettingsScreen(
    launchOnStartup: Boolean,
    notificationEnabled: Boolean,
    themeColorRgb: Int,
    themeMode: ThemeMode,
    appLanguage: AppLanguage,
    floatingButtonEnabled: Boolean,
    floatingButtonLocked: Boolean,
    floatingButtonSize: Int,
    onLaunchOnStartupChanged: (Boolean) -> Unit,
    onNotificationEnabledChanged: (Boolean) -> Unit,
    onThemeColorChanged: (Int) -> Unit,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onAppLanguageChanged: (AppLanguage) -> Unit,
    onFloatingButtonEnabledChanged: (Boolean) -> Unit,
    onFloatingButtonLockedChanged: (Boolean) -> Unit,
    onFloatingButtonSizeChanged: (Int) -> Unit,
    onFloatingButtonSizeChangeFinished: (Int) -> Unit,
    onOpenKeyboardSettings: () -> Unit,
    onChooseKeyboard: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    var showThemeColorDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    val groupColor = lerp(
        MaterialTheme.colorScheme.surface,
        MaterialTheme.colorScheme.surfaceVariant,
        0.07f
    )
    val groupBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.06f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                lerp(
                    MaterialTheme.colorScheme.background,
                    MaterialTheme.colorScheme.surfaceVariant,
                    0.08f
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        SettingsTopBar(title = stringResource(R.string.settings_title))

        Spacer(modifier = Modifier.height(8.dp))

        SectionHeader(
            title = stringResource(R.string.actions),
            actionIconRes = R.drawable.ic_about,
            actionContentDescription = stringResource(R.string.about),
            onActionClick = onNavigateToAbout
        )

        SettingsGroup(
            color = groupColor,
            borderColor = groupBorderColor
        ) {
            ActionRow(
                title = stringResource(R.string.open_keyboard_settings),
                onClick = onOpenKeyboardSettings
            )

            GroupDivider()

            ActionRow(
                title = stringResource(R.string.choose_keyboard),
                onClick = onChooseKeyboard
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        SectionHeader(title = stringResource(R.string.preferences))

        SettingsGroup(
            color = groupColor,
            borderColor = groupBorderColor
        ) {
            SettingSwitchRow(
                title = stringResource(R.string.launch_on_startup),
                checked = launchOnStartup,
                onCheckedChange = onLaunchOnStartupChanged
            )

            GroupDivider()

            SettingSwitchRow(
                title = stringResource(R.string.enable_notification),
                checked = notificationEnabled,
                onCheckedChange = onNotificationEnabledChanged,
                enabled = !floatingButtonEnabled
            )

            GroupDivider()

            ThemeColorRow(
                title = stringResource(R.string.theme_color),
                colorRgb = themeColorRgb,
                onClick = {
                    showThemeColorDialog = true
                }
            )

            GroupDivider()

            SettingThemeModeRow(
                title = stringResource(R.string.theme_mode),
                selectedMode = themeMode,
                onModeSelected = onThemeModeChanged
            )

            GroupDivider()

            SettingValueRow(
                title = stringResource(R.string.app_language),
                value = stringResource(appLanguageLabelRes(appLanguage)),
                onClick = {
                    showLanguageDialog = true
                }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        SectionHeader(title = stringResource(R.string.behavior))

        SettingsGroup(
            color = groupColor,
            borderColor = groupBorderColor
        ) {
            SettingSwitchRow(
                title = stringResource(R.string.enable_floating_button),
                checked = floatingButtonEnabled,
                onCheckedChange = onFloatingButtonEnabledChanged
            )

            if (floatingButtonEnabled) {
                GroupDivider()

                SettingSwitchRow(
                    title = stringResource(R.string.lock_floating_button_position),
                    checked = floatingButtonLocked,
                    onCheckedChange = onFloatingButtonLockedChanged
                )

                GroupDivider()

                SettingSliderRow(
                    title = stringResource(R.string.floating_button_size),
                    value = floatingButtonSize,
                    valueRange = 50f..180f,
                    onValueChange = onFloatingButtonSizeChanged,
                    onValueChangeFinished = onFloatingButtonSizeChangeFinished
                )
            }
        }
    }

    if (showThemeColorDialog) {
        ThemeColorPickerDialog(
            initialColorRgb = themeColorRgb,
            onDismiss = {
                showThemeColorDialog = false
            },
            onConfirm = { colorRgb ->
                onThemeColorChanged(colorRgb)
                showThemeColorDialog = false
            }
        )
    }

    if (showLanguageDialog) {
        AppLanguagePickerDialog(
            selectedLanguage = appLanguage,
            onDismiss = {
                showLanguageDialog = false
            },
            onLanguageSelected = { language ->
                showLanguageDialog = false
                onAppLanguageChanged(language)
            }
        )
    }
}

@Composable
private fun SettingsTopBar(
    title: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 28.dp)
            .padding(horizontal = 2.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionIconRes: Int? = null,
    actionContentDescription: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.88f)
        )

        if (actionIconRes != null && actionContentDescription != null && onActionClick != null) {
            val interactionSource = remember { MutableInteractionSource() }

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onActionClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(actionIconRes),
                    contentDescription = actionContentDescription,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
                )
            }
        }
    }
}

@Composable
private fun SettingsGroup(
    color: Color,
    borderColor: Color,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = color,
        border = BorderStroke(
            width = 1.dp,
            color = borderColor
        ),
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun GroupDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)
    )
}

@Composable
private fun SettingValueRow(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = SettingsRowMinHeight)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(
                horizontal = 16.dp,
                vertical = SettingsRowVerticalPadding
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "›",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
            )
        }
    }
}

@Composable
private fun ActionRow(
    title: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = SettingsRowMinHeight)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(
                horizontal = 16.dp,
                vertical = SettingsRowVerticalPadding
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "›",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.88f)
        )
    }
}

@Composable
private fun SettingThemeModeRow(
    title: String,
    selectedMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = SettingsRowMinHeight)
            .padding(
                horizontal = 16.dp,
                vertical = SettingsRowVerticalPadding
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium
        )

        ThemeModeIconSelector(
            selectedMode = selectedMode,
            onModeSelected = onModeSelected
        )
    }
}

@Composable
private fun ThemeModeIconSelector(
    selectedMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ThemeMode.values().forEach { mode ->
            val selected = selectedMode == mode
            val interactionSource = remember(mode) { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        } else {
                            Color.Transparent
                        },
                        shape = CircleShape
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { onModeSelected(mode) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = themeModeIconRes(mode)),
                    contentDescription = stringResource(id = themeModeLabelRes(mode)),
                    modifier = Modifier.size(21.dp),
                    tint = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f)
                    }
                )
            }
        }
    }
}

private fun themeModeLabelRes(mode: ThemeMode): Int {
    return when (mode) {
        ThemeMode.SYSTEM -> R.string.theme_mode_system
        ThemeMode.LIGHT -> R.string.theme_mode_light
        ThemeMode.DARK -> R.string.theme_mode_dark
    }
}

private fun themeModeIconRes(mode: ThemeMode): Int {
    return when (mode) {
        ThemeMode.SYSTEM -> R.drawable.ic_theme_mode_system
        ThemeMode.LIGHT -> R.drawable.ic_theme_mode_light
        ThemeMode.DARK -> R.drawable.ic_theme_mode_dark
    }
}

@Composable
private fun AppLanguagePickerDialog(
    selectedLanguage: AppLanguage,
    onDismiss: () -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(22.dp)
                ),
            shape = RoundedCornerShape(22.dp),
            color = lerp(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.surfaceVariant,
                0.06f
            ),
            tonalElevation = 0.dp,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.choose_app_language),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)
                )

                AppLanguage.entries.forEachIndexed { index, language ->
                    LanguageOptionRow(
                        language = language,
                        selected = selectedLanguage == language,
                        onClick = {
                            onLanguageSelected(language)
                        }
                    )

                    if (index != AppLanguage.entries.lastIndex) {
                        GroupDivider()
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)
                )

                QuietTextDialogAction(
                    text = stringResource(R.string.cancel),
                    onClick = onDismiss
                )
            }
        }
    }
}

@Composable
private fun LanguageOptionRow(
    language: AppLanguage,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(appLanguageLabelRes(language)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (selected) {
            Text(
                text = "✓",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun QuietTextDialogAction(
    text: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

private fun appLanguageLabelRes(language: AppLanguage): Int {
    return when (language) {          
        AppLanguage.SIMPLIFIED_CHINESE -> R.string.app_language_simplified_chinese
        AppLanguage.TRADITIONAL_CHINESE -> R.string.app_language_traditional_chinese
        AppLanguage.ENGLISH -> R.string.app_language_english
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    supportingText: String? = null
) {
    if (supportingText == null) {
        SwitchRowContent(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = SettingsRowMinHeight)
                .padding(
                    horizontal = 16.dp,
                    vertical = SettingsRowVerticalPadding
                ),
            title = title,
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = SettingsRowVerticalPadding
                ),
        ) {
            SwitchRowContent(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = SettingsRowMinHeight),
                title = title,
                checked = checked,
                enabled = enabled,
                onCheckedChange = onCheckedChange
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun SwitchRowContent(
    modifier: Modifier = Modifier,
    title: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium
        )

        PowerSwitch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun PowerSwitch(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val trackColor = when {
        !enabled && checked -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)
        checked -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        else -> lerp(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceVariant,
            0.34f
        )
    }
    val borderColor = when {
        !enabled && checked -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        !enabled -> MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
        checked -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
    }
    val thumbColor = when {
        !enabled -> MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
        checked -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.74f)
    }
    val iconTint = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        checked -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.surface
    }
    val checkedTransition = updateTransition(
        targetState = checked,
        label = "powerSwitchCheckedTransition"
    )
    // Theme palette changes should snap immediately; only the checked state should move the thumb.
    val thumbOffset by checkedTransition.animateDp(
        label = "powerSwitchThumbOffset"
    ) { isChecked ->
        if (isChecked) 16.dp else 0.dp
    }


    Box(
        modifier = Modifier
            .size(width = 36.dp, height = 20.dp)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(999.dp)
            )
            .background(
                color = trackColor,
                shape = RoundedCornerShape(999.dp)
            )
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = interactionSource,
                indication = null,
                onValueChange = onCheckedChange
            )
            .padding(horizontal = 2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(16.dp)
                .background(
                    color = thumbColor,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_power_switch),
                contentDescription = null,
                modifier = Modifier.size(9.dp),
                tint = iconTint
            )
        }
    }
}

@Composable
private fun SettingSliderRow(
    title: String,
    value: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Int) -> Unit,
    onValueChangeFinished: (Int) -> Unit
) {
    var sliderValue by remember(value) {
        mutableFloatStateOf(value.toFloat())
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = SettingsRowVerticalPadding
            )
    ) {
        Text(
            text = "$title: ${sliderValue.roundToInt()}%",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            value = sliderValue,
            onValueChange = {
                sliderValue = it
                onValueChange(it.roundToInt())
            },
            onValueChangeFinished = {
                onValueChangeFinished(sliderValue.roundToInt())
            },
            valueRange = valueRange
        )
    }
}


