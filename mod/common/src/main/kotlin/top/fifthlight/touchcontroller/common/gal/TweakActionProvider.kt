package top.fifthlight.touchcontroller.common.gal

/**
 * 「增强功能」需要真正动到原版操作的那几个动作。
 *
 * <p>common 层拿不到 Minecraft 的 `Minecraft#gameMode`、准星命中这些对象，
 * 所以这里只声明「要做什么」，由版本层给出实现；没实现的平台全是空操作，
 * 功能自然不生效，也不会影响别的触控逻辑。</p>
 */
interface TweakActionProvider {
    /**
     * 现在这一下「使用」能不能走基岩版的四周放置：
     * 手里拿着方块、低头站在方块边缘、准星前面是空地。
     */
    fun canReachAroundPlace(): Boolean

    /**
     * 真正执行一次「使用」点击（放方块 / 开箱子 / 吃东西 / 拉弓 / 四周放置）。
     * 原版在「正在举盾」时会把点击队列整批丢弃，所以举盾点方块由这里代执行。
     *
     * @param reachAround 准星对着空气时，是否先试一次基岩版的四周放置
     */
    fun performUseClick(reachAround: Boolean)

    /** 真正执行一次「攻击 / 挖方块」点击。 */
    fun performAttackClick()

    /**
     * 举盾期间要落一次点击：先让原版把盾放下（走原版释放流程，数据包同步齐全），
     * 后面的点击才不会继续被「正在使用物品」吞掉。
     */
    fun releaseShieldForClick()

    /**
     * 替原版开始使用手上的食物 / 蓄力武器（拉弓、上弦、吃东西）。
     * 长按方块时用来抢在「右键方块」前面，避免开箱子/按按钮把这一下吃掉。
     */
    /** 替原版启动「按住使用」；返回 false 表示这东西根本不是按住持续用的（投掷物、点一下就生效的道具）。 */
    fun startUsingHeldItem(): Boolean

    /**
     * 挥一下主手（挥手动画）。
     *
     * <p>原版只在「这次点击真的做了什么」时才挥手：拿着用不出去的东西点方块、
     * 点了个空操作，屏幕上是完全没有反馈的。这里用来补那一下动画。</p>
     */
    fun swingArm()

    /**
     * 直接驱动原版把盾举起来（潜行举盾用）。
     *
     * <p>不碰「使用」键的点击 / 按键状态——右键通道保持干净，
     * 举盾期间玩家单击方块 / 攻击不会被原版吞掉。幂等：已经在用东西时直接返回。</p>
     */
    fun raiseShield()

    /** 直接驱动原版把盾放下来（只收盾，不影响其他正在使用的物品）。 */
    fun lowerShield()

    /**
     * 把原版「使用」键队列里还没被消费的点击全部丢掉。
     *
     * <p>挖方块前必须清一次：`KeyBindingState.addLock` 在按下瞬间会往原版队列塞一条点击，
     * 上一条要是没被消费（刚放完方块 / 刚吃完东西），挖方块时原版会先把这条右键消费掉——
     * 于是手里的方块、刷怪蛋先被放出去一次，然后才开始挖。</p>
     */
    fun clearUseClicks()

    /**
     * 把原版「攻击」键队列里还没被消费的点击全部丢掉。
     *
     * <p>点方块这一下应该是「用」：上一条攻击点击要是没消费掉（刚挖过方块），
     * 原版会先把这次左键消费掉——创造模式下就是直接把方块打掉，
     * 看起来像「点一下就把方块挖了」。</p>
     */
    fun clearAttackClicks()
}

object EmptyTweakActionProvider : TweakActionProvider {
    override fun canReachAroundPlace() = false

    override fun performUseClick(reachAround: Boolean) {}

    override fun performAttackClick() {}

    override fun releaseShieldForClick() {}

    override fun startUsingHeldItem() = false

    override fun swingArm() {}

    override fun raiseShield() {}

    override fun lowerShield() {}

    override fun clearUseClicks() {}

    override fun clearAttackClicks() {}
}

/** 当前生效的动作实现，注册方式和 [TweakState] 一致：版本模块加载时装进来。 */
object TweakAction {
    @Volatile
    var provider: TweakActionProvider = EmptyTweakActionProvider
}
