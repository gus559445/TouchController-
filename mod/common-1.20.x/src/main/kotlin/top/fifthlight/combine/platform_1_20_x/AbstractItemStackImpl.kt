package top.fifthlight.combine.platform_1_20_x

import net.minecraft.world.item.ItemStack
import top.fifthlight.combine.data.Text
import top.fifthlight.combine.data.ItemStack as CombineItemStack

abstract class AbstractItemStackImpl(
    val inner: ItemStack
) : CombineItemStack {
    override val amount: Int
        get() = inner.count

    override val isEmpty: Boolean
        get() = inner.isEmpty

    override val name: Text
        get() = TextImpl(inner.hoverName)
}

fun CombineItemStack.toVanilla() = (this as AbstractItemStackImpl).inner
