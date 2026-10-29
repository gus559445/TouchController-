package top.fifthlight.combine.modifier.scroll

import androidx.compose.runtime.Composable
import top.fifthlight.combine.data.BackgroundTexture
import top.fifthlight.combine.input.pointer.PointerEvent
import top.fifthlight.combine.input.pointer.PointerEventType
import top.fifthlight.combine.layout.Measurable
import top.fifthlight.combine.layout.MeasureResult
import top.fifthlight.combine.layout.MeasureScope
import top.fifthlight.combine.layout.Placeable
import top.fifthlight.combine.modifier.*
import top.fifthlight.combine.node.LayoutNode
import top.fifthlight.combine.paint.Canvas
import top.fifthlight.combine.paint.Color
import top.fifthlight.data.IntOffset
import top.fifthlight.data.IntRect
import top.fifthlight.data.IntSize
import top.fifthlight.data.Offset
import top.fifthlight.data.Rect
import top.fifthlight.data.Size
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

@Composable
fun Modifier.verticalScroll(
    reverse: Boolean,
) = verticalScroll(
    scrollState = rememberScrollState(),
    reverse = reverse,
)

@Composable
fun Modifier.verticalScroll(
    scrollState: ScrollState = rememberScrollState(),
    reverse: Boolean = false,
    background: BackgroundTexture? = null,
    backgroundScale: Float = 1f
) = then(VerticalScrollNode(
    scrollState = scrollState,
    reverse = reverse,
    background = background,
    backgroundScale = backgroundScale
))

private data class VerticalScrollNode(
    val scrollState: ScrollState,
    val reverse: Boolean,
    val background: BackgroundTexture?,
    val backgroundScale: Float,
) : LayoutModifierNode, DrawModifierNode, PointerInputModifierNode, Modifier.Node<VerticalScrollNode> {
    override fun onPointerEvent(
        event: PointerEvent,
        node: Placeable,
        layoutNode: LayoutNode,
        children: (PointerEvent) -> Boolean
    ): Boolean {
        return when (event.type) {
            PointerEventType.Scroll -> {
                val scrollDelta = if (reverse) -event.scrollDelta.y else event.scrollDelta.y
                scrollState.updateProgress((scrollState.progress.value - scrollDelta * 12).toInt(), animateOverscroll = true)
                true
            }

            PointerEventType.Press -> {
                scrollState.initialPointerPosition = event.position
                scrollState.startPointerPosition = null
                scrollState.scrolling = false
                scrollState.stopAnimation()
                false
            }

            PointerEventType.Cancel, PointerEventType.Release -> {
                scrollState.initialPointerPosition = null
                scrollState.startPointerPosition = null
                scrollState.updateProgress(scrollState.progress.value, animateOverscroll = true)
                if (scrollState.scrolling) {
                    scrollState.scrolling = false
                    true
                } else {
                    false
                }
            }

            PointerEventType.Move -> {
                val initialPosition = scrollState.initialPointerPosition
                if (scrollState.scrolling) {
                    val distance = if (reverse) {
                        (event.position.y - scrollState.startPointerPosition!!.y).roundToInt()
                    } else {
                        (scrollState.startPointerPosition!!.y - event.position.y).roundToInt()
                    }
                    scrollState.updateProgress(distance + scrollState.startProgress)
                    true
                } else if (initialPosition != null) {
                    val distance = if (reverse) {
                        (event.position.y - initialPosition.y)
                    } else {
                        (initialPosition.y - event.position.y)
                    }
                    if (distance.absoluteValue > 8) {
                        scrollState.scrolling = true
                        scrollState.startProgress = scrollState.progress.value
                        scrollState.startPointerPosition = event.position
                        children(event.copy(type = PointerEventType.Cancel))
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            }

            else -> false
        }
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val viewportMaxHeight = constraints.maxHeight
        if (viewportMaxHeight == Int.MAX_VALUE) {
            error("Bad maxHeight of verticalScroll(): check nested scroll modifiers")
        }

        val placeable = measurable.measure(
            constraints.copy(
                minHeight = constraints.minHeight,
                maxHeight = Int.MAX_VALUE,
            )
        )

        val viewportHeight = placeable.height.coerceAtMost(viewportMaxHeight)
        scrollState.contentHeight = placeable.height
        scrollState.viewportHeight = viewportHeight

        val maxScrollOffset = (placeable.height - viewportHeight).coerceAtLeast(0)
        val actualProgress = scrollState.actualProgress.value
        if (actualProgress > maxScrollOffset) {
            scrollState.updateProgress(maxScrollOffset)
        } else if (actualProgress < 0) {
            scrollState.updateProgress(0)
        }

        return layout(placeable.width, viewportHeight) {
            val yOffset = if (reverse) {
                -(maxScrollOffset - scrollState.progress.value)
            } else {
                -scrollState.progress.value
            }
            placeable.placeAt(0, yOffset)
        }
    }

    override fun Canvas.renderBefore(node: Placeable) {
        pushClip(
            IntRect(
                offset = IntOffset(node.absoluteX, node.absoluteY),
                size = IntSize(node.width, node.height)
            ),
            IntRect(
                offset = IntOffset(node.x, node.y),
                size = IntSize(node.width, node.height)
            ),
        )
        background?.let { background ->
            val height = background.size.height
            if (height == 0) {
                return@let
            }
            val tileHeight = height * backgroundScale
            val tileOffset = scrollState.progress.value.toFloat() % tileHeight
            drawBackgroundTexture(
                texture = background,
                scale = backgroundScale,
                dstRect = Rect(
                    offset = Offset(
                        x = 0f,
                        y = -tileHeight - tileOffset,
                    ),
                    size = Size(
                        width = node.width.toFloat(),
                        height = node.height.toFloat() + tileHeight * 2,
                    ),
                )
            )
        }
    }

    override fun Canvas.renderAfter(node: Placeable) {
        if (scrollState.viewportHeight < scrollState.contentHeight) {
            val progress =
                scrollState.progress.value.toFloat() / (scrollState.contentHeight - scrollState.viewportHeight).toFloat()
            val barHeight = (node.height * scrollState.viewportHeight / scrollState.contentHeight).coerceAtLeast(12)
            val barY = ((node.height - barHeight) * if (reverse) {
                1f - progress
            } else {
                progress
            }).roundToInt()
            fillRect(
                offset = IntOffset(node.width - 3, barY),
                size = IntSize(3, barHeight),
                color = Color(0x66FFFFFFu),
            )
        }
        popClip()
    }
}
