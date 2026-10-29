package top.fifthlight.combine.platform_1_20_1

import top.fifthlight.combine.data.TextBuilder
import top.fifthlight.combine.platform_1_20_x.AbstractTextFactoryImpl
import top.fifthlight.combine.platform_1_20_x.TextImpl
import top.fifthlight.combine.data.Text as CombineText

object TextFactoryImpl : AbstractTextFactoryImpl() {
    override fun build(block: TextBuilder.() -> Unit) = TextBuilderImpl().apply(block).build()
}

fun CombineText.toMinecraft() = (this as TextImpl).inner