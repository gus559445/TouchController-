package top.fifthlight.touchcontroller.tweak;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import top.fifthlight.touchcontroller.common.config.TweakSwitches;

/**
 * 「盾牌弹反击退减弱」：盾牌挡下近战攻击后，把攻击者被弹开的距离从「四五格」收回到约一格。
 *
 * <h3>原版是怎么弹的</h3>
 * 1.20.1 原版在 {@code Player#blockUsingShield} 里对近战攻击者执行
 * {@code knockback(0.5, dx, dz)}，强度 0.5 差不多滑开四五格。
 * 这里监听 {@link ShieldBlockEvent} 把那一下标记下来，再在
 * {@link LivingKnockBackEvent} 里把强度改成 0.3（约一格）。
 *
 * <h3>只处理近战</h3>
 * 箭这类弹射物原版本就不弹返攻击者，所以跳过（{@link DamageTypeTags#IS_PROJECTILE}）。
 * 音效、格挡动画、耐久损耗全部还是原版的，这里只动「被弹开多远」这一个数值。
 *
 * <h3>联机说明</h3>
 * 击退是服务端算的，所以这项功能需要服务端也装着 TouchController（单人游戏里集成服务端
 * 和客户端在同一个进程，自动生效）。只装在客户端时，联机服务器上这一条不生效，其余功能不受影响。
 */
@Mod.EventBusSubscriber(modid = "touchcontroller")
public final class ShieldKnockback {

    /** 弹返强度：原版 0.5 约四五格；这里 0.7 明显弹得更开（与 Kotlin 版 ShieldTweaks 保持一致）。 */
    private static final float SHIELD_PUSH_STRENGTH = 0.7F;

    /** 本 tick 内刚被盾牌挡下近战攻击的实体（等它的击退事件到来时削弱一次）。 */
    private static final Set<UUID> SHIELD_PUSHED_ATTACKERS = new HashSet<>();

    private ShieldKnockback() {
    }

    /** 格挡成功：把近战攻击者记下来，紧跟着的那次击退就削它。 */
    @SubscribeEvent
    public static void onShieldBlock(ShieldBlockEvent event) {
        if (!TweakSwitches.current().getShieldKnockback()) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (player.level().isClientSide) {
            return;
        }
        if (event.getDamageSource().is(DamageTypeTags.IS_PROJECTILE)) {
            return;
        }
        if (event.getDamageSource().getDirectEntity() instanceof LivingEntity attacker) {
            SHIELD_PUSHED_ATTACKERS.add(attacker.getUUID());
        }
    }

    /** 把刚才那次弹返击退削弱掉；标记单次消费，不重复削同一次。 */
    @SubscribeEvent
    public static void onKnockBack(LivingKnockBackEvent event) {
        if (SHIELD_PUSHED_ATTACKERS.remove(event.getEntity().getUUID())) {
            event.setStrength(SHIELD_PUSH_STRENGTH);
        }
    }

    /** 击退本来就在格挡的同一调用栈里发生，这里只防漏网的标记留到下一 tick。 */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !SHIELD_PUSHED_ATTACKERS.isEmpty()) {
            SHIELD_PUSHED_ATTACKERS.clear();
        }
    }
}
