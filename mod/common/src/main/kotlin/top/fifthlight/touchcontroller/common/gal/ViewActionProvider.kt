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
}