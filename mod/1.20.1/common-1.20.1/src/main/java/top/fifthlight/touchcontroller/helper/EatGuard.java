package top.fifthlight.touchcontroller.helper;

/**
 * 「正在长按使用物品」期间压制方块交互的开关。
 *
 * <p>吃东西时触控层会锁住 USE，也就是替玩家按住右键——这样原版才能靠
 * `continueUsingItem` 把进食进度走完。但按住右键同时意味着：一旦进食在某一 tick
 * 没在继续（刚吃完一口、下一口还没接上），原版就会拿这一下右键去 `useItemOn`，
 * 对着箱子就是「吃完把箱子打开了」。</p>
 *
 * <p>与其去拆按键状态（会连带吃不完），不如直接压掉这个交互：由触控层每帧写，
 * mixin 在 `MultiPlayerGameMode.useItemOn` 的 HEAD 读到 true 就返回 PASS。</p>
 */
public final class EatGuard {
    public static volatile boolean suppressBlockInteraction = false;

    private EatGuard() {
    }
}
