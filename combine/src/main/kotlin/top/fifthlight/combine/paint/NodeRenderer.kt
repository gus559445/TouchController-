package top.fifthlight.combine.paint

import top.fifthlight.combine.layout.Placeable

fun interface NodeRenderer {
    fun Canvas.render(node: Placeable)

    companion object EmptyRenderer : NodeRenderer {
        override fun Canvas.render(node: Placeable) = Unit
    }
}