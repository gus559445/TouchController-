package top.fifthlight.touchcontroller

import net.minecraft.client.Minecraft
import net.minecraft.client.settings.GameSettings
import top.fifthlight.touchcontroller.common.config.GameConfigEditor
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object GameConfigEditorImpl : GameConfigEditor {
    private val pendingCallbackLock = ReentrantLock()
    private var pendingCallbacks: MutableList<GameConfigEditor.Callback>? = mutableListOf()

    private class EditorImpl(val options: GameSettings) : GameConfigEditor.Editor {
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
            if (callbacks.isNotEmpty()) {
                with(EditorImpl(Minecraft.getMinecraft().gameSettings)) {
                    callbacks.forEach { callback ->
                        callback.invoke(this)
                    }
                    options.saveOptions()
                }
            }
        }
    }

    override fun submit(callback: GameConfigEditor.Callback) {
        pendingCallbackLock.withLock {
            pendingCallbacks?.add(callback) ?: run {
                with(EditorImpl(Minecraft.getMinecraft().gameSettings)) {
                    callback.invoke(this)
                    options.saveOptions()
                }
            }
        }
    }
}
