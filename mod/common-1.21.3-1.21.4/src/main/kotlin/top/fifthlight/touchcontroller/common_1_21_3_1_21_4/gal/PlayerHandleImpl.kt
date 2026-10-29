package top.fifthlight.touchcontroller.common_1_21_3_1_21_4.gal

import kotlinx.collections.immutable.toPersistentList
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import top.fifthlight.combine.platform_1_21_3_1_21_5.toCombine
import top.fifthlight.touchcontroller.common.gal.PlayerHandle
import top.fifthlight.touchcontroller.common.gal.PlayerHandleFactory
import top.fifthlight.touchcontroller.common.gal.PlayerInventory
import top.fifthlight.touchcontroller.common_1_21_3_1_21_5.gal.AbstractPlayerHandleImpl

class PlayerHandleImpl(
    inner: LocalPlayer,
) : AbstractPlayerHandleImpl(inner) {
    override var currentSelectedSlot: Int
        get() = inner.inventory.selected
        set(value) {
            inner.inventory.selected = value
        }

    override fun getInventory() = PlayerInventory(
        main = inner.inventory.items.map { it.toCombine() }.toPersistentList(),
        armor = inner.inventory.armor.map { it.toCombine() }.toPersistentList(),
        offHand = inner.inventory.offhand.first().toCombine(),
    )
}

object PlayerHandleFactoryImpl : PlayerHandleFactory {
    private val client = Minecraft.getInstance()

    override fun getPlayerHandle(): PlayerHandle? = client.player?.let(::PlayerHandleImpl)
}