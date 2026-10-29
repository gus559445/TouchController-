package top.fifthlight.combine.widget.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import top.fifthlight.combine.input.InteractionSource
import top.fifthlight.combine.input.MutableInteractionSource
import top.fifthlight.combine.layout.Alignment
import top.fifthlight.combine.layout.Arrangement
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.focus.focusable
import top.fifthlight.combine.modifier.pointer.clickable
import top.fifthlight.combine.modifier.pointer.toggleable
import top.fifthlight.combine.sound.LocalSoundManager
import top.fifthlight.combine.sound.SoundKind
import top.fifthlight.combine.sound.SoundManager
import top.fifthlight.combine.ui.style.DrawableSet
import top.fifthlight.combine.widget.base.layout.Row
import top.fifthlight.combine.widget.base.layout.RowScope
import top.fifthlight.touchcontroller.assets.Textures

data class CheckBoxDrawableSet(
    val unchecked: DrawableSet,
    val checked: DrawableSet,
)

val defaultCheckBoxDrawableSet = CheckBoxDrawableSet(
    unchecked = DrawableSet(
        normal = Textures.WIDGET_CHECKBOX_CHECKBOX,
        focus = Textures.WIDGET_CHECKBOX_CHECKBOX_HOVER,
        hover = Textures.WIDGET_CHECKBOX_CHECKBOX_HOVER,
        active = Textures.WIDGET_CHECKBOX_CHECKBOX_ACTIVE,
        disabled = Textures.WIDGET_CHECKBOX_CHECKBOX,
    ),
    checked = DrawableSet(
        normal = Textures.WIDGET_CHECKBOX_CHECKBOX_CHECKED,
        focus = Textures.WIDGET_CHECKBOX_CHECKBOX_CHECKED_HOVER,
        hover = Textures.WIDGET_CHECKBOX_CHECKBOX_CHECKED_HOVER,
        active = Textures.WIDGET_CHECKBOX_CHECKBOX_CHECKED_ACTIVE,
        disabled = Textures.WIDGET_CHECKBOX_CHECKBOX_CHECKED,
    )
)

val LocalCheckBoxDrawableSet = staticCompositionLocalOf<CheckBoxDrawableSet> { defaultCheckBoxDrawableSet }

@Composable
fun CheckBoxIcon(
    modifier: Modifier = Modifier,
    interactionSource: InteractionSource,
    drawableSet: CheckBoxDrawableSet = LocalCheckBoxDrawableSet.current,
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
fun CheckBoxItem(
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    value: Boolean,
    onValueChanged: (Boolean) -> Unit,
    clickSound: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val soundManager: SoundManager = LocalSoundManager.current
    Row(
        modifier = Modifier.toggleable(
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
        CheckBoxIcon(
            interactionSource = interactionSource,
            value = value,
        )
        content()
    }
}

@Composable
fun CheckBox(
    modifier: Modifier = Modifier,
    drawableSet: CheckBoxDrawableSet = LocalCheckBoxDrawableSet.current,
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

    CheckBoxIcon(
        modifier = modifier,
        interactionSource = interactionSource,
        drawableSet = drawableSet,
        value = value,
    )
}