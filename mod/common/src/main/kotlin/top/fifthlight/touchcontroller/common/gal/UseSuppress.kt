package top.fifthlight.touchcontroller.common.gal

/**
 * 「长按方块时的右键拦截」。
 *
 * <p>对着方块长按、手上是食物或蓄力武器时，那一记右键必须让给吃/蓄力。
 * 但原版处理右键的时机在 tick 里，跟触控的渲染帧不同步，光靠「不锁 USE 键」
 * 拦不住：按键锁本身会往原版队列塞一条点击，原版下一帧就把它用在方块上了。</p>
 *
 * <p>所以这里给平台层一个「这段时间别处理右键」的窗口，
 * 由平台层在 {@code InputEvent.InteractionKeyMappingTriggered} 里直接 cancel 掉。
 * 短按（点一下）不进这个窗口，点方块照旧是正常交互。</p>
 */
object UseSuppress {
    private const val WINDOW_MS = 250L

    @Volatile
    private var suppressUntilMillis = 0L

    /** 打开一个拦截窗口（长按方块期间每帧刷新一次即可）。 */
    fun suppressFor(millis: Long = WINDOW_MS) {
        suppressUntilMillis = System.currentTimeMillis() + millis
    }

    /** 这一刻要不要拦掉原版的右键使用。 */
    fun isSuppressed(): Boolean = System.currentTimeMillis() < suppressUntilMillis
}
