package top.fifthlight.combine.widget.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import top.fifthlight.combine.input.MutableInteractionSource
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.focus.focusable
import top.fifthlight.combine.modifier.placement.height
import top.fifthlight.combine.modifier.pointer.draggable
import top.fifthlight.combine.ui.style.DrawableSet
import top.fifthlight.combine.widget.base.Canvas
import top.fifthlight.data.IntOffset
import top.fifthlight.data.IntRect
import top.fifthlight.data.IntSize
import top.fifthlight.touchcontroller.assets.Textures

data class SliderDrawableSet(
    val activeTrack: DrawableSet,
    val inactiveTrack: DrawableSet?,
    val handle: DrawableSet,
)

val defaultSliderDrawable = SliderDrawableSet(
    activeTrack = DrawableSet(
        normal = Textures.WIDGET_SLIDER_SLIDER_ACTIVE,
        focus = Textures.WIDGET_SLIDER_SLIDER_ACTIVE_HOVER,
        hover = Textures.WIDGET_SLIDER_SLIDER_ACTIVE_HOVER,
        active = Textures.WIDGET_SLIDER_SLIDER_ACTIVE_ACTIVE,
        disabled = Textures.WIDGET_SLIDER_SLIDER_ACTIVE_DISABLED,
    ),
    inactiveTrack = DrawableSet(
        normal = Textures.WIDGET_SLIDER_SLIDER_INACTIVE,
        focus = Textures.WIDGET_SLIDER_SLIDER_INACTIVE_HOVER,
        hover = Textures.WIDGET_SLIDER_SLIDER_INACTIVE_HOVER,
        active = Textures.WIDGET_SLIDER_SLIDER_INACTIVE_ACTIVE,
        disabled = Textures.WIDGET_SLIDER_SLIDER_INACTIVE_DISABLED,
    ),
    handle = DrawableSet(
        normal = Textures.WIDGET_HANDLE_HANDLE,
        focus = Textures.WIDGET_HANDLE_HANDLE_HOVER,
        hover = Textures.WIDGET_HANDLE_HANDLE_HOVER,
        active = Textures.WIDGET_HANDLE_HANDLE_ACTIVE,
        disabled = Textures.WIDGET_HANDLE_HANDLE_DISABLED,
    ),
)

val LocalSliderDrawable = staticCompositionLocalOf<SliderDrawableSet> { defaultSliderDrawable }

@Composable
fun Slider(
    modifier: Modifier = Modifier,
    drawableSet: SliderDrawableSet = LocalSliderDrawable.current,
    range: ClosedFloatingPointRange<Float>,
    value: Float,
    onValueChanged: (Float) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val state by widgetState(interactionSource)

    fun Float.toValue() = this * (range.endInclusive - range.start) + range.start
    fun Float.toProgress() = (this - range.start) / (range.endInclusive - range.start)

    val progress = value.toProgress()

    val handleDrawable = drawableSet.handle.getByState(state)
    val handleLeftHalfWidth = handleDrawable.size.width / 2

    val activeTrackDrawable = drawableSet.activeTrack.getByState(state)
    val inactiveTrackDrawable = drawableSet.inactiveTrack?.getByState(state)

    Canvas(
        modifier = Modifier
            .height(height = 16)
            .draggable(interactionSource) { _, absolute ->
                val rawProgress = (absolute.x - handleLeftHalfWidth) / (size.width - handleDrawable.size.width)
                val newProgress = rawProgress.coerceIn(0f, 1f)
                onValueChanged(newProgress.toValue())
            }
            .focusable(interactionSource)
            .then(modifier),
    ) { node ->
        val trackRect = IntRect(
            offset = IntOffset(
                x = handleLeftHalfWidth,
                y = 0
            ),
            size = IntSize(
                width = node.width - handleDrawable.size.width,
                height = node.height
            )
        )
        val progressWidth = (trackRect.size.width * progress).toInt()

        activeTrackDrawable.run { draw(trackRect) }

        inactiveTrackDrawable?.run {
            draw(
                IntRect(
                    offset = trackRect.offset + IntOffset(
                        x = progressWidth.toInt(),
                        y = 0,
                    ),
                    size = IntSize(
                        width = trackRect.size.width - progressWidth.toInt(),
                        height = trackRect.size.height,
                    )
                )
            )
        }

        handleDrawable.run {
            draw(
                IntRect(
                    offset = IntOffset(
                        x = progressWidth,
                        y = 0,
                    ),
                    size = handleDrawable.size,
                )
            )
        }
    }
}

@Composable
fun IntSlider(
    modifier: Modifier = Modifier,
    drawableSet: SliderDrawableSet = LocalSliderDrawable.current,
    range: IntRange,
    value: Int,
    onValueChanged: (Int) -> Unit,
) {
    fun Int.toProgress() = (this - range.first).toFloat() / (range.last - range.first)
    fun Float.toValue() = (this * (range.last - range.first)).toInt() + range.first

    Slider(
        modifier = modifier,
        drawableSet = drawableSet,
        range = 0f..1f,
        value = value.toProgress(),
        onValueChanged = {
            onValueChanged(it.toValue())
        },
    )
}