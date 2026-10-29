package top.fifthlight.touchcontroller.common.gal

/**
 * 「瞄准指定实体」的过渡钩子：让准星判定也能用上锁定中的目标。
 *
 * <p>松手后触摸圈就没了，如果 {@code getCrosshairTarget()} 还只看实时的
 * {@code minecraft.hitResult}，图层里的按钮会按「当前准星」走分支、作用到错误的位置。
 * 所以平台层把「锁定中的目标类型」装进来，这里优先用它。</p>
 *
 * <p>用法和 [TweakState] 一致：版本模块加载时装实现，没装的平台一直返回 null（保持原行为）。</p>
 */
object CrosshairLock {
    /** 锁定中的准星目标类型；没有有效锁定就返回 null。 */
    @Volatile
    var lockedTarget: () -> CrosshairTarget? = { null }

    /**
     * 每帧把锁定中的命中结果写回原版 {@code Minecraft.hitResult}。
     *
     * <p>只改 {@code getCrosshairTarget()} 不够：图层里的按钮走的是**原版按键**路径
     * （模拟左/右键 → {@code Minecraft.startAttack / startUseItem}），那里直接读
     * {@code hitResult}。不写回去的话，按钮还是打在松手后的实时准星上。</p>
     */
    @Volatile
    var applyLockedHit: () -> Unit = {}

    /**
     * 清掉当前锁定。
     *
     * <p>锁定期间原版射线一直返回锁定目标，所以「准星」永远停在那只实体上；
     * 玩家重新按下屏幕（想瞄别的目标）时必须先解锁，否则永远换不了目标。</p>
     */
    @Volatile
    var clearLock: () -> Unit = {}
}
