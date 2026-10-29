package top.fifthlight.touchcontroller.common_1_20_1.gal

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Items
import top.fifthlight.combine.platform_1_20_1.FoodComponentImpl
import top.fifthlight.combine.platform_1_20_1.ItemFactoryImpl
import top.fifthlight.combine.platform_1_20_1.ItemImpl
import top.fifthlight.touchcontroller.common.config.ItemList
import top.fifthlight.touchcontroller.common.gal.DefaultItemListProvider

object DefaultItemListProviderImpl : DefaultItemListProvider {
    /**
     * 按 id 取一个「软依赖」的模组物品：没装那个模组就返回 null，不往清单里塞空气。
     *
     * <p>「点一下就生效」的模组道具如果不进这份清单，触控长按会把它们当成挖方块，
     * 表现就是「怎么按都没反应」——所以默认替玩家把它们勾上。</p>
     */
    private fun modItem(id: String): ItemImpl? {
        val item = BuiltInRegistries.ITEM.get(ResourceLocation(id))
        return if (item == Items.AIR) null else ItemImpl(item)
    }

    /** mob_controller：驭兽哨 / 特制驭兽哨 / 戎卫号角，都是「右键一下就用」的道具。 */
    private val mobControllerItems: List<ItemImpl> = listOfNotNull(
        modItem("mob_controller:control_command"),
        modItem("mob_controller:mode_select_control_command"),
        modItem("mob_controller:aggressive_switch"),
    )

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
        ).addAll(mobControllerItems),
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

    /**
     * 「长按用不了也不挖方块」的默认例外：金苹果、附魔金苹果、三种药水。
     *
     * <p>这些物品「吃饱了也要按着用」（金苹果 / 附魔金苹果满饥饿也能吃、药水本来就不看饥饿值），
     * 所以长按它们时即便判定「现在用不出去」，也**不该**改判成挖方块。</p>
     *
     * <p>存进的是 [ItemList] 的 `blacklist` —— 界面上的「黑名单」就是这个字段：
     * 命中的物品长按用不出去时保持空按，不改判挖方块。</p>
     */
    override val longPressDigItems = ItemList(
        blacklist = persistentListOf(
            ItemImpl(Items.GOLDEN_APPLE),
            ItemImpl(Items.ENCHANTED_GOLDEN_APPLE),
            ItemImpl(Items.POTION),
            ItemImpl(Items.SPLASH_POTION),
            ItemImpl(Items.LINGERING_POTION),
        ),
    )
}