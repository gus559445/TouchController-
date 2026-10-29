package top.fifthlight.touchcontroller.common.helper

import top.fifthlight.touchcontroller.common.config.TweaksConfig
import top.fifthlight.touchcontroller.common.gal.TweakStateProvider

/**
 * 「蓄力准星」这一项当前该不该让原版准星亮着。
 *
 * <p>规则和原来的「携带版准星」模组一致：</p>
 * <ul>
 *   <li><b>手上拿着可蓄力武器</b>（弓、弩、三叉戟这类）但还没开始蓄力 —— 准星不显示，
 *       这时候瞄准参照应该是手指底下的触控圈；</li>
 *   <li><b>正在蓄力</b>（拉弓、上弩弦、举三叉戟），或者<b>弩已经装弹待发</b> —— 准星显示，
 *       同时触控圈让位（见 layout/Crosshair）；</li>
 *   <li>手上没有可蓄力武器 —— 返回 null，交回给 TouchController 原本的准星规则。</li>
 * </ul>
 *
 * <p>返回 {@code null} 表示「本功能不表态」，让调用方继续走原来的判断。</p>
 */
fun chargeCrosshairOverride(config: TweaksConfig, tweakState: TweakStateProvider): Boolean? {
    if (!config.chargeCrosshair) {
        return null
    }
    if (tweakState.isChargingChargeableItem() || tweakState.hasLoadedCrossbow()) {
        return true
    }
    if (tweakState.holdsChargeableItem()) {
        return false
    }
    return null
}

/**
 * 触控圈该不该让位：只有在「蓄力准星」明确要让准星亮起来的时候，圆环才藏起来。
 * 和原版准星那边读同一份判断，两边不会一个亮一个不亮。
 */
fun shouldHideTouchRing(config: TweaksConfig, tweakState: TweakStateProvider): Boolean =
    chargeCrosshairOverride(config, tweakState) == true
