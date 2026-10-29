package top.fifthlight.touchcontroller.tweak

import net.minecraft.client.player.LocalPlayer
import net.minecraft.tags.DamageTypeTags
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.UseAnim
import net.minecraftforge.client.event.MovementInputUpdateEvent
import net.minecraftforge.event.entity.living.LivingKnockBackEvent
import net.minecraftforge.event.entity.living.ShieldBlockEvent
import java.util.UUID

/**
 * 盾牌的两项手感修正（对应设置里的「盾牌击退」和「举盾减速」两个开关）。
 *
 * <h3>① 盾牌挡下近战后，把攻击者被弹开的距离削弱到约一格（[PUSH_STRENGTH]）</h3>
 * 原版挡下近战攻击后会执行 {@code attacker.knockback(0.5, dx, dz)}，能把人滑开四五格。
 * 这里监听 [ShieldBlockEvent] 把攻击者记下来，等紧跟着到来的 [LivingKnockBackEvent]
 * 把这一下强度改成 0.3——明显变弱但仍有推击感。弹射物（箭）原版本就不弹返，不处理。
 *
 * <h3>② 举盾减速不与潜行减速叠加</h3>
 * 原版客户端在「潜行」和「正在使用物品（含举盾）」时会对移动输入分别乘 0.3 和 0.2，
 * 两者叠加只剩 6% 速度，站着举盾走路慢得像卡住。
 *
 * <p><b>不能</b>去改 {@code MOVEMENT_SPEED} 属性：原版 {@code AbstractClientPlayer#getFieldOfViewModifier}
 * 直接拿属性值算 FOV，改属性会让视角随举盾一起缩放。正确做法是在 [MovementInputUpdateEvent] 里
 * 把输入冲量乘 5，正好抵消原版 {@code LocalPlayer#aiStep} 之后会乘的那个 ×0.2，
 * 净效果等于只受潜行减速（30%），视角完全不变。服务端没有这条输入缩放，无需补偿。</p>
 */
object ShieldTweaks {
    /** 弹返击退强度：原版 0.5 约四五格；这里 0.7 明显弹得更开，挡一下就推得动近战怪。 */
    private const val PUSH_STRENGTH = 0.7f

    /** 本 tick 内刚被盾牌挡下近战的攻击者（等弹返击退到来时削弱一次，消费即移除）。 */
    private val pushedAttackers: MutableSet<UUID> = HashSet()

    /** 等到 tick 末尾再推开的攻击者（方向已经算好）。 */
    private val pendingPush: MutableList<PendingPush> = ArrayList()

    private class PendingPush(
        val attacker: LivingEntity,
        val nx: Double,
        val nz: Double,
    )

    // ------------------------------------------------------------------
    // ① 弹返击退削弱
    // ------------------------------------------------------------------

    /**
     * @param enabled 设置里的「盾牌击退」开关
     */
    fun onShieldBlock(event: ShieldBlockEvent, enabled: Boolean) {
        if (!enabled) {
            return
        }
        val player = event.entity as? Player ?: return
        if (player.level().isClientSide) {
            return
        }
        // 弹射物原版本就不弹返攻击者，不标记。
        if (event.damageSource.`is`(DamageTypeTags.IS_PROJECTILE)) {
            return
        }
        val attacker = event.damageSource.directEntity as? LivingEntity ?: return
        pushedAttackers.add(attacker.uuid)
        // 记下方向，下一 tick 再施加（见 onServerTick）。
        // 为什么不用 attacker.knockback(...)：它内部走 LivingKnockBackEvent，
        // 而「推力抵抗 / 战斗机制」这类模组正是监听那个事件把击退清零或改弱的；
        // 直接改速度又会被紧随其后的原版 knockback(0.5) 覆盖，所以必须晚一 tick。
        val dx = attacker.x - player.x
        val dz = attacker.z - player.z
        val horizontal = kotlin.math.sqrt(dx * dx + dz * dz)
        pendingPush.add(
            PendingPush(
                attacker,
                if (horizontal > 1.0E-4) dx / horizontal else 0.0,
                if (horizontal > 1.0E-4) dz / horizontal else 0.0,
            )
        )
    }

    /**
     * 服务器 tick 末尾：把上一 tick 记下的攻击者真正推出去。
     *
     * <p>放在 tick 末尾是为了「最后写入」——原版格挡后的 knockback(0.5) 和别的模组
     * 对击退的修改都在这一 tick 内更早发生，这里写进去的速度不会再被它们盖掉。</p>
     */
    fun onServerTick() {
        if (pendingPush.isEmpty()) {
            return
        }
        for (push in pendingPush) {
            val attacker = push.attacker
            if (!attacker.isAlive) {
                continue
            }
            attacker.deltaMovement = attacker.deltaMovement.add(
                push.nx * PUSH_STRENGTH,
                0.35,
                push.nz * PUSH_STRENGTH,
            )
            attacker.hasImpulse = true
        }
        pendingPush.clear()
    }

    fun onKnockBack(event: LivingKnockBackEvent) {
        // 标记单次消费：只削弱「盾牌挡下」引发的那一次击退。
        if (pushedAttackers.remove(event.entity.uuid)) {
            event.strength = PUSH_STRENGTH
        }
    }

    // ------------------------------------------------------------------
    // ② 举盾减速不叠加
    // ------------------------------------------------------------------

    /**
     * @param enabled 设置里的「举盾减速」开关
     */
    fun onMovementInput(event: MovementInputUpdateEvent, enabled: Boolean) {
        if (!enabled) {
            return
        }
        val player = event.entity as? LocalPlayer ?: return
        if (!player.isCrouching || !isUsingShield(player)) {
            return
        }
        val input = event.input ?: return
        // 抵消原版「正在使用物品 ×0.2」的输入缩放（见类注释）。
        input.leftImpulse *= 5.0f
        input.forwardImpulse *= 5.0f
    }

    // ------------------------------------------------------------------
    // 内部辅助
    // ------------------------------------------------------------------

    private fun isUsingShield(player: Player): Boolean =
        player.isUsingItem && isShield(player.useItem)

    /** 是不是盾牌：原版盾认物品 id，模组盾只要照抄原版的 BLOCK 使用动画也能认出来。 */
    private fun isShield(stack: ItemStack): Boolean =
        !stack.isEmpty && (stack.item == Items.SHIELD || stack.useAnimation == UseAnim.BLOCK)
}
