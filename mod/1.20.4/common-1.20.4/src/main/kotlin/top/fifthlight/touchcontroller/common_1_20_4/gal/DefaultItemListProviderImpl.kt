package top.fifthlight.touchcontroller.common_1_20_4.gal

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import net.minecraft.world.item.Items
import top.fifthlight.combine.platform_1_20_4.FoodComponentImpl
import top.fifthlight.combine.platform_1_20_4.ItemFactoryImpl
import top.fifthlight.combine.platform_1_20_4.ItemImpl
import top.fifthlight.touchcontroller.common.config.ItemList
import top.fifthlight.touchcontroller.common.gal.DefaultItemListProvider

object DefaultItemListProviderImpl : DefaultItemListProvider {
    override val usableItems = ItemList(
        whitelist = persistentListOf(
            ItemImpl(Items.FISHING_ROD),
            ItemImpl(Items.SPYGLASS),
            ItemImpl(Items.MAP),
            ItemImpl(Items.SHIELD),
            ItemImpl(Items.KNOWLEDGE_BOOK),
            ItemImpl(Items.WRITABLE_BOOK),
            ItemImpl(Items.WRITTEN_BOOK),
            ItemImpl(Items.ENDER_EYE),
            ItemImpl(Items.ENDER_PEARL),
            ItemImpl(Items.POTION),
            ItemImpl(Items.SNOWBALL),
            ItemImpl(Items.EGG),
            ItemImpl(Items.SPLASH_POTION),
            ItemImpl(Items.LINGERING_POTION),
            ItemImpl(Items.EXPERIENCE_BOTTLE),
            ItemImpl(Items.TRIDENT),
            ItemImpl(Items.MILK_BUCKET),
        ),
        subclasses = persistentSetOf(
            ItemFactoryImpl.rangedWeaponSubclass,
            ItemFactoryImpl.armorSubclass,
            ItemFactoryImpl.bundleSubclass,
        ),
        components = persistentListOf(
            FoodComponentImpl,
        )
    )

    override val showCrosshairItems = ItemList(
        whitelist = persistentListOf(
            ItemImpl(Items.EGG),
            ItemImpl(Items.SNOWBALL),
            ItemImpl(Items.SPLASH_POTION),
            ItemImpl(Items.LINGERING_POTION),
            ItemImpl(Items.EXPERIENCE_BOTTLE),
            ItemImpl(Items.TRIDENT),
            ItemImpl(Items.ENDER_PEARL),
        ),
        subclasses = persistentSetOf(
            ItemFactoryImpl.rangedWeaponSubclass,
        )
    )

    override val crosshairAimingItems = ItemList(
        whitelist = persistentListOf(
            ItemImpl(Items.ENDER_EYE),
            ItemImpl(Items.GLASS_BOTTLE),
        ),
        subclasses = persistentSetOf(
            ItemFactoryImpl.bucketSubclass,
            ItemFactoryImpl.boatSubclass,
            ItemFactoryImpl.placeableOnWaterSubclass,
            ItemFactoryImpl.spawnEggSubclass,
        )
    )
}