package top.fifthlight.touchcontroller.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.LayeredDraw;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fifthlight.touchcontroller.HudLayer;

@Mixin(Gui.class)
public abstract class HudRenderMixin {
    @Shadow
    @Final
    private LayeredDraw layers;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void initLayers(Minecraft minecraft, CallbackInfo ci) {
        layers.add(HudLayer.INSTANCE);
    }
}