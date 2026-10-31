package top.fifthlight.touchcontroller.common_1_20_1.refactor

import net.minecraft.client.Minecraft
import net.minecraft.world.InteractionHand

/**
 * 补一次挥手。
 *
 * <p>短按方块时下游走的是 `useKeyState.click()`，原版只有在这一下 `useItemOn` **成功**时
 * 才会自己 `swing`。拿用不出去的东西点方块、或者交互被拒时，原版什么都不做，
 * 玩家看不到任何反馈。这里在「点方块」这一支上显式补一次挥臂：
 * 真做成的时候原版同一 tick 也会挥，两次挥手看不出来；没做成的时候这就是唯一的反馈。</p>
 */
internal fun swingArm() {
    val player = Minecraft.getInstance().player ?: return
    player.swing(InteractionHand.MAIN_HAND)
}
