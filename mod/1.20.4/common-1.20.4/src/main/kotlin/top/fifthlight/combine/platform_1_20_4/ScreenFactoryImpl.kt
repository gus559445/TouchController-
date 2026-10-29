package top.fifthlight.combine.platform_1_20_4

import androidx.compose.runtime.Composable
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import top.fifthlight.combine.platform_1_20_x.AbstractCombineScreen
import top.fifthlight.combine.screen.ScreenFactory
import top.fifthlight.combine.data.Text as CombineText

private class CombineScreen(
    title: Component,
    parent: Screen?,
    renderBackground: Boolean,
) : AbstractCombineScreen(title, parent, renderBackground) {
    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        amountX: Double,
        amountY: Double,
    ): Boolean = handleMouseScrolled(mouseX, mouseY, amountY, amountX)

    override fun render(drawContext: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        if (renderBackground) {
            this.renderBackground(drawContext, mouseX, mouseY, delta)
        }
        handleRender(drawContext)
    }
}

object ScreenFactoryImpl : ScreenFactory {
    override fun openScreen(
        renderBackground: Boolean,
        title: CombineText,
        content: @Composable () -> Unit
    ) {
        val client = Minecraft.getInstance()
        val screen = getScreen(client.screen, renderBackground, title, content)
        client.setScreen(screen as Screen)
    }

    override fun getScreen(
        parent: Any?,
        renderBackground: Boolean,
        title: CombineText,
        content: @Composable () -> Unit
    ): Any {
        val screen = CombineScreen(
            title = title.toMinecraft(),
            parent = parent?.let { it as Screen },
            renderBackground = renderBackground
        )
        screen.setContent {
            content()
        }
        return screen
    }
}
