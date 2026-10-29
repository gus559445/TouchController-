package top.fifthlight.combine.widget.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import top.fifthlight.combine.animation.animateFloatAsState
import top.fifthlight.combine.input.MutableInteractionSource
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.coerceIn
import top.fifthlight.combine.modifier.focus.focusable
import top.fifthlight.combine.modifier.pointer.toggleable
import top.fifthlight.combine.sound.LocalSoundManager
import top.fifthlight.combine.sound.SoundKind
import top.fifthlight.combine.sound.SoundManager
import top.fifthlight.combine.ui.style.DrawableSet
import top.fifthlight.combine.ui.style.TextureSet
import top.fifthlight.combine.widget.base.Canvas
import top.fifthlight.data.IntOffset
import top.fifthlight.data.IntRect
import top.fifthlight.data.IntSize
import top.fifthlight.touchcontroller.assets.Textures
import kotlin.math.max
import kotlin.math.roundToInt

data class SwitchDrawableSet(
    val frame: DrawableSet,
    val background: TextureSet,
    val handle: DrawableSet
)

val defaultSwitchDrawable = SwitchDrawableSet(
    frame = DrawableSet(
        normal = Textures.WIDGET_SWITCH_FRAME,
        focus = Textures.WIDGET_SWITCH_FRAME_HOVER,
        hover = Textures.WIDGET_SWITCH_FRAME_HOVER,
        active = Textures.WIDGET_SWITCH_FRAME_ACTIVE,
        disabled = Textures.WIDGET_SWITCH_FRAME_DISABLED,
    ),
    background = TextureSet(
        normal = Textures.WIDGET_SWITCH_SWITCH,
        focus = Textures.WIDGET_SWITCH_SWITCH_HOVER,
        hover = Textures.WIDGET_SWITCH_SWITCH_HOVER,
        active = Textures.WIDGET_SWITCH_SWITCH_ACTIVE,
        disabled = Textures.WIDGET_SWITCH_SWITCH_DISABLED,
    ),
    handle = DrawableSet(
        normal = Textures.WIDGET_HANDLE_HANDLE,
        focus = Textures.WIDGET_HANDLE_HANDLE_HOVER,
        hover = Textures.WIDGET_HANDLE_HANDLE_HOVER,
        active = Textures.WIDGET_HANDLE_HANDLE_ACTIVE,
        disabled = Textures.WIDGET_HANDLE_HANDLE_DISABLED,
    ),
)

val LocalSwitchDrawable = staticCompositionLocalOf<SwitchDrawableSet> { defaultSwitchDrawable }

@Composable
fun Switch(
    modifier: Modifier = Modifier,
    drawableSet: SwitchDrawableSet = LocalSwitchDrawable.current,
    enabled: Boolean = true,
    value: Boolean,
    clickSound: Boolean = true,
    onValueChanged: ((Boolean) -> Unit)?,
) {
    val soundManager: SoundManager = LocalSoundManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val state by widgetState(interactionSource)
    val frameDrawable = drawableSet.frame.getByState(state, enabled = enabled)
    val backgroundTexture = drawableSet.background.getByState(state, enabled = enabled)
    val handleDrawable = drawableSet.handle.getByState(state, enabled = enabled)

    val modifier = if (onValueChanged == null || !enabled) {
        modifier
    } else {
        Modifier
            .toggleable(
                interactionSource,
                value,
            ) {
                if (clickSound) {
                    soundManager.play(SoundKind.BUTTON_PRESS, 1f)
                }
                onValueChanged(it)
            }
            .focusable(interactionSource)
            .then(modifier)
    }

    val size = IntSize(
        width = frameDrawable.size.width,
        height = max(frameDrawable.size.height, handleDrawable.size.height),
    )

    val handleValue by animateFloatAsState(if (value) 1f else 0f)

    Canvas(
        modifier = modifier,
        measurePolicy = { _, constraints -> layout(size.coerceIn(constraints)) {} },
    ) { node ->
        val frameRect = IntRect(
            offset = IntOffset(
                x = 0,
                y = (node.size.height - handleDrawable.size.height) / 2,
            ),
            size = frameDrawable.size
        )
        val backgroundInitialOffsetX = (backgroundTexture.size.width - handleDrawable.size.width) / 2
        val handleMoveWidth = frameDrawable.size.width - handleDrawable.size.width
        val handleMoveOffsetX = (handleMoveWidth * handleValue).roundToInt()
        drawTexture(
            texture = backgroundTexture,
            srcRect = IntRect(
                offset = IntOffset(
                    x = backgroundInitialOffsetX - handleMoveOffsetX,
                    y = (node.size.height - backgroundTexture.size.height) / 2
                ),
                size = IntSize(
                    width = frameRect.size.width,
                    height = backgroundTexture.size.height,
                )
            ),
            dstRect = frameRect.toRect(),
        )
        frameDrawable.run {
            draw(rect = frameRect)
        }
        handleDrawable.run {
            draw(
                IntRect(
                    offset = IntOffset(
                        x = handleMoveOffsetX,
                        y = (node.size.height - handleDrawable.size.height) / 2,
                    ),
                    size = handleDrawable.size,
                )
            )
        }
    }
}