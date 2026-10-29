package top.fifthlight.combine.widget.ui

import androidx.compose.runtime.*
import top.fifthlight.combine.input.MutableInteractionSource
import top.fifthlight.combine.layout.Alignment
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.drawing.border
import top.fifthlight.combine.modifier.focus.focusable
import top.fifthlight.combine.modifier.placement.minSize
import top.fifthlight.combine.modifier.placement.padding
import top.fifthlight.combine.modifier.pointer.clickable
import top.fifthlight.combine.paint.Colors
import top.fifthlight.combine.sound.LocalSoundManager
import top.fifthlight.combine.sound.SoundKind
import top.fifthlight.combine.ui.style.ColorTheme
import top.fifthlight.combine.ui.style.DrawableSet
import top.fifthlight.combine.ui.style.LocalColorTheme
import top.fifthlight.combine.widget.base.layout.Box
import top.fifthlight.combine.widget.base.layout.BoxScope
import top.fifthlight.data.IntPadding
import top.fifthlight.data.IntSize
import top.fifthlight.touchcontroller.assets.Textures

val defaultButtonDrawable = DrawableSet(
    normal = Textures.WIDGET_BUTTON_BUTTON,
    focus = Textures.WIDGET_BUTTON_BUTTON_HOVER,
    hover = Textures.WIDGET_BUTTON_BUTTON_HOVER,
    active = Textures.WIDGET_BUTTON_BUTTON_ACTIVE,
    disabled = Textures.WIDGET_BUTTON_BUTTON_DISABLED,
)

val guideButtonTexture = defaultButtonDrawable.copy(
    normal = Textures.WIDGET_BUTTON_BUTTON_GUIDE,
    focus = Textures.WIDGET_BUTTON_BUTTON_GUIDE_HOVER,
    hover = Textures.WIDGET_BUTTON_BUTTON_GUIDE_HOVER,
)

val warningButtonTexture = defaultButtonDrawable.copy(
    normal = Textures.WIDGET_BUTTON_BUTTON_WARNING,
    focus = Textures.WIDGET_BUTTON_BUTTON_WARNING_HOVER,
    hover = Textures.WIDGET_BUTTON_BUTTON_WARNING_HOVER,
)

val LocalButtonDrawable = staticCompositionLocalOf { defaultButtonDrawable }
val LocalGuideButtonDrawable = staticCompositionLocalOf { guideButtonTexture }
val LocalWarningButtonDrawable = staticCompositionLocalOf { warningButtonTexture }

@NonSkippableComposable
@Composable
fun GuideButton(
    modifier: Modifier = Modifier,
    focusable: Boolean = true,
    drawableSet: DrawableSet = LocalGuideButtonDrawable.current,
    colorTheme: ColorTheme? = ColorTheme.dark,
    minSize: IntSize = IntSize(48, 20),
    enabled: Boolean = true,
    onClick: () -> Unit,
    clickSound: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    Button(
        modifier = modifier,
        focusable = focusable,
        drawableSet = drawableSet,
        colorTheme = colorTheme,
        minSize = minSize,
        enabled = enabled,
        onClick = onClick,
        clickSound = clickSound,
        content = content
    )
}

@NonSkippableComposable
@Composable
fun WarningButton(
    modifier: Modifier = Modifier,
    focusable: Boolean = true,
    drawableSet: DrawableSet = LocalWarningButtonDrawable.current,
    colorTheme: ColorTheme? = ColorTheme.dark,
    minSize: IntSize = IntSize(48, 20),
    enabled: Boolean = true,
    onClick: () -> Unit,
    clickSound: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    Button(
        modifier = modifier,
        focusable = focusable,
        drawableSet = drawableSet,
        colorTheme = colorTheme,
        minSize = minSize,
        enabled = enabled,
        onClick = onClick,
        clickSound = clickSound,
        content = content
    )
}

@Composable
fun Button(
    modifier: Modifier = Modifier,
    focusable: Boolean = true,
    drawableSet: DrawableSet = LocalButtonDrawable.current,
    colorTheme: ColorTheme? = null,
    minSize: IntSize = IntSize(48, 20),
    padding: IntPadding = IntPadding(left = 4, right = 4),
    enabled: Boolean = true,
    onClick: () -> Unit,
    clickSound: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val soundManager = LocalSoundManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val state by widgetState(interactionSource)
    val drawable = drawableSet.getByState(state, enabled = enabled)

    val clickableModifier = Modifier.clickable(interactionSource) {
        if (clickSound) {
            soundManager.play(SoundKind.BUTTON_PRESS, 1f)
        }
        onClick()
    }
    val focusableModifier = Modifier.focusable(interactionSource)

    fun Modifier.then(modifier: Modifier?) = if (modifier != null) {
        then(modifier)
    } else {
        this
    }

    Box(
        modifier = Modifier
            .padding(padding)
            .border(drawable)
            .minSize(minSize)
            .then(clickableModifier.takeIf { enabled })
            .then(focusableModifier.takeIf { enabled && focusable })
            .then(modifier),
        alignment = Alignment.Center,
    ) {
        var colorTheme = colorTheme ?: ColorTheme.light
        if (!enabled) {
            colorTheme = colorTheme.copy(foreground = Colors.SECONDARY_WHITE)
        }
        CompositionLocalProvider(
            LocalColorTheme provides colorTheme,
            LocalWidgetState provides state,
        ) {
            content()
        }
    }
}
