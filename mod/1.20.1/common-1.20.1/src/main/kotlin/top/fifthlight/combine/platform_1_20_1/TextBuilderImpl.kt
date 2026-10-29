package top.fifthlight.combine.platform_1_20_1

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.contents.LiteralContents
import top.fifthlight.combine.data.TextBuilder
import top.fifthlight.combine.platform_1_20_x.TextImpl
import top.fifthlight.combine.data.Text as CombineText

class TextBuilderImpl(
    private val text: MutableComponent = Component.empty(),
    private val style: Style = Style.EMPTY,
) : TextBuilder {
    override fun bold(bold: Boolean, block: TextBuilder.() -> Unit) {
        block(
            TextBuilderImpl(
                text = text,
                style = style.withBold(bold),
            )
        )
    }

    override fun underline(underline: Boolean, block: TextBuilder.() -> Unit) {
        block(
            TextBuilderImpl(
                text = text,
                style = style.withUnderlined(underline),
            )
        )
    }

    override fun italic(italic: Boolean, block: TextBuilder.() -> Unit) {
        block(
            TextBuilderImpl(
                text = text,
                style = style.withItalic(italic),
            )
        )
    }

    override fun append(string: String) {
        this.text.append(MutableComponent.create(LiteralContents(string)).setStyle(style))
    }

    override fun appendWithoutStyle(text: CombineText) {
        this.text.append(MutableComponent.create(text.toMinecraft().contents).setStyle(style))
    }

    fun build(): CombineText = TextImpl(text)
}
