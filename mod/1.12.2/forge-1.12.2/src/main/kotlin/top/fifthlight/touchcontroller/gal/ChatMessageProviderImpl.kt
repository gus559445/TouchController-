package top.fifthlight.touchcontroller.gal

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import net.minecraft.client.Minecraft
import org.apache.commons.lang3.StringUtils
import top.fifthlight.combine.platform.TextImpl
import top.fifthlight.touchcontroller.common.gal.ChatMessage
import top.fifthlight.touchcontroller.common.gal.ChatMessageProvider

object ChatMessageProviderImpl : ChatMessageProvider {
    private val client = Minecraft.getMinecraft()

    override fun getMessages(): PersistentList<ChatMessage> =
        client.ingameGUI.chatGUI.chatLines
            .reversed()
            .map { ChatMessage(message = TextImpl(it.chatComponent)) }
            .toPersistentList()

    override fun sendMessage(message: String) {
        val message = StringUtils.normalizeSpace(message.trim())
        if (!message.isEmpty()) {
            client.ingameGUI.chatGUI.addToSentMessages(message)
            client.player!!.sendChatMessage(message)
        }
    }
}