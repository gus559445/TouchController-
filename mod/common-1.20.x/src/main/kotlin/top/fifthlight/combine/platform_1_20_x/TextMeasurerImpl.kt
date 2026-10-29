package top.fifthlight.combine.platform_1_20_x

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import top.fifthlight.combine.data.Text
import top.fifthlight.combine.paint.TextMeasurer
import top.fifthlight.data.IntSize

object TextMeasurerImpl : TextMeasurer {
    private val client = Minecraft.getInstance()
    private val textRenderer = client.font

    override fun measure(text: String) = IntSize(
        width = textRenderer.split(Component.literal(text), Int.MAX_VALUE).maxOfOrNull { textRenderer.width(it) } ?: 0,
        height = textRenderer.wordWrapHeight(text, Int.MAX_VALUE),
    )

    override fun measure(text: String, maxWidth: Int) = IntSize(
        width = textRenderer.split(Component.literal(text), maxWidth)
            .maxOfOrNull { textRenderer.width(it) }
            ?.coerceIn(0, maxWidth) ?: 0,
        height = textRenderer.wordWrapHeight(text, maxWidth),
    )

    override fun measure(text: Text) = IntSize(
        width = textRenderer.split(text.toMinecraft(), Int.MAX_VALUE).maxOfOrNull { textRenderer.width(it) } ?: 0,
        height = textRenderer.wordWrapHeight(text.toMinecraft(), Int.MAX_VALUE),
    )

    override fun measure(text: Text, maxWidth: Int) = IntSize(
        width = textRenderer.split(text.toMinecraft(), maxWidth)
            .maxOfOrNull { textRenderer.width(it) }
            ?.coerceIn(0, maxWidth) ?: 0,
        height = textRenderer.wordWrapHeight(text.toMinecraft(), maxWidth),
    )
}