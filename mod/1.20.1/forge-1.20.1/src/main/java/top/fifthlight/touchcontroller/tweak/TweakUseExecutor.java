package top.fifthlight.touchcontroller.tweak;

import java.lang.reflect.Field;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.ToolActions;

import top.fifthlight.touchcontroller.common_1_20_1.gal.TargetLock;

/**
 * 「增强功能」里真正动到原版操作的那几个动作（1.20.1 Forge 实现）。
 *
 * <p>TouchController 的 common 层碰不到 {@code Minecraft#gameMode} 和准星命中，
 * 所以都收在这里，由 {@link TweakActionProviderImpl} 转交。</p>
 *
 * <p>每一处入口都先判空：进世界前、退世界后、打开界面时调用都不会出事。</p>
 */
public final class TweakUseExecutor {
    /** 原版 {@code Minecraft#rightClickDelay}，让代执行的右键遵守原版 4 tick 连点间隔。 */
    private static final Field RIGHT_CLICK_DELAY_FIELD = findRightClickDelayField();

    private TweakUseExecutor() {
    }

    // ------------------------------------------------------------------
    // 手上这堆东西「现在能不能用出去」：给「食物/蓄力优先」判断要不要替原版开始使用
    // （长按改判挖方块的主判定在 common-1.20.1 的 TweakStateProviderImpl 里）
    // ------------------------------------------------------------------

    /**
     * 这只手的物品是不是「现在就能用出去」：
     * {@code null} 表示不适用（空手、不是食物/弓弩，交给 0.3 秒兜底）。
     */
    private static Boolean judgeHand(LocalPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        if (stack.isEdible()) {
            FoodProperties food = stack.getFoodProperties(player);
            boolean alwaysEdible = food != null && food.canAlwaysEat();
            // 原版 Item#use 的判定：canEat 为 false 时进食根本不开始。
            return player.canEat(alwaysEdible);
        }
        if (stack.getItem() instanceof CrossbowItem) {
            return CrossbowItem.isCharged(stack) || hasRangedAmmo(player, stack);
        }
        if (stack.getItem() instanceof BowItem) {
            return hasRangedAmmo(player, stack);
        }
        return null;
    }

    private static boolean hasRangedAmmo(LocalPlayer player, ItemStack stack) {
        // 创造模式不需要弹药也能拉弓上弦。
        return player.getAbilities().instabuild || !player.getProjectile(stack).isEmpty();
    }

    // ------------------------------------------------------------------
    // 食物 / 蓄力优先：长按方块时先替原版开始使用
    // ------------------------------------------------------------------

    /**
     * 长按方块、手里拿的是能吃的东西或蓄力武器时，替原版走一次 {@code useItem}。
     *
     * <p>原版处理「按着右键」时，会先用这一下右键去对方块：开箱子、按按钮、开门……
     * 轮不到吃，也轮不到拉弓。先让玩家进入「正在使用物品」状态之后，
     * 原版这一 tick 就不会再走方块交互了，食物和蓄力不会被抢走。</p>
     */
    public static boolean startUsingHeldItem() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.gameMode == null) {
            return false;
        }
        // 已经在用了（吃 / 拉弓 / 举盾）：算启动成功，别再动它。
        if (player.isUsingItem()) {
            return true;
        }
        InteractionHand hand = foodOrChargeHand(player);
        if (hand == null) {
            // 手上没有「能吃 / 能蓄力」的东西：投掷物、点一下就生效的模组道具都走这里。
            // 返回 false 让调用方把这一下当成普通右键代执行掉，别掉进挖方块。
            return false;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack.getItem())) {
            return false;
        }
        minecraft.gameMode.useItem(player, hand);
        return player.isUsingItem();
    }

    /** 拿着「能吃/能蓄力」那只手：主手优先、其次副手；两只手都不是就返回 {@code null}。 */
    private static InteractionHand foodOrChargeHand(LocalPlayer player) {
        if (isUsableFoodOrCharge(player, player.getMainHandItem())) {
            return InteractionHand.MAIN_HAND;
        }
        if (isUsableFoodOrCharge(player, player.getOffhandItem())) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    private static boolean isUsableFoodOrCharge(LocalPlayer player, ItemStack stack) {
        Boolean judged = judgeHand(player, stack);
        if (Boolean.TRUE.equals(judged)) {
            return true;
        }
        // judgeHand 不认识的模组物品（月光蠕虫女王这类蓄力武器）：
        // 它确实有使用时长，就该能替原版启动，否则对着可交互方块长按会变成什么都不发生。
        return !stack.isEmpty()
                && !stack.isEdible()
                && stack.getUseDuration() > 0;
    }

    // ------------------------------------------------------------------
    // 举盾时点方块：先放盾，再自己把这次点击执行掉
    // ------------------------------------------------------------------

    /**
     * 让原版把盾放下。举盾期间原版把所有点击队列整批丢掉（它看到「正在使用物品」），
     * 所以点方块前必须先走原版释放流程——释放数据包、服务端状态都随之同步。
     */
    public static void releaseShieldForClick() {
        lowerShield();
    }

    // ------------------------------------------------------------------
    // 潜行举盾：直接驱动原版举盾 / 收盾，和右键（USE 键）彻底分离
    // ------------------------------------------------------------------

    /**
     * 驱动原版把盾举起来。
     *
     * <p>直接走 {@code GameMode#useItem}，让玩家进入「正在使用物品（盾）」状态。
     * 不碰 KeyMapping 的点击 / 按键状态——右键通道保持干净，
     * 举盾期间玩家单击方块 / 攻击不会被原版吞掉。</p>
     *
     * <p>幂等：已经在用东西时直接返回，同一帧重复调用也不会反复触发。</p>
     */
    public static void raiseShield() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.gameMode == null || minecraft.level == null) {
            return;
        }
        InteractionHand hand = shieldHand(player);
        if (hand == null) {
            return;
        }
        if (player.isUsingItem()) {
            // 已经在举盾（幂等），或者正在吃 / 拉弓 / 用别的东西：
            // 一律不动它。打断进食会让「潜行举盾期间吃东西」变成吃一口就被放下，
            // 所以这里不再强制停掉；这一口 / 这一箭结束后由 SneakShieldHandler 重新举盾。
            return;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack.getItem())) {
            return;
        }
        minecraft.gameMode.useItem(player, hand);
    }

    /**
     * 驱动原版把盾放下来。走原版 {@code releaseUsingItem}，
     * 释放数据包、服务端状态、动画全由原版负责，这里不碰私有字段。
     */
    public static void lowerShield() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.gameMode == null) {
            return;
        }
        if (player.isUsingItem() && isShield(player.getUseItem())) {
            minecraft.gameMode.releaseUsingItem(player);
        }
    }

    /**
     * 把原版「使用」键队列里还没被消费的点击全部丢掉。
     *
     * <p>挖方块前清一次：{@code KeyBindingState#addLock} 在按下瞬间会往原版队列塞一条点击，
     * 上一条要是没被消费（刚放完方块、刚吃完东西、刚举过盾），挖方块时原版会先把这条右键
     * 消费掉，手里的方块 / 刷怪蛋就先被放出去一次，然后才开始挖。</p>
     */
    public static void clearUseClicks() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }
        while (minecraft.options.keyUse.consumeClick()) {
            // 丢弃，这一下是挖方块，不该同时「用」。
        }
    }

    /**
     * 把原版「攻击」键队列里还没被消费的点击全部丢掉。
     *
     * <p>点方块这一下应该是「用」：上一条攻击点击要是没消费掉（刚挖过方块 / 刚点过生物），
     * 原版会先把这次左键消费掉——创造模式下直接把方块打掉，看起来就是「点一下就把方块挖了」。</p>
     */
    public static void clearAttackClicks() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }
        while (minecraft.options.keyAttack.consumeClick()) {
            // 丢弃，这一下是「用」，不该同时「打」。
        }
    }

    /** 拿着盾的那只手：主手优先、其次副手；两只手都不是盾返回 {@code null}。 */
    private static InteractionHand shieldHand(LocalPlayer player) {
        if (isShield(player.getMainHandItem())) {
            return InteractionHand.MAIN_HAND;
        }
        if (isShield(player.getOffhandItem())) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    // ------------------------------------------------------------------
    // 点方块统一补一次挥手动画
    // ------------------------------------------------------------------

    /**
     * 挥一下主手。
     *
     * <p>原版只在「这次点击真的做了点什么」时才挥手（放方块成功、打中实体、挖到方块）。
     * 拿着用不出去的东西点方块（空手点石头、吃饱了拿着食物点方块、点了没有交互的方块）
     * 走的都是「什么都没发生」的分支，屏幕上一点反馈都没有，手感很闷。
     * 这里补一下，让每一次点方块都有挥手动画。</p>
     */
    public static void swingArm() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }
        player.swing(InteractionHand.MAIN_HAND);
    }

    /** 现在这一下能不能走四周放置（手里拿方块、准星是空气、人贴着方块边低头）。 */
    public static boolean canReachAroundPlace() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        return ReachAroundPlacement.find(player, player.getMainHandItem()) != null
                || ReachAroundPlacement.find(player, player.getOffhandItem()) != null;
    }

    // ------------------------------------------------------------------
    // 真正执行点击
    // ------------------------------------------------------------------

    /**
     * 执行一次「使用」点击（对应原版 {@code Minecraft#startUseItem}）。
     *
     * <p>先把原版点击队列里的「使用」点击消费掉，避免同一 tick 原版再执行一次。
     * 之后按原版顺序、原版规则执行：四周放置（如果开关着且满足条件）→ 生物交互 →
     * 方块交互 → 物品自身使用。</p>
     *
     * @param reachAround 是否允许先试四周放置（举盾点方块时准星在方块上，不需要）
     */
    public static void performUseClick(boolean reachAround) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.gameMode == null || minecraft.level == null || minecraft.screen != null) {
            return;
        }
        // 本模组接管了这次点击：原版点击队列里那一条不能再执行一遍。
        while (minecraft.options.keyUse.consumeClick()) {
            // 丢弃，动作由下面这一段自己完成。
        }
        resetRightClickDelay(minecraft);
        performUseInternal(minecraft, player, reachAround);
    }

    /** 执行一次「攻击 / 挖方块」点击（对应原版 {@code Minecraft#startAttack}）。 */
    public static void performAttackClick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.gameMode == null || minecraft.level == null || minecraft.screen != null) {
            return;
        }
        while (minecraft.options.keyAttack.consumeClick()) {
            // 丢弃，动作由下面这一段自己完成。
        }
        player.resetAttackStrengthTicker();
        HitResult hit = TargetLock.INSTANCE.effectiveHit();
        if (hit == null) {
            player.swing(InteractionHand.MAIN_HAND);
            return;
        }
        switch (hit.getType()) {
            case ENTITY -> {
                Entity target = ((EntityHitResult) hit).getEntity();
                if (target.isAttackable() && !target.isSpectator()) {
                    minecraft.gameMode.attack(player, target);
                }
                player.swing(InteractionHand.MAIN_HAND);
            }
            case BLOCK -> {
                BlockHitResult blockHit = (BlockHitResult) hit;
                if (!minecraft.level.isEmptyBlock(blockHit.getBlockPos())) {
                    minecraft.gameMode.startDestroyBlock(blockHit.getBlockPos(), blockHit.getDirection());
                }
                player.swing(InteractionHand.MAIN_HAND);
            }
            default -> player.swing(InteractionHand.MAIN_HAND);
        }
    }

    private static void performUseInternal(Minecraft minecraft, LocalPlayer player, boolean reachAround) {
        // 对着生物时一律用**实时**准星：瞄准锁可能还停在上一只生物（甚至一块方块）上，
        // 用锁定的旧 hit 去打，interactLivingEntity（金羹驯服、戎卫号角…）就落在了错误的目标上，
        // 表现就是「长按不能驯服，点一下才行」。
        HitResult liveHit = minecraft.hitResult;
        HitResult hit = (liveHit != null && liveHit.getType() == HitResult.Type.ENTITY)
                ? liveHit
                : TargetLock.INSTANCE.effectiveHit();
        final boolean blockTapped = hit != null && hit.getType() == HitResult.Type.BLOCK;
        // 准星对着空气时，先试四周放置：脚下前方那一格。
        // 必须排在两只手的常规使用之前：盾在主手时，常规流程里的 useItem 会立刻把盾重新举起来
        // 并返回「已消耗」，循环根本轮不到副手那摞方块。
        if (reachAround && (hit == null || hit.getType() == HitResult.Type.MISS)) {
            InteractionHand reachHand = blockItemHand(player);
            if (reachHand != null) {
                BlockHitResult reachAroundHit = ReachAroundPlacement.find(player, player.getItemInHand(reachHand));
                if (reachAroundHit != null) {
                    InteractionResult result = minecraft.gameMode.useItemOn(player, reachHand, reachAroundHit);
                    if (result.consumesAction()) {
                        player.swing(result.shouldSwing() ? reachHand : InteractionHand.MAIN_HAND);
                        return;
                    }
                }
            }
        }
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.isEmpty()) {
                continue;
            }
            if (hit != null && hit.getType() == HitResult.Type.ENTITY) {
                EntityHitResult entityHit = (EntityHitResult) hit;
                Entity entity = entityHit.getEntity();
                if (minecraft.level != null
                        && !minecraft.level.getWorldBorder().isWithinBounds(entity.blockPosition())) {
                    return;
                }
                InteractionResult result = minecraft.gameMode.interactAt(player, entity, entityHit, hand);
                if (!result.consumesAction()) {
                    result = minecraft.gameMode.interact(player, entity, hand);
                }
                if (result.consumesAction()) {
                    if (result.shouldSwing()) {
                        player.swing(hand);
                    }
                    return;
                }
            } else if (blockTapped) {
                BlockHitResult blockHit = (BlockHitResult) hit;
                InteractionResult result = minecraft.gameMode.useItemOn(player, hand, blockHit);
                if (result.consumesAction()) {
                    // 单击方块：原版只为 SUCCESS 挥手；返回 CONSUME（模组物品常见）时也补主手一下。
                    player.swing(result.shouldSwing() ? hand : InteractionHand.MAIN_HAND);
                    return;
                }
            } else if (hit == null || hit.getType() == HitResult.Type.MISS) {
                BlockHitResult reachAroundHit = ReachAroundPlacement.find(player, stack);
                if (reachAroundHit != null) {
                    InteractionResult result = minecraft.gameMode.useItemOn(player, hand, reachAroundHit);
                    if (result.consumesAction()) {
                        player.swing(result.shouldSwing() ? hand : InteractionHand.MAIN_HAND);
                        return;
                    }
                }
            }
            InteractionResult itemResult = minecraft.gameMode.useItem(player, hand);
            if (itemResult.consumesAction()) {
                // 食物、弓、药水等在原版是 CONSUME（它自己不挥手），单击方块时同样补主手一下。
                player.swing(itemResult.shouldSwing() ? hand : InteractionHand.MAIN_HAND);
                return;
            }
        }
        // 主副手都没消耗动作（含两只手都空）：只要准星确实点在方块上，就补一次主手挥动。
        if (blockTapped) {
            player.swing(InteractionHand.MAIN_HAND);
        }
    }

    /** 拿着方块的那只手：主手优先、没有再看副手；两只手都不是方块返回 {@code null}。 */
    private static InteractionHand blockItemHand(LocalPlayer player) {
        if (ReachAroundPlacement.isBlockItem(player.getMainHandItem())) {
            return InteractionHand.MAIN_HAND;
        }
        if (ReachAroundPlacement.isBlockItem(player.getOffhandItem())) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    private static boolean isShield(ItemStack stack) {
        return top.fifthlight.touchcontroller.common_1_20_1.gal.ShieldWhitelist.INSTANCE.matches(stack);
    }

    private static void resetRightClickDelay(Minecraft minecraft) {
        if (RIGHT_CLICK_DELAY_FIELD == null) {
            return;
        }
        try {
            RIGHT_CLICK_DELAY_FIELD.setInt(minecraft, 4);
        } catch (ReflectiveOperationException ignored) {
            // 反射失败只影响连点节奏，不影响功能。
        }
    }

    private static Field findRightClickDelayField() {
        try {
            Field field = Minecraft.class.getDeclaredField("rightClickDelay");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
