package top.fifthlight.touchcontroller.common_1_20_x.gal

import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import top.fifthlight.combine.data.Text
import top.fifthlight.combine.platform_1_20_x.TextImpl
import top.fifthlight.touchcontroller.common.gal.DefaultKeyBindingType
import top.fifthlight.touchcontroller.common.gal.KeyBindingHandler
import top.fifthlight.touchcontroller.common.gal.KeyBindingState
import top.fifthlight.touchcontroller.helper.ClickableKeyBinding

private fun KeyMapping.click() {
    (this as ClickableKeyBinding).`touchController$click`()
}

private fun KeyMapping.getClickCount() = (this as ClickableKeyBinding).`touchController$getClickCount`()

private class KeyBindingStateImpl(
    private val keyBinding: KeyMapping,
) : KeyBindingState() {
    override val id: String
        get() = keyBinding.name

    override val name: Text
        get() = TextImpl(Component.translatable(keyBinding.name))

    override val categoryId: String
        get() = keyBinding.category

    override val categoryName: Text
        get() = TextImpl(Component.translatable(keyBinding.category))

    override fun click() {
        keyBinding.click()
    }

    override fun haveClickCount(): Boolean = keyBinding.getClickCount() > 0
}

abstract class AbstractKeyBindingHandlerImpl : KeyBindingHandler() {
    private val client = Minecraft.getInstance()
    private val options = client.options
    private val state = mutableMapOf<KeyMapping, KeyBindingStateImpl>()

    abstract fun getKeyBinding(name: String): KeyMapping?
    abstract fun getAllKeyBinding(): Map<String, KeyMapping>

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

    fun isDown(key: KeyMapping) = state[key]?.let { it.clicked || it.locked } == true

    private fun getState(key: KeyMapping) = state.getOrPut(key) {
        KeyBindingStateImpl(key)
    }

    override fun getState(type: DefaultKeyBindingType): KeyBindingState {
        return getState(type.toMinecraft())
    }

    override fun getState(id: String): KeyBindingState? = getKeyBinding(id)?.let(::getState)

    override fun getAllStates(): Map<String, KeyBindingState> =
        getAllKeyBinding().mapValues { (_, value) -> getState(value) }

    override fun getExistingStates(): Collection<KeyBindingState> = state.values
}
