package top.fifthlight.touchcontroller.gal

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.Item
import top.fifthlight.combine.platform.ItemFactoryImpl
import top.fifthlight.combine.platform.ItemImpl
import top.fifthlight.touchcontroller.common.config.ItemList
import top.fifthlight.touchcontroller.common.gal.DefaultItemListProvider

object DefaultItemListProviderImpl : DefaultItemListProvider {
    override val usableItems = ItemList(
        whitelist = persistentListOf(
            ItemImpl(Items.BOW),
            ItemImpl(Items.FISHING_ROD),
            ItemImpl(Items.MAP),
            ItemImpl(Items.SHIELD),
            ItemImpl(Items.KNOWLEDGE_BOOK),
            ItemImpl(Items.WRITABLE_BOOK),
            ItemImpl(Items.WRITTEN_BOOK),
            ItemImpl(Items.ENDER_EYE),
            ItemImpl(Items.ENDER_PEARL),
            ItemImpl(Items.POTIONITEM),
            ItemImpl(Items.SNOWBALL),
            ItemImpl(Items.EGG),
            ItemImpl(Items.SPLASH_POTION),
            ItemImpl(Items.LINGERING_POTION),
            ItemImpl(Items.EXPERIENCE_BOTTLE),
            ItemImpl(Items.MILK_BUCKET),
        ),
        subclasses = persistentSetOf(
            ItemFactoryImpl.armorSubclass,
            ItemFactoryImpl.foodSubclass,
        ),
    )

    override val showCrosshairItems = ItemList(
        whitelist = persistentListOf(
            ItemImpl(Items.EGG),
            ItemImpl(Items.SNOWBALL),
            ItemImpl(Items.BOW),
            ItemImpl(Items.SPLASH_POTION),
            ItemImpl(Items.LINGERING_POTION),
            ItemImpl(Items.EXPERIENCE_BOTTLE),
            ItemImpl(Items.ENDER_EYE),
        ),
    )

    override val crosshairAimingItems = ItemList(
        whitelist = persistentListOf(
            ItemImpl(Items.ENDER_EYE),
            ItemImpl(Item.getItemFromBlock(Blocks.WATERLILY)),
        ),
        subclasses = persistentSetOf(
            ItemFactoryImpl.bucketSubclass,
            ItemFactoryImpl.universalBucketSubclass,
            ItemFactoryImpl.boatSubclass,
            ItemFactoryImpl.monsterPlacerSubclass,
        ),
    )
}