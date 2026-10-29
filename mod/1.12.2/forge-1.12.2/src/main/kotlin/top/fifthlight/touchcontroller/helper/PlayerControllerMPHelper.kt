@file:Suppress("unused")

package top.fifthlight.touchcontroller.helper

import net.minecraft.client.multiplayer.PlayerControllerMP
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.util.EnumHand
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import top.fifthlight.combine.platform.toCombine
import top.fifthlight.touchcontroller.common.config.GlobalConfigHolder
import top.fifthlight.touchcontroller.common.helper.CrosshairTargetHelper

object PlayerControllerMPHelper : KoinComponent {
    private val globalConfigHolder: GlobalConfigHolder by inject()

    private var prevYaw: Float = 0f
    private var prevPitch: Float = 0f
    private var resetPlayerLookTarget = false

    @JvmStatic
    fun beforeUsingItem(controller: PlayerControllerMP, player: EntityPlayer, hand: EnumHand) {
        val itemStack = player.getHeldItem(hand)
        val crosshairAimingItems = globalConfigHolder.config.value.item.crosshairAimingItems
        if (itemStack.item.toCombine() !in crosshairAimingItems) {
            return
        }

        prevYaw = player.rotationYaw
        prevPitch = player.rotationPitch

        val (yaw, pitch) = CrosshairTargetHelper.calculatePlayerRotation(CrosshairTargetHelper.lastCrosshairDirection)
        player.rotationYaw = yaw
        player.rotationPitch = pitch
        controller.connection.sendPacket(CPacketPlayer.Rotation(yaw, pitch, player.onGround))
        resetPlayerLookTarget = true
    }

    @JvmStatic
    fun afterUsingItem(controller: PlayerControllerMP, player: EntityPlayer) {
        if (!resetPlayerLookTarget) {
            return
        }
        resetPlayerLookTarget = false
        player.rotationYaw = prevYaw
        player.rotationPitch = prevPitch
        controller.connection.sendPacket(CPacketPlayer.Rotation(player.cameraYaw, player.cameraPitch, player.onGround))
    }
}