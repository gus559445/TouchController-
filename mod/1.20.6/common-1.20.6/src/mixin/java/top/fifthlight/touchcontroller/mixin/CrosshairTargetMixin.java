package top.fifthlight.touchcontroller.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import top.fifthlight.touchcontroller.common.helper.CrosshairTargetHelper;

@Mixin(GameRenderer.class)
public abstract class CrosshairTargetMixin {
    @Unique
    private static Vec3 touchController$currentDirection;
    @Shadow
    @Final
    private Camera mainCamera;

    @Shadow
    public abstract Matrix4f getProjectionMatrix(double pFov);

    @Shadow
    protected abstract double getFov(Camera pActiveRenderInfo, float pPartialTicks, boolean pUseFOVSetting);

    @Redirect(
            method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;pick(DFZ)Lnet/minecraft/world/phys/HitResult;",
                    ordinal = 0
            )
    )
    private HitResult cameraRaycast(Entity instance, double pHitDistance, float pPartialTicks, boolean pHitFluids) {
        var fov = getFov(mainCamera, pPartialTicks, true);
        var cameraPitch = Math.toRadians(instance.getViewXRot(pPartialTicks));
        var cameraYaw = Math.toRadians(instance.getViewYRot(pPartialTicks));

        var position = instance.getEyePosition(pPartialTicks);
        var projectionMatrix = getProjectionMatrix(fov);
        var direction = CrosshairTargetHelper.getCrosshairDirection(projectionMatrix, cameraPitch, cameraYaw);
        CrosshairTargetHelper.INSTANCE.setLastCrosshairDirection(direction);

        touchController$currentDirection = new Vec3(direction.x, direction.y, direction.z);
        var interactionTarget = position.add(direction.x * pHitDistance, direction.y * pHitDistance, direction.z * pHitDistance);
        var clipContextFluid = pHitFluids ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE;
        return instance.level().clip(new ClipContext(position, interactionTarget, ClipContext.Block.OUTLINE, clipContextFluid, instance));
    }

    @Redirect(
            method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;getViewVector(F)Lnet/minecraft/world/phys/Vec3;",
                    ordinal = 0
            )
    )
    private Vec3 getRotationVec(Entity instance, float tickDelta) {
        return touchController$currentDirection;
    }
}
