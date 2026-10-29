package top.fifthlight.combine.platform_1_20_x

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.mojang.blaze3d.systems.RenderSystem
import kotlinx.coroutines.CoroutineScope
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import org.koin.compose.KoinContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import org.lwjgl.glfw.GLFW
import top.fifthlight.combine.data.LocalDataComponentTypeFactory
import top.fifthlight.combine.data.LocalItemFactory
import top.fifthlight.combine.data.LocalTextFactory
import top.fifthlight.combine.input.input.LocalClipboard
import top.fifthlight.combine.input.key.KeyEvent
import top.fifthlight.combine.input.pointer.PointerButton
import top.fifthlight.combine.input.pointer.PointerEvent
import top.fifthlight.combine.input.pointer.PointerEventType
import top.fifthlight.combine.input.pointer.PointerType
import top.fifthlight.combine.node.CombineOwner
import top.fifthlight.combine.node.LocalInputHandler
import top.fifthlight.combine.platform.mapKeyCode
import top.fifthlight.combine.platform.mapModifier
import top.fifthlight.combine.screen.LocalOnDismissRequestDispatcher
import top.fifthlight.combine.screen.LocalScreenFactory
import top.fifthlight.combine.screen.OnDismissRequestDispatcher
import top.fifthlight.combine.sound.LocalSoundManager
import top.fifthlight.combine.util.CloseHandler
import top.fifthlight.combine.util.LocalCloseHandler
import top.fifthlight.data.IntSize
import top.fifthlight.data.Offset
import top.fifthlight.touchcontroller.common.gal.GameDispatcher
import top.fifthlight.touchcontroller.common.input.InputManager
import kotlin.coroutines.CoroutineContext

val LocalScreen = staticCompositionLocalOf<Screen> { error("No screen in context") }

private class ScreenCloseHandler(private val screen: Screen) : CloseHandler {
    override fun close() {
        screen.onClose()
    }
}

abstract class AbstractCombineScreen(
    title: Component,
    private val parent: Screen?,
    protected val renderBackground: Boolean,
) : Screen(title), CoroutineScope, KoinComponent {
    private val client = Minecraft.getInstance()
    private var initialized = false
    private val dispatcher: GameDispatcher by inject()
    private val soundManager = SoundManagerImpl(client.soundManager)
    private val closeHandler = ScreenCloseHandler(this@AbstractCombineScreen)
    private val dismissDispatcher = OnDismissRequestDispatcher()

    private val owner = CombineOwner(dispatcher = dispatcher, textMeasurer = TextMeasurerImpl)
    override val coroutineContext: CoroutineContext
        get() = owner.coroutineContext

    fun setContent(content: @Composable () -> Unit) {
        owner.setContent {
            KoinContext {
                CompositionLocalProvider(
                    LocalSoundManager provides soundManager,
                    LocalScreen provides this,
                    LocalCloseHandler provides closeHandler,
                    LocalItemFactory provides get(),
                    LocalTextFactory provides get(),
                    LocalDataComponentTypeFactory provides get(),
                    LocalClipboard provides ClipboardHandlerImpl,
                    LocalScreenFactory provides get(),
                    LocalOnDismissRequestDispatcher provides dismissDispatcher,
                    LocalInputHandler provides InputManager,
                ) {
                    content()
                }
            }
        }
    }

    override fun init() {
        super.init()
        if (!initialized) {
            initialized = true
            owner.start()
        }
    }

    private fun mapMouseButton(button: Int) = when (button) {
        0 -> PointerButton.Left
        1 -> PointerButton.Middle
        2 -> PointerButton.Right
        else -> null
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val mouseButton = mapMouseButton(button) ?: return true
        owner.onPointerEvent(
            PointerEvent(
                id = 0,
                position = Offset(
                    x = mouseX.toFloat(),
                    y = mouseY.toFloat(),
                ),
                pointerType = PointerType.Mouse,
                button = mouseButton,
                type = PointerEventType.Press
            )
        )
        return true
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val mouseButton = mapMouseButton(button) ?: return true
        owner.onPointerEvent(
            PointerEvent(
                id = 0,
                position = Offset(
                    x = mouseX.toFloat(),
                    y = mouseY.toFloat(),
                ),
                pointerType = PointerType.Mouse,
                button = mouseButton,
                type = PointerEventType.Release
            )
        )
        return true
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        owner.onPointerEvent(
            PointerEvent(
                id = 0,
                position = Offset(
                    x = mouseX.toFloat(),
                    y = mouseY.toFloat(),
                ),
                pointerType = PointerType.Mouse,
                button = null,
                type = PointerEventType.Move
            )
        )
    }

    protected fun handleMouseScrolled(
        mouseX: Double,
        mouseY: Double,
        amountY: Double,
        amountX: Double = 0.0,
    ): Boolean {
        owner.onPointerEvent(
            PointerEvent(
                id = 0,
                position = Offset(
                    x = mouseX.toFloat(),
                    y = mouseY.toFloat(),
                ),
                pointerType = PointerType.Mouse,
                button = null,
                scrollDelta = Offset(
                    x = amountX.toFloat(),
                    y = amountY.toFloat(),
                ),
                type = PointerEventType.Scroll
            )
        )
        return true
    }

    override fun charTyped(char: Char, modifiers: Int): Boolean {
        owner.onTextInput(char.toString())
        return true
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (dismissDispatcher.hasEnabledCallbacks()) {
                dismissDispatcher.dispatchOnDismissed()
            } else {
                onClose()
            }
            return true
        }
        owner.onKeyEvent(
            KeyEvent(
                key = mapKeyCode(keyCode),
                modifier = mapModifier(modifiers),
                pressed = true
            )
        )
        return true
    }

    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        owner.onKeyEvent(
            KeyEvent(
                key = mapKeyCode(keyCode),
                modifier = mapModifier(modifiers),
                pressed = false
            )
        )
        return true
    }

    protected fun handleRender(drawContext: GuiGraphics) {
        val canvas = CanvasImpl(drawContext)
        val size = IntSize(width, height)
        RenderSystem.enableBlend()
        owner.render(size, canvas)
        RenderSystem.enableBlend()
    }

    override fun onClose() {
        owner.close()
        client.setScreen(parent)
    }
}
