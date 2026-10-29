package top.fifthlight.touchcontroller.mixin;

import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.NewChatGui;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import top.fifthlight.touchcontroller.helper.ChatComponentWithMessages;

import java.util.List;

@Mixin(NewChatGui.class)
public abstract class ChatComponentMixin implements ChatComponentWithMessages {
    @Shadow
    @Final
    private List<ChatLine<ITextComponent>> allMessages;

    @Override
    public List<ChatLine<ITextComponent>> touchcontroller$getMessages() {
        return allMessages;
    }
}
