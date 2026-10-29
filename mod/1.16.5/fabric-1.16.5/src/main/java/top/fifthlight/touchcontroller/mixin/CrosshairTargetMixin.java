package top.fifthlight.touchcontroller.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Vector3d;
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
    @Shadow
    @Final
    private Camera camera;

    @Shadow
    public abstract Matrix4f getBasicProjectionMatrix(Camera camera, float tickDelta, boolean changingFov);

    @Unique
    private static Vec3d currentDirection;

    @Redirect(
            method = "updateTargetedEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;raycast(DFZ)Lnet/minecraft/util/hit/HitResult;",
                    ordinal = 0
            )
    )
    private HitResult cameraRaycast(Entity instance, double maxDistance, float tickDelta, boolean includeFluids) {
        double cameraPitch = Math.toRadians(instance.getPitch(tickDelta));
        double cameraYaw = Math.toRadians(instance.getYaw(tickDelta));

        Vec3d position = instance.getCameraPosVec(tickDelta);
        org.joml.Matrix4f projectionMatrix = ((IntoJomlMatrix4f) (Object) getBasicProjectionMatrix(camera, tickDelta, true)).touchController$into();
        Vector3d direction = CrosshairTargetHelper.getCrosshairDirection(projectionMatrix, cameraPitch, cameraYaw);
        CrosshairTargetHelper.INSTANCE.setLastCrosshairDirection(direction);

        currentDirection = new Vec3d(direction.x, direction.y, direction.z);
        Vec3d interactionTarget = position.add(direction.x * maxDistance, direction.y * maxDistance, direction.z * maxDistance);
        RaycastContext.FluidHandling fluidHandling = includeFluids ? RaycastContext.FluidHandling.ANY : RaycastContext.FluidHandling.NONE;
        return instance.world.raycast(new RaycastContext(position, interactionTarget, RaycastContext.ShapeType.OUTLINE, fluidHandling, instance));
    }

    @Redirect(
            method = "updateTargetedEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;getRotationVec(F)Lnet/minecraft/util/math/Vec3d;",
                    ordinal = 0
            )
    )
    private Vec3d getRotationVec(Entity instance, float tickDelta) {
        return currentDirection;
    }
}
