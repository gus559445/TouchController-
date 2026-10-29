package top.fifthlight.touchcontroller.common.helper

import top.fifthlight.touchcontroller.common.config.TweaksConfig
import top.fifthlight.touchcontroller.common.gal.DefaultKeyBindingType
import top.fifthlight.touchcontroller.common.gal.KeyBindingHandler
import top.fifthlight.touchcontroller.common.gal.PlayerHandle
import top.fifthlight.touchcontroller.common.gal.TweakAction
import top.fifthlight.touchcontroller.common.gal.TweakState

/**
 * 「潜行举盾」：潜行且手上有盾牌时自动格挡，站起来或要动手时放下。
 *
 * <h3>举盾怎么驱动</h3>
 * 直接走原版 {@code GameMode#useItem(盾所在那只手)}：数据包、服务端同步、格挡计时、
 * 音效、动画全部交给原版，模组不自己维护任何格挡状态。
 *
 * <p>因为是指定「盾的手」而不是靠按住使用键，所以主手拿着食物时也会正确举盾
 * （原版「开始使用」只会用主手，食物会抢在前面）。</p>
 *
 * <h3>为什么要屏蔽原版的「强制收盾」</h3>
 * 原版的举盾完全依赖「使用键按住」这个状态：一旦发现键没按着，它每个 tick 都会主动
 * {@code releaseUsingItem}。于是「自己调 useItem 举盾」会变成「举一帧、掉一帧」——
 * 盾一直抽搐，服务端也从没收到过稳定的格挡状态（表现就是没有格挡效果）。
 *
 * <p>解决办法：版本层 Mixin 在 {@code MultiPlayerGameMode#releaseUsingItem} 里，
 * 对「[isShieldUp] 为真 且 正在用盾」的释放直接取消。我们自己要收盾时先把
 * [isShieldUp] 置回 false，释放就能正常通过。</p>
 *
 * <h3>和右键彻底分开</h3>
 * <ul>
 *   <li>举盾只由本状态机决定，**玩家自己右键 / 长按不再举盾**
 *       （右键使用盾牌在 Mixin 里被吞掉；拿着盾长按会被改判成挖方块）；</li>
 *   <li>举盾期间要点击方块 / 生物：View 先调 [release] 收盾，再自己代执行那一下点击；</li>
 *   <li>攻击、拉弓、站起来、开界面都会让路给它们（盾自动放下，结束后自动重举）。</li>
 * </ul>
 */
object SneakShieldHandler {

    /** 盾此刻是不是举着的（我们自己维护，避免重复判断）。 */
    private var isRaised = false

    /**
     * 立刻收盾。
     *
     * 触控圈要动手（挖方块、点生物、放方块）时会先叫一下这个，
     * 免得下一帧状态机又把盾举起来，把这一下动作盖掉。
     *
     * <p>先把 [isRaised] 置回 false 再真正释放——Mixin 里「屏蔽原版强制收盾」的判断
     * 就是看 [isShieldUp]，先复位才能让我们自己发起的释放通过。</p>
     */
    fun release(keyBindingHandler: KeyBindingHandler) {
        isRaised = false
        TweakAction.provider.lowerShield()
    }

    /** View.kt 单击路径要问：盾现在是不是举着的？举着就先收盾再代执行点击，避免被原版吞掉。 */
    fun isShieldUp(): Boolean = isRaised

    /**
     * 渲染帧更新一次举盾状态。
     *
     * @param config             全局设置（读 tweaks.sneakShield）
     * @param player             本地玩家，进世界前为 null
     * @param keyBindingHandler  TouchController 的按键状态表
     * @param renderTick         当前渲染帧号，用来给锁续命
     * @param inGui              是否打开了界面（背包、箱子、聊天…）
     */
    fun update(
        config: TweaksConfig,
        player: PlayerHandle?,
        keyBindingHandler: KeyBindingHandler,
        renderTick: Int,
        inGui: Boolean,
    ) {
        val attackState = keyBindingHandler.getState(DefaultKeyBindingType.ATTACK)
        // 攻击优先：手指按住屏幕挖方块 / 点了一下要攻击，就先收盾让路；攻击一结束立刻重新举。
        val attacked = attackState.locked || attackState.haveClickCount()

        val useState = keyBindingHandler.getState(DefaultKeyBindingType.USE)
        // 「用」优先：手指正在用物品（吃东西、拉弓、放方块 / 开箱子那一下），
        // 这一下按着必须是「用」，不能拿盾去顶——盾一顶，原版看到「正在使用物品」
        // 就把这次点击整批丢掉，表现就是吃不了东西、方块放不出去。
        // 用完（手指抬起 / 锁清掉）之后自动重新举盾。
        val using = useState.locked || useState.haveClickCount()

        // 拿弓就是换战斗姿态：手上有没有「现在正在拉/射」的弓弩？
        // 有的话让位给拉弓，别用盾把弓顶掉（放下弓 / 没箭了会自动恢复举盾）。
        val bowCharging = TweakState.provider.isChargingChargeableItem()

        // 正在吃 / 拉弓 / 用别的物品时让路：不抢，等这一口 / 这一箭结束再自动举盾。
        // （自己举着的盾不算打扰——isRaised 为真时正在用的就是盾。）
        val usingOther = player != null && player.isUsingItem && !isRaised

        val shouldRaise =
            config.sneakShield &&
                !inGui &&
                (player != null) &&
                player.isSneaking &&
                !attacked &&
                !using &&
                !bowCharging &&
                !usingOther &&
                TweakState.provider.hasShieldOnHand()

        if (shouldRaise) {
            // 主动走原版 useItem 管道把盾举起来（指定盾所在那只手，所以主手是食物也不影响）。
            // 原版「没按着使用键就强制收盾」那一下，由 Mixin 在 releaseUsingItem 里屏蔽掉，
            // 所以这里不需要再靠按键锁维持——格挡是真正稳定生效的。
            // isRaised 必须先于 raiseShield 置位：Mixin 豁免 / useItem 放行都看它。
            isRaised = true
            TweakAction.provider.raiseShield()
        } else if (isRaised) {
            isRaised = false
            TweakAction.provider.lowerShield()
        } else if (player != null && player.isUsingItem) {
            // 没在潜行自动举盾，但玩家自己按出了格挡（右键 / 长按拿盾）：
            // 立刻收掉——举盾统一由潜行接管，不再支持手动举盾。
            // （lowerShield 只对「正在用的东西是盾」生效，吃东西 / 拉弓不受影响。）
            TweakAction.provider.lowerShield()
        }
    }

    /** 退出世界 / 关闭功能时清干净（按键状态本身会随场景重建，这里清一下自己的标记）。 */
    fun reset() {
        isRaised = false
    }
}
