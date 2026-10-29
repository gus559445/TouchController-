package top.fifthlight.touchcontroller.common_1_20_6.gal

import kotlinx.collections.immutable.toPersistentList
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.animal.Pig
import net.minecraft.world.entity.animal.camel.Camel
import net.minecraft.world.entity.animal.horse.*
import net.minecraft.world.entity.monster.Strider
import net.minecraft.world.entity.vehicle.Boat
import net.minecraft.world.entity.vehicle.Minecart
import top.fifthlight.combine.data.Item
import top.fifthlight.combine.data.ItemStack
import top.fifthlight.combine.platform_1_20_6.ItemStackImpl
import top.fifthlight.combine.platform_1_20_6.toCombine
import top.fifthlight.touchcontroller.common.config.ItemList
import top.fifthlight.touchcontroller.common.gal.PlayerHandle
import top.fifthlight.touchcontroller.common.gal.PlayerHandleFactory
import top.fifthlight.touchcontroller.common.gal.PlayerInventory
import top.fifthlight.touchcontroller.common.gal.RidingEntityType
import top.fifthlight.touchcontroller.common_1_20_x.gal.AbstractPlayerHandleImpl

class PlayerHandleImpl(inner: LocalPlayer) : AbstractPlayerHandleImpl(inner) {
    override fun hasItemsOnHand(list: ItemList): Boolean = InteractionHand.entries.any { hand ->
        inner.getItemInHand(hand).toCombine().item in list
    }

    override fun matchesItemOnHand(item: Item): Boolean = InteractionHand.entries.any { hand ->
        item.matches(inner.getItemInHand(hand).toCombine().item)
    }

    override fun getInventorySlot(index: Int): ItemStack = ItemStackImpl(inner.inventory.getItem(index))

    override fun getInventory() = PlayerInventory(
        main = inner.inventory.items.map { it.toCombine() }.toPersistentList(),
        armor = inner.inventory.armor.map { it.toCombine() }.toPersistentList(),
        offHand = inner.inventory.offhand.first().toCombine(),
    )

    override val ridingEntityType: RidingEntityType?
        get() = when (inner.vehicle) {
            null -> null
            is Minecart -> RidingEntityType.MINECART
            is Boat -> RidingEntityType.BOAT
            is Pig -> RidingEntityType.PIG
            is Camel -> RidingEntityType.CAMEL
            is Horse, is Donkey, is Mule, is ZombieHorse, is SkeletonHorse -> RidingEntityType.HORSE
            is Llama -> RidingEntityType.LLAMA
            is Strider -> RidingEntityType.STRIDER
            else -> RidingEntityType.OTHER
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PlayerHandleImpl

        return inner == other.inner
    }

    override fun hashCode(): Int = inner.hashCode()
}

object PlayerHandleFactoryImpl : PlayerHandleFactory {
    private val client = Minecraft.getInstance()

    override fun getPlayerHandle(): PlayerHandle? = client.player?.let(::PlayerHandleImpl)
}