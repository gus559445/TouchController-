package top.fifthlight.touchcontroller

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.LayeredDraw
import top.fifthlight.combine.platform_1_21_1_21_1.CanvasImpl
import top.fifthlight.touchcontroller.common.event.RenderEvents

object HudLayer: LayeredDraw.Layer {
    override fun render(guiGraphics: GuiGraphics, partialTick: DeltaTracker) {
        var minecraft = Minecraft.getInstance()
        if (minecraft.options.hideGui) {
            return
        }
        var canvas = CanvasImpl(guiGraphics)
        RenderSystem.enableBlend()
        RenderEvents.onHudRender(canvas)
        RenderSystem.disableBlend()
    }
}