package top.fifthlight.touchcontroller.gal

import top.fifthlight.touchcontroller.common.gal.TweakActionProvider
import top.fifthlight.touchcontroller.tweak.TweakUseExecutor

/**
 * 1.20.1 Forge 上「增强功能」要动到原版操作的那几个动作。
 *
 * <p>common 层只声明「要做什么」，真正碰 {@code Minecraft#gameMode}、准星命中、
 * 四周放置的实现都在 Java 的 [TweakUseExecutor] 里；这个类只做一层转交，
 * 免得 common 层的接口被迫引入 Minecraft 的类。</p>
 */
object TweakActionProviderImpl : TweakActionProvider {
    override fun canReachAroundPlace(): Boolean = TweakUseExecutor.canReachAroundPlace()

    override fun performUseClick(reachAround: Boolean) = TweakUseExecutor.performUseClick(reachAround)

    override fun performAttackClick() = TweakUseExecutor.performAttackClick()

    override fun releaseShieldForClick() = TweakUseExecutor.releaseShieldForClick()

    override fun startUsingHeldItem() = TweakUseExecutor.startUsingHeldItem()

    override fun swingArm() = TweakUseExecutor.swingArm()

    override fun raiseShield() = TweakUseExecutor.raiseShield()

    override fun lowerShield() = TweakUseExecutor.lowerShield()

    override fun clearUseClicks() = TweakUseExecutor.clearUseClicks()

    override fun clearAttackClicks() = TweakUseExecutor.clearAttackClicks()
}
