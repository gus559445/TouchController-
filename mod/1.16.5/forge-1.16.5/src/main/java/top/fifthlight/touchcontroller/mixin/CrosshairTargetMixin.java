package top.fifthlight.touchcontroller.mixin;

import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import top.fifthlight.touchcontroller.common.helper.CrosshairTargetHelper;
import top.fifthlight.touchcontroller.helper.IntoJomlMatrix4f;

@Mixin(GameRenderer.class)
public abstract class CrosshairTargetMixin {
    @Unique
    private static Vector3d currentDirection;
    @Shadow
    @Final
    private ActiveRenderInfo mainCamera;

    @Shadow
    public abstract Matrix4f getProjectionMatrix(ActiveRenderInfo camera, float tickDelta, boolean changingFov);

    @Redirect(
            method = "pick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;pick(DFZ)Lnet/minecraft/util/math/RayTraceResult;",
                    ordinal = 0
            )
    )
    private RayTraceResult cameraRaycast(Entity instance, double maxDistance, float tickDelta, boolean includeFluids) {
        double cameraPitch = Math.toRadians(instance.getViewXRot(tickDelta));
        double cameraYaw = Math.toRadians(instance.getViewYRot(tickDelta));

        Vector3d position = instance.getEyePosition(tickDelta);
        org.joml.Matrix4f projectionMatrix = ((IntoJomlMatrix4f) (Object) getProjectionMatrix(mainCamera, tickDelta, true)).touchController$into();
        org.joml.Vector3d direction = CrosshairTargetHelper.getCrosshairDirection(projectionMatrix, cameraPitch, cameraYaw);
        CrosshairTargetHelper.INSTANCE.setLastCrosshairDirection(direction);

        currentDirection = new Vector3d(direction.x, direction.y, direction.z);
        Vector3d interactionTarget = position.add(direction.x * maxDistance, direction.y * maxDistance, direction.z * maxDistance);
        RayTraceContext.FluidMode fluidHandling = includeFluids ? RayTraceContext.FluidMode.ANY : RayTraceContext.FluidMode.NONE;
        return instance.level.clip(new RayTraceContext(position, interactionTarget, RayTraceContext.BlockMode.OUTLINE, fluidHandling, instance));
    }

    @Redirect(
            method = "pick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;getViewVector(F)Lnet/minecraft/util/math/vector/Vector3d;",
                    ordinal = 0
            )
    )
    private Vector3d getRotationVec(Entity instance, float tickDelta) {
        return currentDirection;
    }
}
