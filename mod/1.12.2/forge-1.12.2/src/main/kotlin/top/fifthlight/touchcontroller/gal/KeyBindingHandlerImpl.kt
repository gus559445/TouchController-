package top.fifthlight.touchcontroller.gal

import net.minecraft.client.Minecraft
import net.minecraft.client.settings.KeyBinding
import net.minecraft.util.text.TextComponentTranslation
import top.fifthlight.combine.data.Text
import top.fifthlight.combine.platform.TextImpl
import top.fifthlight.touchcontroller.common.gal.DefaultKeyBindingType
import top.fifthlight.touchcontroller.common.gal.KeyBindingHandler
import top.fifthlight.touchcontroller.common.gal.KeyBindingState

private class KeyBindingStateImpl(
    private val keyBinding: KeyBinding,
) : KeyBindingState() {
    override val id: String
        get() = keyBinding.keyDescription

    override val name: Text
        get() = TextImpl(TextComponentTranslation(keyBinding.keyDescription))

    override val categoryId: String
        get() = keyBinding.keyCategory

    override val categoryName: Text
        get() = TextImpl(TextComponentTranslation(keyBinding.keyCategory))

    override fun click() {
        keyBinding.pressTime++
    }

    override fun haveClickCount(): Boolean = keyBinding.pressTime > 0
}

object KeyBindingHandlerImpl : KeyBindingHandler() {
    private val client = Minecraft.getMinecraft()
    private val options = client.gameSettings
    private val state = mutableMapOf<KeyBinding, KeyBindingStateImpl>()

    private fun DefaultKeyBindingType.toMinecraft() = when (this) {
        DefaultKeyBindingType.ATTACK -> options.keyBindAttack
        DefaultKeyBindingType.USE -> options.keyBindUseItem
        DefaultKeyBindingType.INVENTORY -> options.keyBindInventory
        DefaultKeyBindingType.SWAP_HANDS -> options.keyBindSwapHands
        DefaultKeyBindingType.SNEAK -> options.keyBindSneak
        DefaultKeyBindingType.SPRINT -> options.keyBindSprint
        DefaultKeyBindingType.JUMP -> options.keyBindJump
        DefaultKeyBindingType.PLAYER_LIST -> options.keyBindPlayerList
        DefaultKeyBindingType.LEFT -> options.keyBindLeft
        DefaultKeyBindingType.RIGHT -> options.keyBindRight
        DefaultKeyBindingType.UP -> options.keyBindForward
        DefaultKeyBindingType.DOWN -> options.keyBindBack
    }

    fun isDown(key: KeyBinding) = state[key]?.let { it.clicked || it.locked } == true

    private fun getState(key: KeyBinding) = state.getOrPut(key) {
        KeyBindingStateImpl(key)
    }

    override fun getState(type: DefaultKeyBindingType): KeyBindingState {
        return getState(type.toMinecraft())
    }

    override fun getState(id: String): KeyBindingState? = KeyBinding.KEYBIND_ARRAY[id]?.let { getState(it) }

    override fun getAllStates(): Map<String, KeyBindingState> =
        KeyBinding.KEYBIND_ARRAY.mapValues { (_, binding) -> getState(binding) }

    override fun getExistingStates(): Collection<KeyBindingState> = state.values
}
