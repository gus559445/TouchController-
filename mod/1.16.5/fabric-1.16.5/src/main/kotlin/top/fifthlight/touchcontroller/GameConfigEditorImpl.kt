package top.fifthlight.touchcontroller

import net.minecraft.client.MinecraftClient
import net.minecraft.client.option.GameOptions
import top.fifthlight.touchcontroller.common.config.GameConfigEditor
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object GameConfigEditorImpl : GameConfigEditor {
    private val pendingCallbackLock = ReentrantLock()
    private var pendingCallbacks: MutableList<GameConfigEditor.Callback>? = mutableListOf()

    private class EditorImpl(val options: GameOptions) : GameConfigEditor.Editor {
        override var autoJump: Boolean
            get() = options.autoJump
            set(value) {
                options.autoJump = value
            }
    }

    fun executePendingCallback() {
        pendingCallbackLock.withLock {
            val callbacks = pendingCallbacks
            if (callbacks == null) {
                return
            }
            pendingCallbacks = null
            with(EditorImpl(MinecraftClient.getInstance().options)) {
                callbacks.forEach { callback ->
                    callback.invoke(this)
                }
                options.write()
            }
        }
    }

    override fun submit(callback: GameConfigEditor.Callback) {
        pendingCallbackLock.withLock {
            pendingCallbacks?.add(callback) ?: run {
                with(EditorImpl(MinecraftClient.getInstance().options)) {
                    callback.invoke(this)
                    options.write()
                }
            }
        }
    }
}
