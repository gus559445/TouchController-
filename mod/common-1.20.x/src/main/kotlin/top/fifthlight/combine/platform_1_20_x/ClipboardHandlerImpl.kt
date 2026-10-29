package top.fifthlight.combine.platform_1_20_x

import net.minecraft.client.Minecraft
import top.fifthlight.combine.input.input.ClipboardHandler

object ClipboardHandlerImpl : ClipboardHandler {
    private val client by lazy { Minecraft.getInstance() }

    override var text: String
        get() = client.keyboardHandler.clipboard
        set(value) {
            client.keyboardHandler.clipboard = value
        }
}