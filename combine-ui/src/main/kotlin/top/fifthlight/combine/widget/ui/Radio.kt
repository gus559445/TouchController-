package top.fifthlight.combine.widget.ui

import androidx.compose.runtime.*
import top.fifthlight.combine.input.InteractionSource
import top.fifthlight.combine.input.MutableInteractionSource
import top.fifthlight.combine.layout.Alignment
import top.fifthlight.combine.layout.Arrangement
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.drawing.border
import top.fifthlight.combine.modifier.focus.focusable
import top.fifthlight.combine.modifier.placement.padding
import top.fifthlight.combine.modifier.pointer.clickable
import top.fifthlight.combine.modifier.pointer.toggleable
import top.fifthlight.combine.sound.LocalSoundManager
import top.fifthlight.combine.sound.SoundKind
import top.fifthlight.combine.sound.SoundManager
import top.fifthlight.combine.ui.style.ColorTheme
import top.fifthlight.combine.ui.style.DrawableSet
import top.fifthlight.combine.ui.style.LocalColorTheme
import top.fifthlight.combine.widget.base.layout.Column
import top.fifthlight.combine.widget.base.layout.ColumnScope
import top.fifthlight.combine.widget.base.layout.Row
import top.fifthlight.combine.widget.base.layout.RowScope
import top.fifthlight.touchcontroller.assets.Textures

data class RadioDrawableSet(
    val unchecked: DrawableSet,
    val checked: DrawableSet,
)

val defaultRadioDrawableSet = RadioDrawableSet(
    unchecked = DrawableSet(
        normal = Textures.WIDGET_RADIO_RADIO,
        focus = Textures.WIDGET_RADIO_RADIO_HOVER,
        hover = Textures.WIDGET_RADIO_RADIO_HOVER,
        active = Textures.WIDGET_RADIO_RADIO_ACTIVE,
        disabled = Textures.WIDGET_RADIO_RADIO,
    ),
    checked = DrawableSet(
        normal = Textures.WIDGET_RADIO_RADIO_CHECKED,
        focus = Textures.WIDGET_RADIO_RADIO_CHECKED_HOVER,
        hover = Textures.WIDGET_RADIO_RADIO_CHECKED_HOVER,
        active = Textures.WIDGET_RADIO_RADIO_CHECKED_ACTIVE,
        disabled = Textures.WIDGET_RADIO_RADIO_CHECKED,
    )
)

val LocalRadioDrawableSet = staticCompositionLocalOf<RadioDrawableSet> { defaultRadioDrawableSet }

@Composable
fun RadioIcon(
    modifier: Modifier = Modifier,
    interactionSource: InteractionSource,
    drawableSet: RadioDrawableSet = LocalRadioDrawableSet.current,
    value: Boolean,
) {
    val currentDrawableSet = if (value) {
        drawableSet.checked
    } else {
        drawableSet.unchecked
    }
    val state by widgetState(interactionSource)
    val drawable = currentDrawableSet.getByState(state)

    Icon(
        modifier = modifier,
        drawable = drawable,
    )
}

@Composable
fun RadioRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .padding(4)
            .border(Textures.WIDGET_BACKGROUND_FLOAT_WINDOW)
            .then(modifier),
        horizontalArrangement = Arrangement.spacedBy(4),
    ) {
        CompositionLocalProvider(
            LocalColorTheme provides ColorTheme.light,
        ) {
            content()
        }
    }
}

@Composable
fun RadioColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .padding(4)
            .border(Textures.WIDGET_BACKGROUND_FLOAT_WINDOW)
            .then(modifier),
        verticalArrangement = Arrangement.spacedBy(4),
    ) {
        CompositionLocalProvider(
            LocalColorTheme provides ColorTheme.light,
        ) {
            content()
        }
    }
}

@Composable
fun RadioBoxItem(
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    value: Boolean,
    onValueChanged: (Boolean) -> Unit,
    clickSound: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val soundManager: SoundManager = LocalSoundManager.current
    Row(
        modifier = modifier.toggleable(
            interactionSource = interactionSource,
            value = value,
            onValueChanged = {
                if (clickSound) {
                    soundManager.play(SoundKind.BUTTON_PRESS, 1f)
                }
                onValueChanged(it)
            },
        ),
        horizontalArrangement = Arrangement.spacedBy(4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioIcon(
            interactionSource = interactionSource,
            value = value,
        )
        content()
    }
}

@Composable
fun Radio(
    modifier: Modifier = Modifier,
    drawableSet: RadioDrawableSet = LocalRadioDrawableSet.current,
    value: Boolean,
    onValueChanged: ((Boolean) -> Unit)?,
) {
    val interactionSource = remember { MutableInteractionSource() }

    val modifier = if (onValueChanged == null) {
        modifier
    } else {
        Modifier
            .clickable(interactionSource) {
                onValueChanged(!value)
            }
            .focusable(interactionSource)
            .then(modifier)
    }

    RadioIcon(
        modifier = modifier,
        interactionSource = interactionSource,
        drawableSet = drawableSet,
        value = value,
    )
}