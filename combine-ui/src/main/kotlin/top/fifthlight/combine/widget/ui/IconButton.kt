package top.fifthlight.combine.widget.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonSkippableComposable
import androidx.compose.runtime.staticCompositionLocalOf
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.ui.style.ColorTheme
import top.fifthlight.combine.ui.style.DrawableSet
import top.fifthlight.combine.widget.base.layout.BoxScope
import top.fifthlight.data.IntPadding
import top.fifthlight.data.IntSize
import top.fifthlight.touchcontroller.assets.Textures

val defaultIconButtonDrawable = DrawableSet(
    normal = Textures.WIDGET_ICON_BUTTON_ICON_BUTTON,
    focus = Textures.WIDGET_ICON_BUTTON_ICON_BUTTON_HOVER,
    hover = Textures.WIDGET_ICON_BUTTON_ICON_BUTTON_HOVER,
    active = Textures.WIDGET_ICON_BUTTON_ICON_BUTTON_ACTIVE,
    disabled = Textures.WIDGET_ICON_BUTTON_ICON_BUTTON_DISABLED,
)

val defaultSelectedIconButtonDrawable = DrawableSet(
    normal = Textures.WIDGET_ICON_BUTTON_ICON_BUTTON_PRESSLOCK,
    focus = Textures.WIDGET_ICON_BUTTON_ICON_BUTTON_PRESSLOCK_HOVER,
    hover = Textures.WIDGET_ICON_BUTTON_ICON_BUTTON_PRESSLOCK_HOVER,
    active = Textures.WIDGET_ICON_BUTTON_ICON_BUTTON_PRESSLOCK_ACTIVE,
    disabled = Textures.WIDGET_ICON_BUTTON_ICON_BUTTON_DISABLED,
)

val LocalIconButtonDrawable = staticCompositionLocalOf { defaultIconButtonDrawable }
val LocalSelectedIconButtonDrawable = staticCompositionLocalOf { defaultSelectedIconButtonDrawable }

@NonSkippableComposable
@Composable
fun IconButton(
    modifier: Modifier = Modifier,
    focusable: Boolean = true,
    selected: Boolean = false,
    drawableSet: DrawableSet = if (selected) {
        LocalSelectedIconButtonDrawable.current
    } else {
        LocalIconButtonDrawable.current
    },
    colorTheme: ColorTheme? = ColorTheme.dark,
    minSize: IntSize = IntSize(0, 0),
    padding: IntPadding = IntPadding(1),
    enabled: Boolean = true,
    onClick: () -> Unit,
    clickSound: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) = Button(
    modifier = modifier,
    focusable = focusable,
    drawableSet = drawableSet,
    colorTheme = colorTheme,
    minSize = minSize,
    padding = padding,
    enabled = enabled,
    onClick = onClick,
    clickSound = clickSound,
    content = content
)
