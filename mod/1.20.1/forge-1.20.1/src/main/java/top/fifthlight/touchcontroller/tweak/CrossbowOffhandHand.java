package top.fifthlight.touchcontroller.tweak;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import top.fifthlight.touchcontroller.common.config.TweakSwitches;

/**
 * 「副手拿着东西时，主手弩的第一人称绘制」。
 *
 * <h3>为什么必须自己画</h3>
 * <p>原版 {@code ItemInHandRenderer#renderArmWithItem} 里弩那一支是提前 return 的，
 * 模组拿不到「按副手有没有东西开关」的接口。更关键的是原版 {@code evaluateWhichHandsToRender}：
 * <b>主手弩一旦上膛，就只画主手、副手整只手跳过</b>，连 Forge 的副手 {@link RenderHandEvent}
 * 都不会发出——所以想让副手不消失，只能在主手这次事件里替原版补画。</p>
 *
 * <h3>本功能的行为</h3>
 * <p>命中条件：主手拿弩 + 副手有物品 + 弩已经上膛、且人没在拉。这时：</p>
 * <ol>
 *   <li><b>弩的位置改成和「未装填」完全一致</b>。原版给上膛的弩额外加了
 *       「往左推 0.641864、再偏 10°」的小动作（就是上膛标记），这里去掉它，
 *       直接用「没上膛」那条变换重画，看起来和空弩一模一样。</li>
 *   <li><b>副手不再消失</b>。原版这时整只手跳过，就按原版副手那套变换把它补画回来；
 *       如果原版本来就会画副手（例如玩家正在用副手吃东西），就交给原版，绝不重复画两遍。</li>
 * </ol>
 * <p><b>上弦过程中</b>（按住右键上弦）完全交给原版：原版这时端着弩、并且把副手收起来。
 * <b>没装填、也没在拉</b>时同样交给原版，原版本来就把两只手都画出来。</p>
 *
 * <p>所有变换都写在自己 {@code pushPose/popPose} 的这一层里，不会漏到随后画的别的渲染上。</p>
 */
@Mod.EventBusSubscriber(modid = "touchcontroller", value = Dist.CLIENT)
public final class CrossbowOffhandHand {

    private CrossbowOffhandHand() {
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (event.getHand() == InteractionHand.OFF_HAND) {
            renderOffhandCrossbowIdle(event);
            return;
        }
        if (!TweakSwitches.current().getCrossbowOffhandRender()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || player.isScoping()) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || !(stack.getItem() instanceof CrossbowItem)) {
            return;
        }
        // 只在「副手拿着东西」时接管；副手空手保持原版第一人称手感。
        if (player.getOffhandItem().isEmpty()) {
            return;
        }
        // 正在给主手弩上弦（原版端着弩、并且把副手收起来）：交给原版。
        if (player.isUsingItem() && player.getUsedItemHand() == InteractionHand.MAIN_HAND
                && player.getUseItemRemainingTicks() > 0) {
            return;
        }
        // 没上膛的弩原版就是「未装填」的样子，也交给原版。
        if (!CrossbowItem.isCharged(stack)) {
            return;
        }

        // 到这里：副手有物品 + 主手弩已上膛。取消原版这只手，自己按「未装填」那条变换重画。
        event.setCanceled(true);

        ItemInHandRenderer renderer = minecraft.getEntityRenderDispatcher().getItemInHandRenderer();
        PoseStack poseStack = event.getPoseStack();
        HumanoidArm arm = player.getMainArm();

        poseStack.pushPose();
        try {
            applyIdleCrossbowPose(poseStack, arm, event.getSwingProgress(), event.getEquipProgress());
            renderHandItem(renderer, player, stack, arm, poseStack, event);
        } finally {
            poseStack.popPose();
        }

        // 副手这一帧原版会不会自己画？会画就交给它；原版因为「主手弩已上膛」整只手跳过时，
        // 由我们补画，副手的物品才不会凭空消失。
        if (!vanillaRendersOffhand(player)) {
            poseStack.pushPose();
            try {
                renderOffhand(renderer, player, poseStack, event);
            } finally {
                poseStack.popPose();
            }
        }
    }

    // ------------------------------------------------------------------
    // 副手弩：也同样不许往上膛那个偏移上跑
    // ------------------------------------------------------------------

    /**
     * 副手拿着<b>上膛的弩</b>时的第一人称绘制（例如主手举盾 / 主手拿别的、副手是装好的弩）。
     *
     * <p>原版给「已上膛」的弩额外加了「往左推 0.641864、再偏 10°」的补偿，主手副手都会吃到，
     * 于是副手那把弩会从手边滑到屏幕中间去——基岩版没有这个偏移，位置是「不动」的。
     * 这里把副手这次渲染取消，改用「未装填」那条变换重画，位置就回到手边。</p>
     *
     * <p>正在使用副手弩（上弦 / 端着）时交给原版，绝不抢。</p>
     */
    private static void renderOffhandCrossbowIdle(RenderHandEvent event) {
        if (!TweakSwitches.current().getCrossbowOffhandRender()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || player.isScoping()) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || !(stack.getItem() instanceof CrossbowItem)) {
            return;
        }
        // 没上膛的弩原版就是「未装填」的样子，不动它。
        if (!CrossbowItem.isCharged(stack)) {
            return;
        }
        // 正在使用这只手的弩：交给原版。
        if (player.isUsingItem() && player.getUsedItemHand() == InteractionHand.OFF_HAND) {
            return;
        }
        // 主手也是上膛的弩时，副手由主手那次事件统一补画，这里不再画一遍（否则双弩会画两次）。
        if (isChargedCrossbow(player.getMainHandItem())) {
            return;
        }

        event.setCanceled(true);

        ItemInHandRenderer renderer = minecraft.getEntityRenderDispatcher().getItemInHandRenderer();
        PoseStack poseStack = event.getPoseStack();
        HumanoidArm arm = player.getMainArm().getOpposite();

        poseStack.pushPose();
        try {
            applyIdleCrossbowPose(poseStack, arm, event.getSwingProgress(), event.getEquipProgress());
            renderHandItem(renderer, player, stack, arm, poseStack, event);
        } finally {
            poseStack.popPose();
        }
    }

    // ------------------------------------------------------------------
    // 主手弩：复刻原版「未装填」那条变换
    // ------------------------------------------------------------------

    /**
     * 原版 {@code renderArmWithItem} 里弩分支、<b>非使用</b>那条路径的完整变换，数值照抄 1.20.1：
     * 挥动偏移（{@code f/f1/f2}）→ {@code applyItemArmTransform}（{@code ±0.56, -0.52-e*0.6, -0.72}）→
     * {@code applyItemArmAttackTransform}（四段旋转）。
     *
     * <p>原版在这条路径末尾还有一段「已上膛就额外往左推 0.641864、再偏 10°」的补偿，
     * 这里<b>故意不写</b>：这样上膛的弩和未装填的弩位置完全一致。</p>
     */
    private static void applyIdleCrossbowPose(PoseStack poseStack, HumanoidArm arm,
            float swingProgress, float equipProgress) {
        int i = arm == HumanoidArm.RIGHT ? 1 : -1;

        float f = -0.4F * Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
        float f1 = 0.2F * Mth.sin(Mth.sqrt(swingProgress) * ((float) Math.PI * 2.0F));
        float f2 = -0.2F * Mth.sin(swingProgress * (float) Math.PI);
        poseStack.translate((float) i * f, f1, f2);

        poseStack.translate((float) i * 0.56F, -0.52F + equipProgress * -0.6F, -0.72F);

        applyItemArmAttackTransform(poseStack, arm, swingProgress);
    }

    /** 原版 {@code applyItemArmAttackTransform}（挥动带来的四段旋转），数值照抄。 */
    private static void applyItemArmAttackTransform(PoseStack poseStack, HumanoidArm arm, float swingProgress) {
        int i = arm == HumanoidArm.RIGHT ? 1 : -1;

        float g = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
        poseStack.mulPose(Axis.YP.rotationDegrees((float) i * (45.0F + -20.0F * g)));
        float h = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) i * h * -20.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(h * -80.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees((float) i * -45.0F));
    }

    // ------------------------------------------------------------------
    // 副手：原版会跳过时补画
    // ------------------------------------------------------------------

    /**
     * 复刻原版 {@code evaluateWhichHandsToRender}：
     * 返回「原版这一帧会不会自己把副手画出来」。返回 {@code false} 时说明副手被整只手跳过了，需要补。
     */
    private static boolean vanillaRendersOffhand(LocalPlayer player) {
        // 主手是「上膛的弩」时，原版 evaluateWhichHandsToRender 只画主手、副手整只手跳过——
        // 哪怕玩家正在用副手举盾也一样。所以这时必须由我们补画副手，否则盾就凭空消失。
        if (isChargedCrossbow(player.getMainHandItem())) {
            return false;
        }
        // 其余情况原版都会画副手（正在用副手时更是它自己负责那只手）。
        return true;
    }

    /**
     * 补画副手。副手这时一定没有被使用，所以走原版「通用物品」的非使用路径：
     * 挥动偏移 → {@code applyItemArmTransform} → {@code applyItemArmAttackTransform}；
     * 副手拿的若是弩则走弩的空闲变换（原版的上膛补偿只作用于主手，副手本来就没有）。
     *
     * <p>副手静止时原版的高度就是满的（{@code equipProgress = 0}），这里按 0 处理，
     * 不重做「刚切到副手那几帧的抬起动画」——那段动画只在换物品的瞬间有，肉眼几乎看不到。</p>
     */
    private static void renderOffhand(ItemInHandRenderer renderer, LocalPlayer player, PoseStack poseStack,
            RenderHandEvent event) {
        ItemStack off = player.getOffhandItem();
        if (off.isEmpty()) {
            return;
        }
        // 地图走的是原版另一套双手地图变换，这里不擅自重画：保持「原版本来就不画」的状态。
        if (off.is(Items.FILLED_MAP)) {
            return;
        }

        HumanoidArm arm = player.getMainArm().getOpposite();
        float swingProgress = player.swingingArm == InteractionHand.OFF_HAND
                ? player.getAttackAnim(event.getPartialTick())
                : 0.0F;

        if (player.isUsingItem() && player.getUsedItemHand() == InteractionHand.OFF_HAND) {
            // 副手正在被使用（潜行举盾 / 吃东西 / 拉弓）：原版这时走的是「使用物品」那条路径，
            // 手臂抬到身前。这里必须照抄那套姿势，否则会把「举起来的盾」按「垂在手边」的
            // 变换画出来——盾牌直接穿过镜头/身体，就是穿模。
            applyUsingItemArmPose(poseStack, arm, event.getEquipProgress(), swingProgress);
        } else if (off.getItem() instanceof CrossbowItem) {
            applyIdleCrossbowPose(poseStack, arm, swingProgress, 0.0F);
        } else {
            int i = arm == HumanoidArm.RIGHT ? 1 : -1;
            float f = -0.4F * Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
            float f1 = 0.2F * Mth.sin(Mth.sqrt(swingProgress) * ((float) Math.PI * 2.0F));
            float f2 = -0.2F * Mth.sin(swingProgress * (float) Math.PI);
            poseStack.translate((float) i * f, f1, f2);
            poseStack.translate((float) i * 0.56F, -0.52F, -0.72F);
            applyItemArmAttackTransform(poseStack, arm, swingProgress);
        }
        renderHandItem(renderer, player, off, arm, poseStack, event);
    }

    /**
     * 原版 {@code ItemInHandRenderer#renderPlayerArm} 的手臂变换（「正在使用物品」那条路径），数值照抄 1.20.1。
     *
     * <p>举盾（{@code UseAnim.BLOCK}）在原版里没有额外的物品偏移，就是这套手臂姿势，
     * 所以补画举起来的盾必须用它，不能套「静止持物」那套。</p>
     */
    private static void applyUsingItemArmPose(PoseStack poseStack, HumanoidArm arm,
            float equipProgress, float swingProgress) {
        boolean right = arm == HumanoidArm.RIGHT;
        float f = right ? 1.0F : -1.0F;
        float f1 = Mth.sqrt(swingProgress);
        float f2 = -0.3F * Mth.sin(f1 * (float) Math.PI);
        float f3 = 0.4F * Mth.sin(f1 * ((float) Math.PI * 2.0F));
        float f4 = -0.4F * Mth.sin(swingProgress * (float) Math.PI);
        poseStack.translate(f * (f2 + 0.64000005F), f3 + -0.6F + equipProgress * -0.6F, f4 + -0.71999997F);
        poseStack.mulPose(Axis.YP.rotationDegrees(f * 45.0F));
        float f5 = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
        float f6 = Mth.sin(f1 * (float) Math.PI);
        poseStack.mulPose(Axis.YP.rotationDegrees(f * f6 * 70.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(f * f5 * -20.0F));
    }

    /** 按手臂决定用哪只手的物品模型、以及原版那个 leftHand 标记。 */
    private static void renderHandItem(ItemInHandRenderer renderer, LocalPlayer player, ItemStack stack,
            HumanoidArm arm, PoseStack poseStack, RenderHandEvent event) {
        boolean rightArm = arm == HumanoidArm.RIGHT;
        renderer.renderItem(player, stack,
                rightArm ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                !rightArm,
                poseStack,
                event.getMultiBufferSource(),
                event.getPackedLight());
    }

    /** 原版 {@code isChargedCrossbow}：是弩、并且已经上膛。 */
    private static boolean isChargedCrossbow(ItemStack stack) {
        return stack.is(Items.CROSSBOW) && CrossbowItem.isCharged(stack);
    }
}
