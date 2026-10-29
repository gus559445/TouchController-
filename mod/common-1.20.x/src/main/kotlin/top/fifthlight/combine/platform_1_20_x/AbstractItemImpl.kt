package top.fifthlight.combine.platform_1_20_x

import net.minecraft.world.item.Item
import top.fifthlight.combine.data.Item as CombineItem

abstract class AbstractItemImpl(
    val inner: Item
) : CombineItem

fun CombineItem.toVanilla() = (this as AbstractItemImpl).inner
