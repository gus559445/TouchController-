package top.fifthlight.combine.platform_1_20_1

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.Item
import top.fifthlight.combine.data.DataComponentType
import top.fifthlight.combine.data.Identifier
import top.fifthlight.combine.data.ItemSubclass
import top.fifthlight.combine.platform_1_20_x.AbstractItemImpl
import top.fifthlight.combine.platform_1_20_x.toCombine

class ItemImpl(inner: Item) : AbstractItemImpl(inner) {
    override val id: Identifier
        get() = BuiltInRegistries.ITEM.getKey(inner).toCombine()

    override fun isSubclassOf(subclass: ItemSubclass): Boolean {
        val targetClazz = (subclass as ItemSubclassImpl<*>).clazz
        val itemClazz = inner.javaClass
        return itemClazz == targetClazz || itemClazz.superclass == targetClazz || itemClazz.interfaces.contains(
            targetClazz
        )
    }

    override fun containComponents(component: DataComponentType) = if (component is FoodComponentImpl) {
        inner.isEdible
    } else {
        false
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ItemImpl

        return inner == other.inner
    }

    override fun hashCode(): Int {
        return inner.hashCode()
    }
}