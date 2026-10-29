package top.fifthlight.touchcontroller.common_1_20_x.gal

import net.minecraft.client.Minecraft
import net.minecraft.world.phys.HitResult
import top.fifthlight.touchcontroller.common.gal.CrosshairLock
import top.fifthlight.touchcontroller.common.gal.CrosshairTarget
import top.fifthlight.touchcontroller.common.gal.ViewActionProvider
import top.fifthlight.touchcontroller.helper.GameModeWithBreakingProgress

object ViewActionProviderImpl : ViewActionProvider {
    private val client = Minecraft.getInstance()

    override fun getCrosshairTarget(): CrosshairTarget? {
        // 有「瞄准指定实体」的锁定时优先用它：松手后触摸圈已经没了，
        // 图层按钮必须作用在松手前瞄到的那个目标上，不能跟着当前准星跑。
        CrosshairLock.lockedTarget()?.let { return it }
        val target = client.hitResult ?: return null
        return when (target.type) {
            HitResult.Type.ENTITY -> CrosshairTarget.ENTITY
            HitResult.Type.BLOCK -> CrosshairTarget.BLOCK
            HitResult.Type.MISS -> CrosshairTarget.MISS
            else -> return null
        }
    }

    /** 实时准星指着的实体 UUID（不看瞄准锁）。 */
    override fun getCrosshairEntityUuid(): java.util.UUID? = client.crosshairPickEntity?.uuid

    /** 准星指着的方块有没有方块实体（箱子/工作台/熔炉…＝可交互）。 */
    override fun isTargetBlockInteractive(): Boolean {
        val blockHit = client.hitResult as? net.minecraft.world.phys.BlockHitResult ?: return false
        val level = client.level ?: return false
        return level.getBlockState(blockHit.blockPos).hasBlockEntity()
    }

    override fun getCurrentBreakingProgress(): Float {
        val manager = client.gameMode
        val accessor = manager as GameModeWithBreakingProgress
        return accessor.`touchcontroller$getBreakingProgress`()
    }
}