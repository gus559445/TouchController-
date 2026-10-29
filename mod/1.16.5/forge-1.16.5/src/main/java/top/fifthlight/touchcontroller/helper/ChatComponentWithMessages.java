package top.fifthlight.touchcontroller.helper;

import net.minecraft.client.gui.ChatLine;
import net.minecraft.util.text.ITextComponent;

import java.util.List;

public interface ChatComponentWithMessages {
    List<ChatLine<ITextComponent>> touchcontroller$getMessages();
}