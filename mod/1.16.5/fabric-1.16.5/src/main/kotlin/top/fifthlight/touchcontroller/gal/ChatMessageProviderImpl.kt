package top.fifthlight.touchcontroller.gal

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import net.minecraft.client.MinecraftClient
import org.apache.commons.lang3.StringUtils
import top.fifthlight.combine.platform.TextImpl
import top.fifthlight.touchcontroller.common.gal.ChatMessage
import top.fifthlight.touchcontroller.common.gal.ChatMessageProvider
import top.fifthlight.touchcontroller.helper.ChatComponentWithMessages

object ChatMessageProviderImpl : ChatMessageProvider {
    private val client = MinecraftClient.getInstance()

    override fun getMessages(): PersistentList<ChatMessage> =
        (client.inGameHud.chatHud as ChatComponentWithMessages).`touchcontroller$getMessages`()
            .reversed()
            .map { ChatMessage(message = TextImpl(it.text)) }
            .toPersistentList()

    override fun sendMessage(message: String) {
        val message = StringUtils.normalizeSpace(message.trim())
        if (!message.isEmpty()) {
            client.inGameHud.chatHud.addToMessageHistory(message)
            client.player!!.sendChatMessage(message)
        }
    }
}