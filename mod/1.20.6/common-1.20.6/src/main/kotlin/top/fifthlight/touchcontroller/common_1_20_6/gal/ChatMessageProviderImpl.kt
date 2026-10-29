package top.fifthlight.touchcontroller.common_1_20_6.gal

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import net.minecraft.client.Minecraft
import net.minecraft.util.StringUtil
import org.apache.commons.lang3.StringUtils
import top.fifthlight.combine.platform_1_20_x.TextImpl
import top.fifthlight.touchcontroller.common.gal.ChatMessage
import top.fifthlight.touchcontroller.common.gal.ChatMessageProvider
import top.fifthlight.touchcontroller.helper.ChatComponentWithMessages

object ChatMessageProviderImpl : ChatMessageProvider {
    private val client = Minecraft.getInstance()

    override fun getMessages(): PersistentList<ChatMessage> =
        (client.gui.chat as ChatComponentWithMessages).`touchcontroller$getMessages`()
            .reversed()
            .map { ChatMessage(message = TextImpl(it.content())) }
            .toPersistentList()

    override fun sendMessage(message: String) {
        val message = StringUtil.trimChatMessage(StringUtils.normalizeSpace(message.trim()))
        if (!message.isEmpty()) {
            client.gui.chat.addRecentChat(message)
            if (message.startsWith("/")) {
                client.player!!.connection.sendCommand(message.substring(1))
            } else {
                client.player!!.connection.sendChat(message)
            }
        }
    }
}