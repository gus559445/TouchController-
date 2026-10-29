package top.fifthlight.touchcontroller.gal

import net.minecraft.client.Minecraft
import net.minecraft.client.settings.KeyBinding
import net.minecraft.util.text.TranslationTextComponent
import top.fifthlight.combine.data.Text
import top.fifthlight.combine.platform.TextImpl
import top.fifthlight.touchcontroller.common.gal.DefaultKeyBindingType
import top.fifthlight.touchcontroller.common.gal.KeyBindingHandler
import top.fifthlight.touchcontroller.common.gal.KeyBindingState
import top.fifthlight.touchcontroller.helper.ClickableKeyBinding

private fun KeyBinding.click() {
    (this as ClickableKeyBinding).`touchController$click`()
}

private fun KeyBinding.getClickCount() = (this as ClickableKeyBinding).`touchController$getClickCount`()

private class KeyBindingStateImpl(
    private val keyBinding: KeyBinding,
) : KeyBindingState() {
    override val id: String
        get() = keyBinding.name

    override val name: Text
        get() = TextImpl(TranslationTextComponent(keyBinding.name))

    override val categoryId: String
        get() = keyBinding.category

    override val categoryName: Text
        get() = TextImpl(TranslationTextComponent(keyBinding.category))

    override fun click() {
        keyBinding.click()
    }

    override fun haveClickCount(): Boolean = keyBinding.getClickCount() > 0
}

object KeyBindingHandlerImpl : KeyBindingHandler() {
    private val client = Minecraft.getInstance()
    private val options = client.options
    private val state = mutableMapOf<KeyBinding, KeyBindingStateImpl>()

    private fun DefaultKeyBindingType.toMinecraft() = when (this) {
        DefaultKeyBindingType.ATTACK -> options.keyAttack
        DefaultKeyBindingType.USE -> options.keyUse
        DefaultKeyBindingType.INVENTORY -> options.keyInventory
        DefaultKeyBindingType.SWAP_HANDS -> options.keySwapOffhand
        DefaultKeyBindingType.SNEAK -> options.keyShift
        DefaultKeyBindingType.SPRINT -> options.keySprint
        DefaultKeyBindingType.JUMP -> options.keyJump
        DefaultKeyBindingType.PLAYER_LIST -> options.keyPlayerList
        DefaultKeyBindingType.LEFT -> options.keyLeft
        DefaultKeyBindingType.RIGHT -> options.keyRight
        DefaultKeyBindingType.UP -> options.keyUp
        DefaultKeyBindingType.DOWN -> options.keyDown
    }

    fun isDown(key: KeyBinding) = state[key]?.let { it.clicked || it.locked } == true

    private fun getState(key: KeyBinding) = state.getOrPut(key) {
        KeyBindingStateImpl(key)
    }

    override fun getState(type: DefaultKeyBindingType): KeyBindingState {
        return getState(type.toMinecraft())
    }

    override fun getState(id: String): KeyBindingState? =
        KeyBinding.ALL[id]?.let { getState(it) }

    override fun getAllStates(): Map<String, KeyBindingState> =
        KeyBinding.ALL.mapValues { (_, key) -> getState(key) }

    override fun getExistingStates(): Collection<KeyBindingState> = state.values
}
