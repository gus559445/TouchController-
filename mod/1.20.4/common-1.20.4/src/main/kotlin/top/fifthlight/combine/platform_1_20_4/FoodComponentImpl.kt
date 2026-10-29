package top.fifthlight.combine.platform_1_20_4

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import net.minecraft.core.registries.BuiltInRegistries
import top.fifthlight.combine.data.DataComponentType
import top.fifthlight.combine.data.Identifier
import top.fifthlight.combine.data.Item
import top.fifthlight.touchcontroller.buildinfo.BuildInfo

object FoodComponentImpl : DataComponentType {
    override val id: Identifier = Identifier.of(BuildInfo.MOD_ID, "food")

    override val allItems: PersistentList<Item> by lazy {
        BuiltInRegistries.ITEM.filter { it.isEdible }.map { it.toCombine() }.toPersistentList()
    }
}