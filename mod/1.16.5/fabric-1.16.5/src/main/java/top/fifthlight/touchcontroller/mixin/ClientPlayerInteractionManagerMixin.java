package top.fifthlight.touchcontroller.mixin;

import kotlin.Pair;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
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
import top.fifthlight.touchcontroller.common.event.BlockBreakEvents;
import top.fifthlight.touchcontroller.common.helper.CrosshairTargetHelper;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {
    @Shadow
    @Final
    private ClientPlayNetworkHandler networkHandler;
    @Unique
    private boolean resetPlayerLookTarget;

    @Unique
    private float prevYaw;

    @Unique
    private float prevPitch;

    @Inject(
            method = "breakBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/Block;onBroken(Lnet/minecraft/world/WorldAccess;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)V"
            )
    )
    public void breakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        BlockBreakEvents.INSTANCE.afterBlockBreak();
    }


    @Unique
    private boolean crosshairAimingContainItem(Item item) {
        GlobalConfigHolder globalConfigHolder = KoinJavaComponent.get(GlobalConfigHolder.class);
        GlobalConfig globalConfig = globalConfigHolder.getConfig().getValue();
        return ItemFactoryImplKt.contains(globalConfig.getItem().getCrosshairAimingItems(), item);
    }

    @Unique
    public void interactBefore(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);
        if (!crosshairAimingContainItem(itemStack.getItem())) {
            return;
        }

        prevYaw = player.yaw;
        prevPitch = player.pitch;

        Pair<Float, Float> rotation = CrosshairTargetHelper.calculatePlayerRotation(CrosshairTargetHelper.INSTANCE.getLastCrosshairDirection());
        float yaw = rotation.getFirst();
        float pitch = rotation.getSecond();
        player.yaw = yaw;
        player.pitch = pitch;
        this.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookOnly(yaw, pitch, player.isOnGround()));
        resetPlayerLookTarget = true;
    }

    @Unique
    public void interactAfter(PlayerEntity player) {
        if (!resetPlayerLookTarget) {
            return;
        }
        resetPlayerLookTarget = false;
        player.yaw = prevYaw;
        player.pitch = prevPitch;
        this.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookOnly(player.yaw, player.pitch, player.isOnGround()));
    }

    @Inject(
            method = "interactBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;getStackInHand(Lnet/minecraft/util/Hand;)Lnet/minecraft/item/ItemStack;",
                    ordinal = 0
            )
    )
    public void interactBlockBefore(ClientPlayerEntity player, ClientWorld world, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        interactBefore(player, hand);
    }

    @Inject(
            method = "interactBlock",
            at = @At("RETURN")
    )
    public void interactBlockAfter(ClientPlayerEntity player, ClientWorld world, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        interactAfter(player);
    }

    @Inject(
            method = "interactItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;syncSelectedSlot()V",
                    ordinal = 0
            )
    )
    public void interactItemBefore(PlayerEntity player, World world, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        interactBefore(player, hand);
    }

    @Inject(
            method = "interactItem",
            at = @At("RETURN")
    )
    public void interactItemAfter(PlayerEntity player, World world, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        interactAfter(player);
    }
}
