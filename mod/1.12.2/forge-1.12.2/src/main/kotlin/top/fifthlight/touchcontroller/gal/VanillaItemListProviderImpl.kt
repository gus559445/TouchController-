package top.fifthlight.touchcontroller.gal

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.ItemStack
import net.minecraft.util.NonNullList
import net.minecraft.util.text.TextComponentTranslation
import net.minecraftforge.fml.common.registry.ForgeRegistries
import top.fifthlight.combine.platform.TextImpl
import top.fifthlight.combine.platform.toCombine
import top.fifthlight.touchcontroller.common.gal.PlayerHandle
import top.fifthlight.touchcontroller.common.gal.VanillaItemListProvider

object VanillaItemListProviderImpl : VanillaItemListProvider {
    class CreativeTabImpl(group: CreativeTabs, items: PersistentList<ItemStack>) :
        VanillaItemListProvider.CreativeTab {
        override val type = when (group) {
            CreativeTabs.INVENTORY -> VanillaItemListProvider.CreativeTab.Type.SURVIVAL_INVENTORY
            CreativeTabs.SEARCH -> VanillaItemListProvider.CreativeTab.Type.SEARCH
            else -> VanillaItemListProvider.CreativeTab.Type.CATEGORY
        }

        override val name = TextImpl(TextComponentTranslation(group.translationKey))

        override val icon = group.icon.toCombine()

        override val items = items.map(ItemStack::toCombine).toPersistentList()
    }

    private val creativeTabs by lazy {
        CreativeTabs
            .CREATIVE_TAB_ARRAY
            .filter { it != CreativeTabs.HOTBAR }
            .map {
                val list = NonNullList.create<ItemStack>()
                ForgeRegistries.ITEMS.forEach { item -> item.getSubItems(it, list) }
                CreativeTabImpl(it, list.toPersistentList())
            }
            .toPersistentList()
    }

    override fun getCreativeTabs(player: PlayerHandle) = creativeTabs
}