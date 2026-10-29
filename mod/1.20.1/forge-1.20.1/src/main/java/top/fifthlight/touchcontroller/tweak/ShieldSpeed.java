package top.fifthlight.touchcontroller.tweak;

import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import top.fifthlight.touchcontroller.common.config.TweakSwitches;

/**
 * 「举盾不叠加减速」：举着盾走路时，只吃潜行本身的减速，不再和「使用物品」的减速叠在一起。
 *
 * <h3>问题在哪</h3>
 * 原版客户端会对移动输入乘两次：潜行时乘 0.3，正在使用物品（含举盾）时再乘 0.2，
 * 两个叠起来只剩 6% 速度，走起来像被粘在地上。
 *
 * <h3>为什么改输入，不改属性</h3>
 * 绝不能改 {@code MOVEMENT_SPEED} 属性——原版 {@code AbstractClientPlayer#getFieldOfViewModifier}
 * 用属性值直接算视野，改属性会让举盾时视角跟着缩放（画面忽远忽近）。
 * 正确做法是在 {@link MovementInputUpdateEvent} 里把输入冲量乘 5，
 * 恰好抵消原版 {@code LocalPlayer.aiStep} 之后会乘的那个 0.2，净效果只剩潜行的 0.3，视角不变。
 *
 * <h3>只在客户端</h3>
 * 服务端没有「使用物品×0.2」这条输入缩放，所以服务端不需要任何补偿。
 */
@Mod.EventBusSubscriber(modid = "touchcontroller", value = Dist.CLIENT)
public final class ShieldSpeed {

    /** 抵消原版「正在使用物品 ×0.2」需要的倍数：0.2 × 5 = 1。 */
    private static final float COMPENSATION = 5.0F;

    private ShieldSpeed() {
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!TweakSwitches.current().getShieldSpeed()) {
            return;
        }
        if (!(event.getEntity() instanceof LocalPlayer player)) {
            return;
        }
        // 只在「潜行 + 举盾」时补偿：潜行减速保留，被抵消的只有使用时那一段。
        if (!player.isCrouching() || !isUsingShield(player)) {
            return;
        }
        Input input = event.getInput();
        if (input == null) {
            return;
        }
        input.leftImpulse *= COMPENSATION;
        input.forwardImpulse *= COMPENSATION;
    }

    private static boolean isUsingShield(LocalPlayer player) {
        return player.isUsingItem() && isShield(player.getUseItem());
    }

    private static boolean isShield(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.canPerformAction(ToolActions.SHIELD_BLOCK);
    }
}
