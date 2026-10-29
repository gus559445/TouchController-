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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.fifthlight.combine.platform_1_20_4.ItemFactoryImplKt;
import top.fifthlight.touchcontroller.common.config.GlobalConfigHolder;
import top.fifthlight.touchcontroller.common.helper.CrosshairTargetHelper;
import top.fifthlight.touchcontroller.helper.GameModeWithBreakingProgress;
import top.fifthlight.touchcontroller.helper.SyncableGameMode;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin implements GameModeWithBreakingProgress, SyncableGameMode {
    @Shadow
    private float destroyProgress;

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

    @Inject(method = "useItem", at = @At("HEAD"))
    public void useItemBefore(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        touchController$interactItemPlayer = player;
        touchController$interactItemHand = hand;
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
