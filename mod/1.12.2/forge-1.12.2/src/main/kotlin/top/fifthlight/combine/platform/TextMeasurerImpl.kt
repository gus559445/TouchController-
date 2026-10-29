package top.fifthlight.combine.platform

import net.minecraft.client.Minecraft
import sun.swing.MenuItemLayoutHelper.max
import top.fifthlight.combine.data.Text
import top.fifthlight.combine.paint.TextMeasurer
import top.fifthlight.data.IntSize
import kotlin.math.min

object TextMeasurerImpl : TextMeasurer {
    private val client = Minecraft.getMinecraft()
    private val textRenderer = client.fontRenderer

    override fun measure(text: String): IntSize {
        var width = 0
        var height = 0
        for (line in text.lineSequence()) {
            val lineWidth = textRenderer.getStringWidth(line)
            width = max(width, lineWidth)
            height += textRenderer.FONT_HEIGHT
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
            val lineWidth = textRenderer.getStringWidth(line)
            val lineHeight = if (lineWidth > maxWidth) {
                textRenderer.getWordWrappedHeight(line, maxWidth)
            } else {
                textRenderer.FONT_HEIGHT
            }
            width = max(width, min(lineWidth, maxWidth))
            height += lineHeight
        }
        return IntSize(width, height)
    }

    override fun measure(text: Text) = measure(text.toMinecraft().formattedText)

    override fun measure(text: Text, maxWidth: Int) = measure(text.toMinecraft().formattedText, maxWidth)
}