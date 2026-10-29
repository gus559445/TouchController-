package top.fifthlight.touchcontroller.mixin;

import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import top.fifthlight.touchcontroller.helper.IntoJomlMatrix4f;

@Mixin(Matrix4f.class)
public class Matrix4fMixin implements IntoJomlMatrix4f {
    @Shadow
    protected float a00;
    @Shadow
    protected float a01;
    @Shadow
    protected float a02;
    @Shadow
    protected float a03;
    @Shadow
    protected float a10;
    @Shadow
    protected float a11;
    @Shadow
    protected float a12;
    @Shadow
    protected float a13;
    @Shadow
    protected float a20;
    @Shadow
    protected float a21;
    @Shadow
    protected float a22;
    @Shadow
    protected float a23;
    @Shadow
    protected float a30;
    @Shadow
    protected float a31;
    @Shadow
    protected float a32;
    @Shadow
    protected float a33;

    @Override
    public org.joml.Matrix4f touchController$into() {
        return new org.joml.Matrix4f(
                a00, a01, a02, a03,
                a10, a11, a12, a13,
                a20, a21, a22, a23,
                a30, a31, a32, a33
        );
    }
}
