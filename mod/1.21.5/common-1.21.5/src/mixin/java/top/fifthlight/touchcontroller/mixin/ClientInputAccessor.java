package top.fifthlight.touchcontroller.mixin;

import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fifthlight.touchcontroller.common_1_21_5.event.KeyboardInputEvents;

@Mixin(ClientInput.class)
public interface ClientInputAccessor {
    @Accessor("moveVector")
    void touchcontroller$setMoveVector(Vec2 moveVector);
}
