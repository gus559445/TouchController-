package top.fifthlight.combine.platform

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import net.minecraft.item.*
import net.minecraft.util.text.ITextComponent
import net.minecraftforge.registries.ForgeRegistries
import top.fifthlight.combine.data.Identifier
import top.fifthlight.combine.data.ItemFactory
import top.fifthlight.combine.data.ItemSubclass
import top.fifthlight.touchcontroller.common.config.ItemList
import top.fifthlight.combine.data.Item as CombineItem
import top.fifthlight.combine.data.ItemStack as CombineItemStack

object ItemFactoryImpl : ItemFactory {
    override fun createItem(id: Identifier): CombineItem? {
        val item = ForgeRegistries.ITEMS.getValue(id.toMinecraft()) ?: return null
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
        val item = ForgeRegistries.ITEMS.getValue(id.toMinecraft()) ?: return null
        val stack = ItemStack(item, amount)
        return ItemStackImpl(stack)
    }

    override val allItems: PersistentList<CombineItem> by lazy {
        ForgeRegistries.ITEMS.map(Item::toCombine).toPersistentList()
    }

    val rangedWeaponSubclass = ItemSubclassImpl(
        name = TextImpl(ITextComponent.nullToEmpty("Ranged weapon")),
        configId = "ShootableItem",
        clazz = ShootableItem::class.java
    )

    val armorSubclass = ItemSubclassImpl(
        name = TextImpl(ITextComponent.nullToEmpty("Armor")),
        configId = "ArmorItem",
        clazz = ArmorItem::class.java
    )

    val bucketSubclass = ItemSubclassImpl(
        name = TextImpl(ITextComponent.nullToEmpty("Bucket")),
        configId = "BucketItem",
        clazz = BucketItem::class.java
    )

    val boatSubclass = ItemSubclassImpl(
        name = TextImpl(ITextComponent.nullToEmpty("Boat")),
        configId = "BoatItem",
        clazz = BoatItem::class.java,
    )

    val spawnEggSubclass = ItemSubclassImpl(
        name = TextImpl(ITextComponent.nullToEmpty("SpawnEgg")),
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
