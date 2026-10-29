package top.fifthlight.touchcontroller.gal

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemStack
import net.minecraft.util.NonNullList
import net.minecraftforge.registries.ForgeRegistries
import top.fifthlight.combine.platform.TextImpl
import top.fifthlight.combine.platform.toCombine
import top.fifthlight.touchcontroller.common.gal.PlayerHandle
import top.fifthlight.touchcontroller.common.gal.VanillaItemListProvider

object VanillaItemListProviderImpl : VanillaItemListProvider {
    class CreativeTabImpl(group: ItemGroup, items: PersistentList<ItemStack>) :
        VanillaItemListProvider.CreativeTab {
        override val type = when (group) {
            ItemGroup.TAB_INVENTORY -> VanillaItemListProvider.CreativeTab.Type.SURVIVAL_INVENTORY
            ItemGroup.TAB_SEARCH -> VanillaItemListProvider.CreativeTab.Type.SEARCH
            else -> VanillaItemListProvider.CreativeTab.Type.CATEGORY
        }

        override val name = TextImpl(group.displayName)

        override val icon = group.iconItem.toCombine()

        override val items = items.map(ItemStack::toCombine).toPersistentList()
    }

    private val creativeTabs by lazy {
        ItemGroup
            .TABS
            .filter { it != ItemGroup.TAB_HOTBAR }
            .map {
                val list = NonNullList.create<ItemStack>()
                ForgeRegistries.ITEMS.forEach { item -> item.fillItemCategory(it, list) }
                CreativeTabImpl(it, list.toPersistentList())
            }
            .toPersistentList()
    }

    override fun getCreativeTabs(player: PlayerHandle) = creativeTabs
}