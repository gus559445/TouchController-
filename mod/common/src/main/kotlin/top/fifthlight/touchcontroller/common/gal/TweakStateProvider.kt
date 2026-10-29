package top.fifthlight.touchcontroller.common.gal

import top.fifthlight.touchcontroller.common.config.ItemList

/**
 * 「增强功能」（见 [top.fifthlight.touchcontroller.common.config.TweaksConfig]）需要的玩家状态判定。
 *
 * common 层是跨平台共用的，碰不到 Minecraft 自己的类（物品、弓弩、盾牌都在版本层），
 * 所以这里只声明「要问哪些事」，由各版本实现层给出回答。
 * 默认实现 [EmptyTweakStateProvider] 一律返回 false，没实现的平台上功能自然处于关闭状态。
 */
interface TweakStateProvider {
    /** 主手或副手拿着可蓄力的武器（弓、弩、三叉戟这类），还没开始蓄力也算。 */
    fun holdsChargeableItem(): Boolean

    /** 玩家正在使用可蓄力的武器：拉着弓、给弩上弦、举着三叉戟准备投掷。 */
    fun isChargingChargeableItem(): Boolean

    /** 主手或副手拿着已经上好弦的弩（装弹完成，一按就能射）。 */
    fun hasLoadedCrossbow(): Boolean

    /** 主手或副手拿着盾牌（潜行举盾的前提）。 */
    fun hasShieldOnHand(): Boolean

    /**
     * 主手拿着盾牌。
     *
     * <p>长按时「用物品」先打到主手，所以只有主手是盾才需要拦一下（不让长按去举盾）；
     * 副手放盾不该影响主手正常吃东西 / 用东西。</p>
     */
    fun hasShieldInMainHand(): Boolean

    /**
     * 主手或副手有没有「现在就能蓄力 / 射击」的弓弩（弓有箭、弩已上弦或有箭、三叉戟可直接蓄力）。
     *
     * <p>拿弓就是换战斗姿态：这时候要让位给拉弓，不能一直举着盾把弓顶掉。</p>
     */
    fun hasUsableChargeableWeapon(): Boolean

    /**
     * 手上（主手或副手）的东西此刻能不能真的「用出去」：
     * 食物得吃得下、弓弩得有弹药或已经上好弦。返回 false 时，长按方块要改判成挖方块。
     * 拿不准的物品（模组武器等）返回 true，交给「长按 0.3 秒还没开始使用」的兜底判定。
     */
    fun canUseHeldItemNow(): Boolean

    /** 玩家正在使用「吃」这种使用动画的物品（面包、熟肉等一切食物；只是拿在手上不算）。 */
    fun isEatingFood(): Boolean

    /** 正在吃的这一口是「靠回饥饿值」的普通食物（营养大于 0，且不是金苹果这类满饥饿也能吃的）。 */
    fun isEatingFillingFood(): Boolean

    /** 饥饿值是不是已经到顶（10 颗心 = 20 点）。 */
    fun isFoodLevelFull(): Boolean

    /**
     * 手上这件「要用的东西」是不是在 [list] 里。
     *
     * 正在使用物品时看的是那只手（玩家真正在吃/在用的那件），否则看主手。
     * 「吃饱收手」的黑名单靠它判断：命中的物品不参与收手，也照旧当成「能接着用」。
     */
    fun isActiveItemIn(list: ItemList): Boolean

    /**
     * 手上这件东西是否「长按用不出去也不改判挖方块」。
     *
     * <p>[list] 命中即豁免（界面叫它黑名单）。[list] 完全留空时使用内置默认：
     * 金苹果、附魔金苹果、三种药水——它们本来就该「吃饱了 / 用不了也按着用」，
     * 这样老配置（没有这个字段）也能得到同样的默认保护。</p>
     */
    fun isLongPressDigExempt(list: ItemList): Boolean

    /**
     * 手上（主手或副手）是不是「现在就能吃下去的食物」：可食用 + 饥饿值没满。
     *
     * <p>这一条独立于「可长按使用的物品」清单：只要现在吃得下，长按就必须是进食，
     * 绝不能被改判成挖方块——玩家还没吃饱，东西明明是能用的。</p>
     */
    fun isHeldFoodEdibleNow(): Boolean

    /**
     * 主手这件「可长按使用的物品」是否在 [list] 里（只看主手，且**盾牌一律不算**）。
     *
     * <p>为什么只看主手：长按方块时原版先动主手，副手那件根本轮不到。</p>
     *
     * <p>为什么盾牌要内置黑名单：右键举盾由潜行自动接管，盾牌要是还留在「可长按使用」
     * 清单里，主手拿方块、副手拿盾的这种搭配长按就会被判成「用物品」，
     * 于是先按右键把主手的方块放出去一次，然后才改判挖方块。</p>
     */
    /**
     * 主手物品是否在清单的**白名单**里（不算子类 / 组件命中）。
     *
     * <p>用来判断「点生物这一下该用物品还是该攻击」：白名单里点名的都是右键交互型道具
     * （戎卫号角、驭兽哨、末影珍珠、药水…），拿着它们点生物应该是「用」，不是「打」。</p>
     */
    fun isHeldItemInWhitelist(list: ItemList): Boolean

    fun hasUsableItemOnMainHand(list: ItemList): Boolean

    /**
     * 主手拿着「能长按使用」的物品（弓、三叉戟、望远镜、月光蠕虫女王…）。
     *
     * <p>这类物品长按永远优先「用」，压过挖方块和所有其它判定——
     * 否则它会被当成普通物品，长按变成挖/放方块。</p>
     */
    fun isLongPressUsableItem(): Boolean
}

object EmptyTweakStateProvider : TweakStateProvider {
    override fun holdsChargeableItem() = false

    override fun isChargingChargeableItem() = false

    override fun hasLoadedCrossbow() = false

    override fun hasShieldOnHand() = false

    override fun hasShieldInMainHand() = false

    override fun hasUsableChargeableWeapon() = false

    override fun canUseHeldItemNow() = true

    override fun isEatingFood() = false

    override fun isEatingFillingFood() = false

    override fun isFoodLevelFull() = false

    override fun isActiveItemIn(list: ItemList) = false

    override fun isLongPressDigExempt(list: ItemList) = false

    override fun isHeldFoodEdibleNow() = false

    override fun isHeldItemInWhitelist(list: ItemList) = false

    override fun hasUsableItemOnMainHand(list: ItemList) = false

    override fun isLongPressUsableItem() = false
}

/**
 * 当前生效的实现。
 *
 * 不用依赖注入的「同类型覆盖」玩法：各版本模块在加载时直接把真实实现装进来，
 * 没装的版本就一直是空实现。取值只是一次普通字段读取，渲染帧里调用也没有开销。
 */
object TweakState {
    @Volatile
    var provider: TweakStateProvider = EmptyTweakStateProvider
}
