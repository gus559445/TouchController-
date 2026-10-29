package top.fifthlight.touchcontroller.tweak;

import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import top.fifthlight.touchcontroller.common.config.TweakSwitches;

/**
 * 「基岩版交互距离」：把伸手距离对齐基岩版的**触控输入**（方块/实体 6 格，创造模式 12 格）。
 *
 * <p>基岩版分两套：键鼠/手柄 5 格，触控 6 格（创造 12 格）。这是手机触控模组，按触控那套对齐。
 * Java 原版是非创造 4.5 / 3.0、创造 5 / 5，差的由 modifier 补上。</p>
 *
 * <h3>为什么用 modifier 而不是改 baseValue</h3>
 * 原版属性总价值 = baseValue + 所有 modifier 之和。武器、饰品、别的模组给的攻击距离
 * 基本都是 {@link AttributeModifier}；以前这里直接 {@code setBaseValue}，
 * 每刻还把 base 强行写回，等于把别人写在 baseValue 上的加成抹掉，
 * 表现就是「拿着加攻击距离的武器也没有更远」。
 * 现在改成挂一个 {@code ADDITION} modifier（非创造：方块 +1.5、实体 +3.0；创造：都 +7.0），
 * 天然叠加：原版基础加上我们的，再加上武器自己的 modifier，互不覆盖。
 *
 * <h3>只在单人模式生效</h3>
 * 伸手距离是服务端说了算的属性。联机服务器上客户端自己加 modifier，
 * 服务端还是按原值判定，就会出现「看着够得着但点不动」的错位，
 * 所以这里直接用 {@code hasSingleplayerServer()} 卡死：进服务器一律不加。
 *
 * <h3>为什么要分两处挂</h3>
 * 玩家每次进世界、换维度、复活都会重建实体，属性随之回到默认值：
 * {@link EntityJoinLevelEvent} 在这些时刻补挂；{@link TickEvent.PlayerTickEvent}
 * 兜底（被别的模组清掉时补回来）。transient modifier 不写存档，重进世界会重新挂。
 */
@Mod.EventBusSubscriber(modid = "touchcontroller")
public final class ExtraReach {

    /** 非创造模式、方块：Java 4.5 → 基岩版「触控输入」的 6 格，加 1.5。 */
    private static final double BLOCK_BONUS_SURVIVAL = 1.5D;

    /** 非创造模式、实体：Java 3.0 → 基岩版「触控输入」的 6 格，加 3.0。 */
    private static final double ENTITY_BONUS_SURVIVAL = 3.0D;

    /**
     * 创造模式：基岩版触控是 12 格。
     *
     * <p>Java 这边创造模式自带 +0.5（方块）/ +2.0（实体）的 modifier，基础值仍是 4.5 / 3.0，
     * 再补 7.0 正好 4.5+0.5+7=12、3.0+2+7=12，所以方块和实体用同一个补正。</p>
     */
    private static final double BONUS_CREATIVE = 7.0D;

    private static final UUID BLOCK_MODIFIER_ID = UUID.fromString("7c1f5a10-3b8e-4d21-9a44-5e6f7a8b9c01");
    private static final UUID ENTITY_MODIFIER_ID = UUID.fromString("7c1f5a10-3b8e-4d21-9a44-5e6f7a8b9c02");

    private static final String MODIFIER_NAME = "touchcontroller_extra_reach";

    private ExtraReach() {
    }

    /** 玩家进入世界（进服、换维度、复活）时立刻挂一次。 */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Player player) {
            applyReach(player);
        }
    }

    /** 兜底：modifier 被别的模组清掉时补回来（每刻只在结尾检查一次）。 */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            applyReach(event.player);
        }
    }

    private static void applyReach(Player player) {
        boolean enabled = TweakSwitches.current().getExtraReach() && singleplayerOnly();
        boolean creative = player.isCreative();
        double blockBonus = creative ? BONUS_CREATIVE : BLOCK_BONUS_SURVIVAL;
        double entityBonus = creative ? BONUS_CREATIVE : ENTITY_BONUS_SURVIVAL;
        applyModifier(player, ForgeMod.BLOCK_REACH.get(), BLOCK_MODIFIER_ID, blockBonus, enabled);
        applyModifier(player, ForgeMod.ENTITY_REACH.get(), ENTITY_MODIFIER_ID, entityBonus, enabled);
    }

    private static void applyModifier(Player player, Attribute attribute, UUID id, double amount, boolean enabled) {
        AttributeInstance instance = player.getAttribute(attribute);
        // 属性没挂上（极少数情况，比如被数据包移除了）就跳过，别让异常打断游戏。
        if (instance == null) {
            return;
        }
        // 加成是 0 的那一项（实体距离：基岩版和 Java 一样是 3.0）不挂，免得白留一条无用 modifier。
        boolean want = enabled && amount > 0.0D;
        AttributeModifier existing = instance.getModifier(id);
        if (want) {
            if (existing == null || existing.getAmount() != amount) {
                if (existing != null) {
                    instance.removeModifier(id);
                }
                instance.addTransientModifier(
                        new AttributeModifier(id, MODIFIER_NAME, amount, AttributeModifier.Operation.ADDITION));
            }
        } else if (existing != null) {
            // 关掉开关（或进了服务器）：只撤掉我们自己挂的那一条，别人的加成原样不动。
            instance.removeModifier(id);
        }
    }

    /** 只有「带集成服务端的单人存档」才生效；联机服务器一律不加，避免和服务端判定错位。 */
    private static boolean singleplayerOnly() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft != null && minecraft.hasSingleplayerServer();
    }
}
