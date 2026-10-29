package top.fifthlight.touchcontroller.mixin;

import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fifthlight.touchcontroller.common_1_21_3_1_21_4.event.KeyboardInputEvents;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {
    @Inject(
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/player/KeyboardInput;leftImpulse:F",
                    ordinal = 0,
                    shift = At.Shift.AFTER
            ),
            method = "tick"
    )
    private void tick(CallbackInfo info) {
        KeyboardInputEvents.INSTANCE.onEndTick((KeyboardInput) (Object) this);
    }
}
