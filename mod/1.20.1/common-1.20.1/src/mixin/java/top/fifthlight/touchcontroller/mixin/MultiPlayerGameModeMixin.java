package top.fifthlight.touchcontroller.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.BlockHitResult;
import org.koin.java.KoinJavaComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.fifthlight.combine.platform_1_20_1.ItemFactoryImplKt;
import top.fifthlight.touchcontroller.common.config.GlobalConfigHolder;
import top.fifthlight.touchcontroller.common.helper.CrosshairTargetHelper;
import top.fifthlight.touchcontroller.common.helper.SneakShieldHandler;
import top.fifthlight.touchcontroller.helper.EatGuard;
import top.fifthlight.touchcontroller.helper.GameModeWithBreakingProgress;
import top.fifthlight.touchcontroller.helper.SyncableGameMode;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin implements GameModeWithBreakingProgress, SyncableGameMode {
    @Shadow
    private float destroyProgress;
    @Shadow
    @Final
    private Minecraft minecraft;
    @Unique
    private boolean touchController$resetPlayerLookTarget;
    @Unique
    private float touchController$prevYaw;
    @Unique
    private float touchController$prevPitch;
    @Unique
    private Player touchController$interactItemPlayer;
    @Unique
    private InteractionHand touchController$interactItemHand;

    @Shadow
    protected abstract void ensureHasSentCarriedItem();

    @Override
    public float touchcontroller$getBreakingProgress() {
        return destroyProgress;
    }

    @Override
    public void touchcontroller$callSyncSelectedSlot() {
        ensureHasSentCarriedItem();
    }

    @Shadow
    protected abstract void startPrediction(ClientLevel pLevel, PredictiveAction pAction);

    @Unique
    private boolean touchController$crosshairAimingContainItem(Item item) {
        GlobalConfigHolder globalConfigHolder = KoinJavaComponent.get(GlobalConfigHolder.class);
        var globalConfig = globalConfigHolder.getConfig().getValue();
        return ItemFactoryImplKt.contains(globalConfig.getItem().getCrosshairAimingItems(), item);
    }

    @Unique
    public ServerboundMovePlayerPacket touchController$interactBefore(Player player, InteractionHand hand, boolean sendFullPacket) {
        var itemStack = player.getItemInHand(hand);
        if (!touchController$crosshairAimingContainItem(itemStack.getItem())) {
            return null;
        }

        touchController$prevYaw = player.getYRot();
        touchController$prevPitch = player.getXRot();

        var rotation = CrosshairTargetHelper.calculatePlayerRotation(CrosshairTargetHelper.INSTANCE.getLastCrosshairDirection());
        float yaw = rotation.getFirst();
        float pitch = rotation.getSecond();
        player.setYRot(yaw);
        player.setXRot(pitch);
        touchController$resetPlayerLookTarget = true;
        if (sendFullPacket) {
            return new ServerboundMovePlayerPacket.PosRot(player.getX(), player.getY(), player.getZ(), yaw, pitch, player.onGround());
        } else {
            return new ServerboundMovePlayerPacket.Rot(yaw, pitch, player.onGround());
        }
    }

    @Unique
    public void touchController$interactAfter(Player player) {
        if (!touchController$resetPlayerLookTarget) {
            return;
        }
        touchController$resetPlayerLookTarget = false;
        player.setYRot(touchController$prevYaw);
        player.setXRot(touchController$prevPitch);
        this.startPrediction(this.minecraft.level, sequence -> new ServerboundMovePlayerPacket.Rot(player.getYRot(), player.getXRot(), player.onGround()));
    }

    /** 正在长按使用物品（吃/拉弓）时压掉方块交互，避免吃完那一两个 tick 把箱子打开。 */
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    public void touchController$skipBlockInteractionWhileEating(LocalPlayer player, InteractionHand hand, BlockHitResult result, CallbackInfoReturnable<InteractionResult> cir) {
        if (EatGuard.suppressBlockInteraction) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    @Inject(
            method = "useItemOn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;startPrediction(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/client/multiplayer/prediction/PredictiveAction;)V",
                    ordinal = 0
            )
    )
    public void useItemOnBefore(LocalPlayer player, InteractionHand hand, BlockHitResult result, CallbackInfoReturnable<InteractionResult> cir) {
        touchController$interactBefore(player, hand, false);
    }

    @Inject(
            method = "useItemOn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;startPrediction(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/client/multiplayer/prediction/PredictiveAction;)V",
                    ordinal = 0,
                    shift = At.Shift.AFTER
            )
    )
    public void useItemOnAfter(LocalPlayer player, InteractionHand hand, BlockHitResult result, CallbackInfoReturnable<InteractionResult> cir) {
        touchController$interactAfter(player);
    }

    /**
     * 潜行举盾期间：屏蔽原版「右键没按住就强制收盾」。
     *
     * <p>原版举盾完全依赖「使用键按住」这个状态，一旦发现键没按着就会主动
     * {@code releaseUsingItem}——这正是「用 gameMode.useItem 举盾就抽风、没格挡」的根源。
     * 举盾 / 收盾由 {@code SneakShieldHandler} 统一决策（收盾时会先把 isShieldUp() 置回 false，
     * 所以这里不会误拦我们自己发起的释放）。</p>
     */
    @Inject(method = "releaseUsingItem", at = @At("HEAD"), cancellable = true)
    public void releaseUsingItemBefore(Player player, CallbackInfo ci) {
        if (SneakShieldHandler.INSTANCE.isShieldUp() && touchController$isUsingShield(player)) {
            ci.cancel();
        }
    }

    /** 玩家此刻正在用「盾牌」吗（正在吃 / 拉弓都不算）。 */
    @Unique
    private static boolean touchController$isUsingShield(Player player) {
        return player != null && player.isUsingItem() && touchController$isShield(player.getUseItem());
    }

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    public void useItemBefore(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        // 右键（使用键）**永远不举盾**：举盾只由「潜行举盾」接管。
        // 潜行举盾生效时（isShieldUp）放行，让原版自己把盾举起来；
        // 其余情况下的「使用盾牌」直接吞掉，返回 PASS 让原版继续试另一只手。
        // 只拦 useItem，不影响 useItemOn —— 开箱子 / 按按钮 / 放方块照常。
        if (!SneakShieldHandler.INSTANCE.isShieldUp() && touchController$isShield(player.getItemInHand(hand))) {
            cir.setReturnValue(InteractionResult.PASS);
            return;
        }
        touchController$interactItemPlayer = player;
        touchController$interactItemHand = hand;
    }

    /**
     * 是不是盾牌。
     *
     * <p>以前这里还把「{@code UseAnim.BLOCK} 使用动画」也算成盾牌，结果把所有用格挡姿势的
     * 右键物品（孔雀羽扇这类模组道具）一起算进去：它们的 {@code useItem} 被这里吞成 PASS，
     * 表现就是「右键完全没反应」。现在只认真正的盾牌类（原版盾 + 继承 {@code ShieldItem} 的模组盾），
     * 宁可有模组盾右键能举起来，也不能把别的物品一起禁掉。</p>
     */
    @Unique
    private static boolean touchController$isShield(net.minecraft.world.item.ItemStack stack) {
        return top.fifthlight.touchcontroller.common_1_20_1.gal.ShieldWhitelist.INSTANCE.matches(stack);
    }

    @Redirect(
            method = "useItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V"
            )
    )
    public void useItemSendPacket(ClientPacketListener instance, Packet<?> packet) {
        var newPacket = touchController$interactBefore(touchController$interactItemPlayer, touchController$interactItemHand, true);
        if (newPacket == null) {
            instance.send(packet);
        } else {
            instance.send(newPacket);
        }
        touchController$interactItemPlayer = null;
        touchController$interactItemHand = null;
    }

    @Inject(
            method = "useItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;startPrediction(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/client/multiplayer/prediction/PredictiveAction;)V",
                    ordinal = 0,
                    shift = At.Shift.AFTER
            )
    )
    public void useItemAfter(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        touchController$interactAfter(player);
    }
}
