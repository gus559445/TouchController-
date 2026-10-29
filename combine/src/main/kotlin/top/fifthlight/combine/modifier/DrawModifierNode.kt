package top.fifthlight.combine.modifier

import top.fifthlight.combine.layout.Placeable
import top.fifthlight.combine.paint.Canvas

interface DrawModifierNode {
    fun Canvas.renderBefore(node: Placeable) {}
    fun Canvas.renderAfter(node: Placeable) {}
}