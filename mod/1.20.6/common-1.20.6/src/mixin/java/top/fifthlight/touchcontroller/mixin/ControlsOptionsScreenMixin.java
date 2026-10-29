package top.fifthlight.touchcontroller.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.controls.ControlsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fifthlight.combine.platform_1_20_x.TextImpl;
import top.fifthlight.touchcontroller.common.ui.screen.ConfigScreenKt;

@Mixin(ControlsScreen.class)
public abstract class ControlsOptionsScreenMixin {
    @Shadow
    private OptionsList list;

    @Inject(at = @At("TAIL"), method = "init")
    protected void init(CallbackInfo ci) {
        var client = Minecraft.getInstance();
        var screen = (ControlsScreen) (Object) this;

        var textObj = ConfigScreenKt.getConfigScreenButtonText();
        if (textObj instanceof TextImpl) {
            textObj = ((TextImpl) textObj).getInner();
        }
        Component text = (Component) textObj;

        list.addSmall(
                Button
                        .builder(
                                text,
                                btn -> client.setScreen((Screen) ConfigScreenKt.getConfigScreen(screen))
                        )
                        .bounds(screen.width / 2 - 155, screen.height / 6 + 60, 150, 20)
                        .build(),
                null
        );
    }
}