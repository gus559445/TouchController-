package top.fifthlight.touchcontroller.helper;

import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.Text;

import java.util.List;

public interface ChatComponentWithMessages {
    List<ChatHudLine<Text>> touchcontroller$getMessages();
}