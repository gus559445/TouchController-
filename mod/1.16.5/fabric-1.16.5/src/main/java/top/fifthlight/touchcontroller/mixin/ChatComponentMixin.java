package top.fifthlight.touchcontroller.mixin;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import top.fifthlight.touchcontroller.helper.ChatComponentWithMessages;

import java.util.List;

@Mixin(ChatHud.class)
public abstract class ChatComponentMixin implements ChatComponentWithMessages {
    @Shadow
    @Final
    private List<ChatHudLine<Text>> messages;

    @Override
    public List<ChatHudLine<Text>> touchcontroller$getMessages() {
        return messages;
    }
}
