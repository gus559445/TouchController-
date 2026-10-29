package top.fifthlight.touchcontroller.common.config

/**
 * 「增强功能」开关的只读快照，专门给 Java 侧的事件处理用。
 *
 * <h3>为什么需要它</h3>
 * 交互距离、盾牌弹反击退这几件事发生在 Forge 事件里（Java），拿不到 Koin 里的配置对象，
 * 也不好为了读一个布尔值把 Koin 的 Java API 拖进来。所以客户端每渲染帧把当前配置往这里存一份，
 * Java 侧直接读最后一个值即可——一次普通的字段读取，没有任何开销。
 *
 * <h3>为什么默认是「全开」</h3>
 * 单人游戏里客户端和集成服务端在同一个 JVM，客户端写入后服务端的事件读到的就是用户设置；
 * 独立服务器上没有客户端写这份快照，就保持默认值（开关默认都是开的），
 * 行为和「没改过设置」一致，不会莫名其妙失效。
 */
object TweakSwitches {
    @Volatile
    private var snapshot = TweaksConfig()

    /** 最后一次读到的开关组合；Java 侧用 `TweakSwitches.current()` 调用。 */
    @JvmStatic
    fun current(): TweaksConfig = snapshot

    /** 客户端每渲染帧调用一次，把用户设置同步过来。 */
    fun update(config: TweaksConfig) {
        snapshot = config
    }
}
