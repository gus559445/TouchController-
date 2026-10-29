package top.fifthlight.touchcontroller.common.helper

import top.fifthlight.touchcontroller.common.config.MAX_EAT_STOP_DURATION
import top.fifthlight.touchcontroller.common.config.TweaksConfig
import top.fifthlight.touchcontroller.common.gal.TweakStateProvider

/**
 * 「吃饱收手」：一口普通食物把饥饿值吃到顶的瞬间，**只封掉动作**，不封视角。
 *
 * <h3>要解决的问题</h3>
 * 吃东西是靠按住屏幕持续进行的。饥饿值回满的那一刻原版会自己结束进食，但手指往往还按着；
 * 这条残留的长按会顺手变成攻击或挖方块（不同触摸方案的表现略有差别）。
 *
 * <h3>收手原理：强制把「长按使用」变成「无任何操作」，但允许滑动视角</h3>
 * 本类只维护一个开关 [isHandsLocked]。锁合上期间，
 * [top.fifthlight.touchcontroller.common.layout.View] 会：
 * <ul>
 *   <li><b>照常</b>把手指位移换算成视角转动（[top.fifthlight.touchcontroller.common.layout.Context.lookDirection]）；</li>
 *   <li><b>不</b>挖方块、<b>不</b>攻击、<b>不</b>用物品——长按彻底变成「按着看四周」。</li>
 * </ul>
 * 这样玩家吃完最后一口还能自由环顾，只是这一下按着不会误触发任何动作。
 *
 * <h3>锁开多久（[TweaksConfig.eatStopDuration]）</h3>
 * <ul>
 *   <li><b>0</b>（默认）：只收当前这一口——锁一直合着，直到手指抬起（原行为）；</li>
 *   <li><b>1 .. MAX-1</b>：从吃饱那一刻起收手 N 个渲染帧，到点自动解除（手指还按着也能继续操作）；</li>
 *   <li><b>[MAX_EAT_STOP_DURATION]（滑块拉满）</b>：一直收手，直到手指抬起才解除。</li>
 * </ul>
 *
 * <h3>哪些情况不算「吃饱」</h3>
 * <ul>
 *   <li>还没吃到满：不是为填肚子，正常结束，不收手；</li>
 *   <li>金苹果这类「满饥饿也能吃」的食物：玩家本来就打算接着吃，不收手；</li>
 *   <li>黑名单（[TweaksConfig.eatStopKeepItems]）里的物品：玩家点名要它接着吃，不收手；</li>
 *   <li>不是吃（拉弓、喝药水、举盾）：完全不参与。</li>
 * </ul>
 */
object EatStopHandler {
    /** 上一帧是不是正在吃。用来捕捉「吃 → 停」的那一帧，也就是这一口结束的瞬间。 */
    private var wasEating = false

    /** 上一帧正在吃的那口，是不是「靠回饥饿值、而且不在黑名单里」的普通食物。 */
    private var wasFilling = false

    /** 收手锁：合上以后，这段长按只剩「看四周」，不产生任何动作。 */
    private var handsLocked = false

    /**
     * 自动解除倒计时（渲染帧）。
     * -1 = 不按帧数解除（等手指抬起 / 由 View 调 [unlock]）；
     * 0 = 当帧就解除；>0 = 还剩这么多帧。
     */
    private var framesLeft = -1

    /** 每渲染帧调用一次（在 Hud 处理触摸之前）。 */
    fun update(config: TweaksConfig, state: TweakStateProvider) {
        if (!config.eatStop) {
            reset()
            return
        }

        // 先推进倒计时：到点了就自动开锁（手指还按着也不影响接下来重新操作）。
        if (handsLocked && framesLeft > 0) {
            framesLeft--
            if (framesLeft == 0) {
                handsLocked = false
            }
        }

        if (state.isEatingFood()) {
            wasEating = true
            // 黑名单里的物品吃饱了也要接着吃：这一口不算「该收手」。
            wasFilling = state.isEatingFillingFood() && !state.isActiveItemIn(config.eatStopKeepItems)
            return
        }
        if (wasEating) {
            // 这一帧从「在吃」变成了「没在吃」：如果刚才那口是填肚子的食物，而且现在已经吃饱，
            // 那就是「吃完最后一口」的那一刻，合上收手锁。
            wasEating = false
            if (wasFilling && state.isFoodLevelFull()) {
                handsLocked = true
                // 0 → 一直收到手指抬起；拉满 → 也是一直收；中间值 → 收 N 帧。
                framesLeft = when {
                    config.eatStopDuration <= 0 -> -1
                    config.eatStopDuration >= MAX_EAT_STOP_DURATION -> -1
                    else -> config.eatStopDuration
                }
            }
            wasFilling = false
        }
    }

    /** 现在是不是处于「收手期间」：这期间挖方块、攻击、用物品全部禁用，但视角照常转动。 */
    fun isHandsLocked(): Boolean = handsLocked

    /** 手指都离开屏幕了：开锁，下一次按下照常。 */
    fun unlock() {
        handsLocked = false
        framesLeft = -1
    }

    /** 退出世界 / 关闭功能时清干净。 */
    fun reset() {
        wasEating = false
        wasFilling = false
        handsLocked = false
        framesLeft = -1
    }
}
