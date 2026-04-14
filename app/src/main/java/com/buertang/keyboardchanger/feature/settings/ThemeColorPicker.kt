package com.buertang.keyboardchanger.feature.settings

import android.graphics.Color as AndroidColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.buertang.keyboardchanger.R
import com.buertang.keyboardchanger.ui.theme.DEFAULT_THEME_COLOR_RGB
import com.buertang.keyboardchanger.ui.theme.themeColorFromRgb
import com.buertang.keyboardchanger.ui.theme.themeColorHex

@Composable
fun ThemeColorRow(
    title: String,
    colorRgb: Int,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = themeColorHex(colorRgb),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.88f)
            )

            Box(
                modifier = Modifier
                    .size(width = 28.dp, height = 20.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(themeColorFromRgb(colorRgb))
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
fun ThemeColorPickerDialog(
    initialColorRgb: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val initialHsv = remember(initialColorRgb) { extractHsv(initialColorRgb) }

    var selectedColorRgb by remember(initialColorRgb) {
        mutableIntStateOf(initialColorRgb)
    }
    var hexInput by remember(initialColorRgb) {
        mutableStateOf(themeColorHex(initialColorRgb).removePrefix("#"))
    }
    var redInput by remember(initialColorRgb) {
        mutableStateOf(((initialColorRgb shr 16) and 0xFF).toString())
    }
    var greenInput by remember(initialColorRgb) {
        mutableStateOf(((initialColorRgb shr 8) and 0xFF).toString())
    }
    var blueInput by remember(initialColorRgb) {
        mutableStateOf((initialColorRgb and 0xFF).toString())
    }
    var hue by remember(initialColorRgb) {
        mutableFloatStateOf(initialHsv[0])
    }
    var saturation by remember(initialColorRgb) {
        mutableFloatStateOf(initialHsv[1])
    }
    var value by remember(initialColorRgb) {
        mutableFloatStateOf(initialHsv[2])
    }

    fun syncInputs(rgb: Int) {
        selectedColorRgb = rgb and 0xFFFFFF
        hexInput = themeColorHex(selectedColorRgb).removePrefix("#")
        redInput = ((selectedColorRgb shr 16) and 0xFF).toString()
        greenInput = ((selectedColorRgb shr 8) and 0xFF).toString()
        blueInput = (selectedColorRgb and 0xFF).toString()

        val hsv = extractHsv(selectedColorRgb)
        hue = hsv[0]
        saturation = hsv[1]
        value = hsv[2]
    }

    val parsedHex = parseHexColor(hexInput)
    val hexHasError = hexInput.length == 6 && parsedHex == null

    val parsedRgb = parseRgbColor(redInput, greenInput, blueInput)
    val rgbHasError = listOf(redInput, greenInput, blueInput).all { it.isNotEmpty() } &&
        parsedRgb == null
    val animatedPreviewColor by animateColorAsState(themeColorFromRgb(selectedColorRgb))

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(24.dp)
                ),
            shape = RoundedCornerShape(24.dp),
            color = lerp(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.surfaceVariant,
                0.06f
            ),
            tonalElevation = 0.dp,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 720.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    Text(
                        text = stringResource(R.string.choose_theme_color),
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PreviewHeroCard(
                        color = animatedPreviewColor,
                        colorRgb = selectedColorRgb
                    )

                    SectionCard {
                        FullColorPanel(
                            hue = hue,
                            saturation = saturation,
                            value = value,
                            onHueChange = { newHue ->
                                syncInputs(hsvToRgb(newHue, saturation, value))
                            },
                            onSaturationValueChange = { newSaturation, newValue ->
                                syncInputs(hsvToRgb(hue, newSaturation, newValue))
                            }
                        )
                    }

                    SectionCard {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FieldCaption(text = "HEX")

                                Text(
                                    text = if (hexHasError) {
                                        "(${stringResource(R.string.invalid_hex_color)})"
                                    } else {
                                        "(${stringResource(R.string.hex_color_hint)})"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (hexHasError) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }

                            CompactTextField(
                                value = hexInput,
                                onValueChange = { value ->
                                    val sanitized = value
                                        .removePrefix("#")
                                        .filter { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
                                        .uppercase()
                                        .take(6)
                                    hexInput = sanitized
                                    parseHexColor(sanitized)?.let(::syncInputs)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp),
                                prefix = "#",
                                placeholder = "6E90A9",
                                isError = hexHasError,
                                textStyle = MaterialTheme.typography.bodySmall,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text
                                )
                            )

                            FieldCaption(text = "RGB")

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RgbChannelEditor(
                                    label = stringResource(R.string.red),
                                    textValue = redInput,
                                    onTextValueChange = { input ->
                                        redInput = input.filter(Char::isDigit).take(3)
                                        parseRgbColor(redInput, greenInput, blueInput)?.let(::syncInputs)
                                    },
                                    modifier = Modifier.weight(1f)
                                )

                                RgbChannelEditor(
                                    label = stringResource(R.string.green),
                                    textValue = greenInput,
                                    onTextValueChange = { input ->
                                        greenInput = input.filter(Char::isDigit).take(3)
                                        parseRgbColor(redInput, greenInput, blueInput)?.let(::syncInputs)
                                    },
                                    modifier = Modifier.weight(1f)
                                )

                                RgbChannelEditor(
                                    label = stringResource(R.string.blue),
                                    textValue = blueInput,
                                    onTextValueChange = { input ->
                                        blueInput = input.filter(Char::isDigit).take(3)
                                        parseRgbColor(redInput, greenInput, blueInput)?.let(::syncInputs)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    if (rgbHasError) {
                        Text(
                            text = stringResource(R.string.invalid_rgb_color),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = lerp(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant,
                        0.06f
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = themeColorHex(selectedColorRgb),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            QuietTextAction(
                                text = stringResource(R.string.restore_default_theme_color),
                                enabled = selectedColorRgb != DEFAULT_THEME_COLOR_RGB,
                                onClick = {
                                    syncInputs(DEFAULT_THEME_COLOR_RGB)
                                }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            QuietDialogButton(
                                text = stringResource(R.string.cancel),
                                modifier = Modifier.weight(1f),
                                onClick = onDismiss
                            )

                            QuietDialogButton(
                                text = stringResource(R.string.apply),
                                modifier = Modifier.weight(1f),
                                backgroundColor = animatedPreviewColor.copy(alpha = 0.16f),
                                contentColor = animatedPreviewColor,
                                borderColor = animatedPreviewColor.copy(alpha = 0.18f),
                                onClick = { onConfirm(selectedColorRgb) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuietTextAction(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 2.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (enabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f)
            }
        )
    }
}

@Composable
private fun QuietDialogButton(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color? = null,
    contentColor: Color? = null,
    borderColor: Color? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val resolvedBackgroundColor = backgroundColor ?: lerp(
        MaterialTheme.colorScheme.surface,
        MaterialTheme.colorScheme.surfaceVariant,
        0.18f
    )
    val resolvedContentColor = contentColor ?: MaterialTheme.colorScheme.onSurface
    val resolvedBorderColor = borderColor
        ?: MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(resolvedBackgroundColor)
            .border(
                width = 1.dp,
                color = resolvedBorderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = resolvedContentColor
        )
    }
}

@Composable
private fun PreviewHeroCard(
    color: Color,
    colorRgb: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = lerp(
            MaterialTheme.colorScheme.surface,
            color.copy(alpha = 0.10f),
            0.65f
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
        ),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = themeColorHex(colorRgb),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "RGB ${rgbLabel(colorRgb)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .size(width = 40.dp, height = 28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun SectionCard(
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
        ),
        tonalElevation = 0.dp,
        color = lerp(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surfaceVariant,
            0.16f
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun FullColorPanel(
    hue: Float,
    saturation: Float,
    value: Float,
    onHueChange: (Float) -> Unit,
    onSaturationValueChange: (Float, Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        var paletteSize by remember { mutableStateOf(IntSize.Zero) }
        val hueColor = hsvToComposeColor(hue, 1f, 1f)
        val selectedColor = hsvToComposeColor(hue, saturation, value)

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(18.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(18.dp)
                )
                .onSizeChanged { paletteSize = it }
                .pointerInput(hue) {
                    detectTapGestures { offset ->
                        updatePaletteSelection(
                            offset = offset,
                            size = paletteSize,
                            onSaturationValueChange = onSaturationValueChange
                        )
                    }
                }
                .pointerInput(hue) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            updatePaletteSelection(
                                offset = offset,
                                size = paletteSize,
                                onSaturationValueChange = onSaturationValueChange
                            )
                        },
                        onDrag = { change, _ ->
                            updatePaletteSelection(
                                offset = change.position,
                                size = paletteSize,
                                onSaturationValueChange = onSaturationValueChange
                            )
                        }
                    )
                }
        ) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.White, hueColor)
                )
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black)
                )
            )

            val selectorCenter = Offset(
                x = saturation * size.width,
                y = (1f - value) * size.height
            )
            drawSelector(
                center = selectorCenter,
                fillColor = selectedColor
            )
        }

        var hueBarSize by remember { mutableStateOf(IntSize.Zero) }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(14.dp)
                )
                .onSizeChanged { hueBarSize = it }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        updateHueSelection(
                            offset = offset,
                            size = hueBarSize,
                            onHueChange = onHueChange
                        )
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            updateHueSelection(
                                offset = offset,
                                size = hueBarSize,
                                onHueChange = onHueChange
                            )
                        },
                        onDrag = { change, _ ->
                            updateHueSelection(
                                offset = change.position,
                                size = hueBarSize,
                                onHueChange = onHueChange
                            )
                        }
                    )
                }
        ) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Red,
                        Color.Yellow,
                        Color.Green,
                        Color.Cyan,
                        Color.Blue,
                        Color.Magenta,
                        Color.Red
                    )
                )
            )

            val selectorCenter = Offset(
                x = (hue / 360f) * size.width,
                y = size.height / 2f
            )
            drawSelector(
                center = selectorCenter,
                fillColor = hueColor,
                radiusDp = 9f
            )
        }
    }
}

@Composable
private fun RgbChannelEditor(
    label: String,
    textValue: String,
    onTextValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        RgbInputField(
            value = textValue,
            onValueChange = onTextValueChange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun RgbInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    CompactTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.height(38.dp),
        placeholder = "0",
        textStyle = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.Center),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number
        )
    )
}

@Composable
private fun CompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    prefix: String? = null,
    isError: Boolean = false,
    textStyle: TextStyle = MaterialTheme.typography.bodySmall,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    val borderColor = if (isError) {
        MaterialTheme.colorScheme.error.copy(alpha = 0.42f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
    }
    val containerColor = lerp(
        MaterialTheme.colorScheme.background,
        MaterialTheme.colorScheme.surfaceVariant,
        0.16f
    )
    val contentColor = MaterialTheme.colorScheme.onSurface
    val placeholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = true,
        textStyle = textStyle.copy(color = contentColor),
        keyboardOptions = keyboardOptions,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(containerColor)
                    .border(
                        width = 1.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (prefix != null) {
                    Text(
                        text = prefix,
                        style = textStyle,
                        color = placeholderColor
                    )
                }

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = when (textStyle.textAlign) {
                        TextAlign.Center -> Alignment.Center
                        TextAlign.End -> Alignment.CenterEnd
                        else -> Alignment.CenterStart
                    }
                ) {
                    if (value.isEmpty() && placeholder != null) {
                        Text(
                            text = placeholder,
                            style = textStyle,
                            color = placeholderColor
                        )
                    }
                    innerTextField()
                }
            }
        }
    )
}

@Composable
private fun FieldCaption(
    text: String
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.74f)
    )
}

@Composable
private fun ColorSwatch(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    showBorder: Boolean = true,
    size: androidx.compose.ui.unit.Dp = 44.dp
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
            .then(
                if (showBorder) {
                    Modifier.border(
                        width = if (selected) 3.dp else 1.dp,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        },
                        shape = CircleShape
                    )
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    )
}

private fun parseHexColor(input: String): Int? {
    if (input.length != 6) return null
    return input.toIntOrNull(16)
}

private fun parseRgbColor(red: String, green: String, blue: String): Int? {
    val redValue = red.toIntOrNull()
    val greenValue = green.toIntOrNull()
    val blueValue = blue.toIntOrNull()

    if (redValue == null || greenValue == null || blueValue == null) {
        return null
    }

    if (redValue !in 0..255 || greenValue !in 0..255 || blueValue !in 0..255) {
        return null
    }

    return rgbFromChannels(redValue, greenValue, blueValue)
}

private fun rgbFromChannels(red: Int, green: Int, blue: Int): Int {
    return (red shl 16) or (green shl 8) or blue
}

private fun extractHsv(rgb: Int): FloatArray {
    val hsv = FloatArray(3)
    AndroidColor.RGBToHSV(
        (rgb shr 16) and 0xFF,
        (rgb shr 8) and 0xFF,
        rgb and 0xFF,
        hsv
    )
    return hsv
}

private fun hsvToRgb(hue: Float, saturation: Float, value: Float): Int {
    return AndroidColor.HSVToColor(
        floatArrayOf(
            hue.coerceIn(0f, 360f),
            saturation.coerceIn(0f, 1f),
            value.coerceIn(0f, 1f)
        )
    ) and 0xFFFFFF
}

private fun hsvToComposeColor(hue: Float, saturation: Float, value: Float): Color {
    return themeColorFromRgb(hsvToRgb(hue, saturation, value))
}

private fun updatePaletteSelection(
    offset: Offset,
    size: IntSize,
    onSaturationValueChange: (Float, Float) -> Unit
) {
    if (size.width == 0 || size.height == 0) return

    val saturation = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
    val value = (1f - (offset.y / size.height.toFloat())).coerceIn(0f, 1f)
    onSaturationValueChange(saturation, value)
}

private fun updateHueSelection(
    offset: Offset,
    size: IntSize,
    onHueChange: (Float) -> Unit
) {
    if (size.width == 0) return

    val hue = ((offset.x / size.width.toFloat()).coerceIn(0f, 1f) * 360f)
        .coerceIn(0f, 360f)
    onHueChange(hue)
}

private fun rgbLabel(rgb: Int): String {
    return "${(rgb shr 16) and 0xFF}, ${(rgb shr 8) and 0xFF}, ${rgb and 0xFF}"
}

private fun DrawScope.drawSelector(
    center: Offset,
    fillColor: Color,
    radiusDp: Float = 10f
) {
    drawCircle(
        color = Color.Black.copy(alpha = 0.18f),
        radius = (radiusDp + 5f).dp.toPx(),
        center = center
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.96f),
        radius = radiusDp.dp.toPx(),
        center = center,
        style = Stroke(width = 3.dp.toPx())
    )
    drawCircle(
        color = fillColor,
        radius = (radiusDp - 3.2f).dp.toPx(),
        center = center
    )
    drawCircle(
        color = Color.Black.copy(alpha = 0.20f),
        radius = (radiusDp - 3.2f).dp.toPx(),
        center = center,
        style = Stroke(width = 1.dp.toPx())
    )
}

private fun contentColorFor(color: Color): Color {
    return if (color.luminance() > 0.5f) Color.Black else Color.White
}

