package top.fifthlight.touchcontroller.common.layout

/**
 * 视野区手势的**版本隔离钩子**。
 *
 * <p>背景：`Context.View()` 位于 `mod/common`，被所有 MC 版本共用。手势状态机的重构
 * 只在 1.20.1 上验证，所以不能直接改共享实现——这里留一个默认空实现的钩子：</p>
 * <ul>
 *   <li>没有任何版本安装实现 → [intercept] 恒为 false，`View()` 走原有逻辑，
 *       其它版本（1.12.2 / 1.16.5 / 1.20.4 / 1.21.x …）行为与字节码行为完全不变；</li>
 *   <li>1.20.1 在启动时通过 [install] 安装实现 → 本帧视野区手势整体交给状态机版本。</li>
 * </ul>
 *
 * <p>实现方是 `top.fifthlight.touchcontroller.common_1_20_1.refactor`，安装点在
 * 1.20.1 的模组入口（`TouchController.onClientSetup`）。</p>
 */
object ViewGestureHook {
    fun interface Handler {
        /** @return true 表示本帧的视野区手势已由实现完全接管，`View()` 不再执行原有逻辑 */
        fun layout(context: Context): Boolean
    }

    @Volatile
    private var handler: Handler? = null

    fun install(handler: Handler) {
        this.handler = handler
    }

    fun uninstall() {
        handler = null
    }

    internal fun intercept(context: Context): Boolean = handler?.layout(context) ?: false
}
