package top.fifthlight.combine.platform

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import net.minecraft.item.*
import net.minecraft.text.Text
import net.minecraft.util.registry.Registry
import top.fifthlight.combine.data.Identifier
import top.fifthlight.combine.data.ItemFactory
import top.fifthlight.combine.data.ItemSubclass
import top.fifthlight.touchcontroller.common.config.ItemList
import kotlin.jvm.optionals.getOrNull
import top.fifthlight.combine.data.Item as CombineItem
import top.fifthlight.combine.data.ItemStack as CombineItemStack

object ItemFactoryImpl : ItemFactory {
    override fun createItem(id: Identifier): CombineItem? {
        val item = Registry.ITEM.getOrEmpty(id.toMinecraft()).getOrNull() ?: return null
        return ItemImpl(item)
    }

    override fun createItemStack(
        item: CombineItem,
        amount: Int
    ): CombineItemStack {
        val minecraftItem = (item as ItemImpl).inner
        val stack = ItemStack(minecraftItem, amount)
        return ItemStackImpl(stack)
    }

    override fun createItemStack(id: Identifier, amount: Int): CombineItemStack? {
        val item = Registry.ITEM.getOrEmpty(id.toMinecraft()).getOrNull() ?: return null
        val stack = ItemStack(item, amount)
        return ItemStackImpl(stack)
    }

    override val allItems: PersistentList<CombineItem> by lazy {
        (Registry.ITEM as Iterable<Item>).map(Item::toCombine).toPersistentList()
    }

    val rangedWeaponSubclass = ItemSubclassImpl(
        name = TextImpl(Text.of("Ranged weapon")),
        configId = "RangedWeaponItem",
        clazz = RangedWeaponItem::class.java
    )

    val armorSubclass = ItemSubclassImpl(
        name = TextImpl(Text.of("Armor")),
        configId = "ArmorItem",
        clazz = ArmorItem::class.java
    )

    val bucketSubclass = ItemSubclassImpl(
        name = TextImpl(Text.of("Bucket")),
        configId = "BucketItem",
        clazz = BucketItem::class.java
    )

    val boatSubclass = ItemSubclassImpl(
        name = TextImpl(Text.of("Boat")),
        configId = "BoatItem",
        clazz = BoatItem::class.java,
    )

    val spawnEggSubclass = ItemSubclassImpl(
        name = TextImpl(Text.of("SpawnEgg")),
        configId = "SpawnEggItem",
        clazz = SpawnEggItem::class.java,
    )

    override val subclasses: PersistentList<ItemSubclass> = persistentListOf(
        rangedWeaponSubclass,
        armorSubclass,
        bucketSubclass,
        boatSubclass,
        spawnEggSubclass,
    )
}

fun Item.toCombine() = ItemImpl(this)
fun ItemStack.toCombine() = ItemStackImpl(this)
fun CombineItem.toVanilla() = (this as ItemImpl).inner
fun CombineItemStack.toVanilla() = (this as ItemStackImpl).inner
fun ItemList.contains(item: Item) = contains(item.toCombine())
