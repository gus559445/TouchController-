package top.fifthlight.touchcontroller.common.gal

enum class CrosshairTarget {
    BLOCK,
    ENTITY,
    MISS
}

interface ViewActionProvider {
    fun getCrosshairTarget(): CrosshairTarget?
    fun getCurrentBreakingProgress(): Float

    /**
     * 准星指着的方块是不是「可交互方块」（箱子、工作台、熔炉、告示牌这类有方块实体的）。
     *
     * <p>对着它们长按必须走挖方块，不能判成「用物品」——否则按住右键的那一下
     * 会被原版拿去开箱子/按按钮，这就是要防的误触。普通方块（石头、泥土）不受影响，
     * 月光蠕虫女王这类要对着方块用的物品照常能蓄力。</p>
     */
    fun isTargetBlockInteractive(): Boolean = false

    /**
     * **实时**准星指着的实体 UUID（不是「瞄准锁」里的旧目标）。
     *
     * <p>长按判定要拿它和「按下那一刻对着的实体」比：按住屏幕转视角、触控圈扫过
     * 另一个可交互生物（村民、马、已驯服的生物）时，实体变了就不算这一下的长按动作，
     * 不会凭空对那只生物发出一记右键。</p>
     */
    fun getCrosshairEntityUuid(): java.util.UUID? = null
}