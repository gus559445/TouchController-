package top.fifthlight.touchcontroller.common.config

import kotlinx.serialization.Serializable
import top.fifthlight.combine.paint.Color
import top.fifthlight.combine.paint.Colors
import top.fifthlight.touchcontroller.common.config.preset.PresetConfig
import top.fifthlight.touchcontroller.common.gal.DefaultItemListProvider

/** 吃饱收手「持续时间」滑块的最大值：拉满表示「一直收手，直到下次长按」。 */
const val MAX_EAT_STOP_DURATION = 100

@Serializable
data class RegularConfig(
    val disableMouseMove: Boolean = true,
    val disableMouseClick: Boolean = true,
    val disableMouseLock: Boolean = false,
    val disableHotBarKey: Boolean = false,
    val vibration: Boolean = true,
    val quickHandSwap: Boolean = false,
)

@Serializable
data class ControlConfig(
    val viewMovementSensitivity: Float = 495f,
    val viewHoldDetectThreshold: Int = 2,
    val viewHoldDetectTicks: Int = 5,
)

@Serializable
data class TouchRingConfig(
    val radius: Int = 36,
    val outerRadius: Int = 2,
    val initialProgress: Float = .5f
)

@Serializable
data class DebugConfig(
    val showPointers: Boolean = false,
    val enableTouchEmulation: Boolean = false,
)

@Serializable
data class ItemConfig(
    val usableItems: ItemList,
    val showCrosshairItems: ItemList,
    val crosshairAimingItems: ItemList,
    /** 「长按用不了改挖方块」的例外清单：命中的物品长按用不出去时**不**改判挖方块。默认金苹果、附魔金苹果、药水。 */
    val longPressDigItems: ItemList = ItemList(),
    /** 自定义盾牌白名单：命中的物品一律当盾牌对待——潜行时自动举盾，右键依旧永不举盾。 */
    val shieldItems: ItemList = ItemList(),
) {
    companion object {
        fun default(itemListProvider: DefaultItemListProvider) = ItemConfig(
            usableItems = itemListProvider.usableItems,
            showCrosshairItems = itemListProvider.showCrosshairItems,
            crosshairAimingItems = itemListProvider.crosshairAimingItems,
            longPressDigItems = itemListProvider.longPressDigItems,
            shieldItems = ItemList(),
        )
    }
}

@Serializable
data class ChatConfig(
    val lineSpacing: Int = 0,
    val textColor: Color = Colors.WHITE,
)

/**
 * 「增强功能」开关：把原本要另装两个模组（携带版准星、潜行举盾）的玩法直接做进 TouchController。
 * 默认开启，想要原版手感可以在设置里逐项关掉，各开关互不影响。
 */
@Serializable
data class TweaksConfig(
    /** 蓄力准星：拉弓 / 给弩上弦 / 举三叉戟蓄力、以及弩已装弹待发时，显示原版准星并暂时隐去触控圈。 */
    val chargeCrosshair: Boolean = true,
    /** 潜行举盾：潜行且手上拿着盾牌时自动举盾，站起来、动手挖方块或攻击时自动放下。 */
    val sneakShield: Boolean = true,
    /** 长按改判：长按方块时手上东西根本用不出去（吃饱还拿食物、没弹药的弓弩），改判成挖方块；
     *  拿着能吃/能蓄力的东西长按方块时，优先吃东西/拉弓，不被方块交互抢走。 */
    val longPressDig: Boolean = true,
    /** 吃饱收手：一口普通食物把饥饿值吃到顶时，把这次触摸长按整段锁掉——手指不抬起来就什么都做不了，
     *  不让吃饱后还按着的手顺势变成攻击或挖方块。 */
    val eatStop: Boolean = true,
    /** 吃饱收手的「黑名单」：清单里的物品不参与收手，吃饱了也继续按着吃/用（不改判挖方块）。
     *  这里存的是 [ItemList] 的白名单部分——「命中 = 跳过收手」，界面上叫它黑名单。 */
    val eatStopKeepItems: ItemList = ItemList(),
    /** 收手持续时间（渲染帧）。0 = 只收当前这一口（手指抬起即解除，原行为）；
     *  1..MAX-1 = 收手 N 帧后自动解除；[MAX_EAT_STOP_DURATION]（拉满）= 一直收手，直到手指抬起。 */
    val eatStopDuration: Int = 0,
    /** 四周放置：低头站在方块边缘、手里拿方块时，点前方空地会贴着自己脚边放一块（基岩版手感）。 */
    val reachAround: Boolean = true,
    /** 六格交互距离：方块和实体都能从 6 格远够到（原版方块 4.5、实体 3）。 */
    val extraReach: Boolean = true,
    /** 盾牌挡下近战后，把攻击者被弹开的距离减弱到约一格。 */
    val shieldKnockback: Boolean = true,
    /** 副手有物品时，主手弩的第一人称绘制保持「未装填」的样子，并把副手画回来。 */
    val crossbowOffhandRender: Boolean = true,
    /** 举盾时移动不再和潜行减速叠加（仍保留潜行本身的减速）。 */
    val shieldSpeed: Boolean = true,
)

@Serializable
data class GlobalConfig(
    val regular: RegularConfig = RegularConfig(),
    val control: ControlConfig = ControlConfig(),
    val touchRing: TouchRingConfig = TouchRingConfig(),
    val debug: DebugConfig = DebugConfig(),
    val item: ItemConfig,
    val preset: PresetConfig = PresetConfig.BuiltIn(),
    val chat: ChatConfig = ChatConfig(),
    val tweaks: TweaksConfig = TweaksConfig(),
) {
    companion object {
        fun default(itemListProvider: DefaultItemListProvider) = GlobalConfig(
            item = ItemConfig.default(itemListProvider),
        )
    }
}
