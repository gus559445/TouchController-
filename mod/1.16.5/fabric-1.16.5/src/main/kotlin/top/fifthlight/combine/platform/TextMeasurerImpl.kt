package top.fifthlight.combine.platform

import net.minecraft.client.MinecraftClient
import top.fifthlight.combine.data.Text
import top.fifthlight.combine.paint.TextMeasurer
import top.fifthlight.data.IntSize
import kotlin.math.max
import kotlin.math.min

object TextMeasurerImpl : TextMeasurer {
    private val client = MinecraftClient.getInstance()
    private val textRenderer = client.textRenderer

    override fun measure(text: String): IntSize {
        var width = 0
        var height = 0
        for (line in text.lineSequence()) {
            val lineWidth = textRenderer.getWidth(line)
            width = max(width, lineWidth)
            height += textRenderer.fontHeight
        }
        return IntSize(width, height)
    }

    override fun measure(text: String, maxWidth: Int): IntSize {
        if (maxWidth < 16) {
            return IntSize.ZERO
        }
        var width = 0
        var height = 0
        for (line in text.lineSequence()) {
            val lineWidth = textRenderer.getWidth(line)
            val lineHeight = if (lineWidth > maxWidth) {
                textRenderer.getWrappedLinesHeight(line, maxWidth)
            } else {
                textRenderer.fontHeight
            }
            width = max(width, min(lineWidth, maxWidth))
            height += lineHeight
        }
        return IntSize(width, height)
    }

    override fun measure(text: Text) = measure(text.toMinecraft().string)

    override fun measure(text: Text, maxWidth: Int) = measure(text.toMinecraft().string, maxWidth)
}