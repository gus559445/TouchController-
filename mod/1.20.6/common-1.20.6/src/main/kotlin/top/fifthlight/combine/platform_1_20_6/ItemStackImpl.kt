package top.fifthlight.combine.platform_1_20_6

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.ItemStack
import top.fifthlight.combine.data.Identifier
import top.fifthlight.combine.data.Item
import top.fifthlight.combine.platform_1_20_x.AbstractItemStackImpl
import top.fifthlight.combine.platform_1_20_x.toCombine

class ItemStackImpl(inner: ItemStack) : AbstractItemStackImpl(inner) {
    override val id: Identifier
        get() = BuiltInRegistries.ITEM.getKey(inner.item).toCombine()

    override val item: Item
        get() = ItemImpl(inner.item)

    override fun withAmount(amount: Int) = ItemStackImpl(inner.copyWithCount(amount))
}