package top.fifthlight.touchcontroller.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import top.fifthlight.touchcontroller.helper.DrawContextWithBuffer;

@Mixin(GuiGraphics.class)
public abstract class DrawContextMixin implements DrawContextWithBuffer {
    @Shadow
    @Final
    private MultiBufferSource.BufferSource bufferSource;

    @Override
    public MultiBufferSource.BufferSource touchcontroller$getVertexConsumers() {
        return bufferSource;
    }
}
