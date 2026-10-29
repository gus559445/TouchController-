package top.fifthlight.touchcontroller.mixin;

import kotlin.Pair;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.multiplayer.PlayerController;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPlayerPacket;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.World;
import org.koin.java.KoinJavaComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.fifthlight.combine.platform.ItemFactoryImplKt;
import top.fifthlight.touchcontroller.common.config.GlobalConfig;
import top.fifthlight.touchcontroller.common.config.GlobalConfigHolder;
import top.fifthlight.touchcontroller.common.helper.CrosshairTargetHelper;

@Mixin(PlayerController.class)
public class ClientPlayerInteractionManagerMixin {
    @Shadow
    @Final
    private ClientPlayNetHandler connection;

    @Unique
    private boolean touchController$resetPlayerLookTarget;

    @Unique
    private float touchController$prevYaw;

    @Unique
    private float touchController$prevPitch;

    @Unique
    private boolean touchController$crosshairAimingContainItem(Item item) {
        GlobalConfigHolder globalConfigHolder = KoinJavaComponent.get(GlobalConfigHolder.class);
        GlobalConfig globalConfig = globalConfigHolder.getConfig().getValue();
        return ItemFactoryImplKt.contains(globalConfig.getItem().getCrosshairAimingItems(), item);
    }

    @Unique
    public void touchController$interactBefore(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (!touchController$crosshairAimingContainItem(itemStack.getItem())) {
            return;
        }

        touchController$prevYaw = player.yRot;
        touchController$prevPitch = player.xRot;

        Pair<Float, Float> rotation = CrosshairTargetHelper.calculatePlayerRotation(CrosshairTargetHelper.INSTANCE.getLastCrosshairDirection());
        float yaw = rotation.getFirst();
        float pitch = rotation.getSecond();
        player.yRot = yaw;
        player.xRot = pitch;
        this.connection.send(new CPlayerPacket.RotationPacket(yaw, pitch, player.isOnGround()));
        touchController$resetPlayerLookTarget = true;
    }

    @Unique
    public void touchController$interactAfter(PlayerEntity player) {
        if (!touchController$resetPlayerLookTarget) {
            return;
        }
        touchController$resetPlayerLookTarget = false;
        player.yRot = touchController$prevYaw;
        player.xRot = touchController$prevPitch;
        this.connection.send(new CPlayerPacket.RotationPacket(player.yRot, player.xRot, player.isOnGround()));
    }

    @Inject(
            method = "useItemOn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/entity/player/ClientPlayerEntity;getItemInHand(Lnet/minecraft/util/Hand;)Lnet/minecraft/item/ItemStack;",
                    ordinal = 0
            )
    )
    public void useItemOnBefore(ClientPlayerEntity player, ClientWorld world, Hand hand, BlockRayTraceResult hitResult, CallbackInfoReturnable<ActionResultType> cir) {
        touchController$interactBefore(player, hand);
    }

    @Inject(
            method = "useItemOn",
            at = @At("RETURN")
    )
    public void useItemOnAfter(ClientPlayerEntity player, ClientWorld world, Hand hand, BlockRayTraceResult hitResult, CallbackInfoReturnable<ActionResultType> cir) {
        touchController$interactAfter(player);
    }

    @Inject(
            method = "useItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/PlayerController;ensureHasSentCarriedItem()V",
                    ordinal = 0
            )
    )
    public void interactItemBefore(PlayerEntity player, World world, Hand hand, CallbackInfoReturnable<ActionResultType> cir) {
        touchController$interactBefore(player, hand);
    }

    @Inject(
            method = "useItem",
            at = @At("RETURN")
    )
    public void interactItemAfter(PlayerEntity player, World world, Hand hand, CallbackInfoReturnable<ActionResultType> cir) {
        touchController$interactAfter(player);
    }
}
