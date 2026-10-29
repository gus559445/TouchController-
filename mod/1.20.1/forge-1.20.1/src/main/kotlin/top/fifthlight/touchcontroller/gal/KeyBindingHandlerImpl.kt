package top.fifthlight.touchcontroller.gal

import net.minecraft.client.KeyMapping
import top.fifthlight.touchcontroller.common_1_20_x.gal.AbstractKeyBindingHandlerImpl

object KeyBindingHandlerImpl : AbstractKeyBindingHandlerImpl() {
    override fun getKeyBinding(name: String): KeyMapping? = KeyMapping.ALL[name]

    override fun getAllKeyBinding(): Map<String, KeyMapping> = KeyMapping.ALL
}
