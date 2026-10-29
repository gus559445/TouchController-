package top.fifthlight.touchcontroller.mixin;

import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(KeyBinding.class)
public interface KeyMappingGetterMixin {
    @Accessor("keysById")
    static Map<String, KeyBinding> touchcontroller$getAllKeyMappings() {
        throw new AssertionError();
    }
}