package top.fifthlight.combine.widget.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import top.fifthlight.combine.layout.Alignment
import top.fifthlight.combine.layout.Arrangement
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.ui.style.ColorTheme
import top.fifthlight.combine.ui.style.DrawableSet
import top.fifthlight.combine.widget.base.layout.Row
import top.fifthlight.combine.widget.base.layout.RowScope
import top.fifthlight.data.IntSize
import top.fifthlight.touchcontroller.assets.Textures

val defaultCheckBoxButtonDrawableSet = DrawableSet(
    normal = Textures.WIDGET_CHECKBOX_CHECKBOX_BUTTON,
    focus = Textures.WIDGET_CHECKBOX_CHECKBOX_BUTTON_HOVER,
    hover = Textures.WIDGET_CHECKBOX_CHECKBOX_BUTTON_HOVER,
    active = Textures.WIDGET_CHECKBOX_CHECKBOX_BUTTON_ACTIVE,
    disabled = Textures.WIDGET_CHECKBOX_CHECKBOX_BUTTON_DISABLED,
)

val LocalCheckBoxButtonDrawable = staticCompositionLocalOf { defaultCheckBoxButtonDrawableSet }

@Composable
fun CheckBoxButton(
    modifier: Modifier = Modifier,
    drawableSet: DrawableSet = LocalCheckBoxButtonDrawable.current,
    checkBoxDrawableSet: CheckBoxDrawableSet = LocalCheckBoxDrawableSet.current,
    colorTheme: ColorTheme? = null,
    minSize: IntSize = IntSize(48, 20),
    enabled: Boolean = true,
    checked: Boolean = false,
    onClick: () -> Unit,
    clickSound: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) = Button(
    modifier = modifier,
    drawableSet = drawableSet,
    colorTheme = colorTheme,
    minSize = minSize,
    enabled = enabled,
    onClick = onClick,
    clickSound = clickSound,
    content = {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4),
            verticalAlignment = Alignment.CenterVertically
        ) {
            content()

            val state = LocalWidgetState.current
            val drawableSet = if (checked) {
                checkBoxDrawableSet.checked
            } else {
                checkBoxDrawableSet.unchecked
            }
            Icon(drawableSet.getByState(state))
        }
    }
)