package top.fifthlight.combine.modifier

import top.fifthlight.combine.node.LayoutNode

interface AttachListenerModifierNode {
    fun onAttachedToNode(node: LayoutNode)
}
