package top.fifthlight.combine.platform

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GLAllocation
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.util.ResourceLocation
import org.joml.Matrix4f
import org.lwjgl.opengl.GL11
import top.fifthlight.combine.data.BackgroundTexture
import top.fifthlight.combine.data.Identifier
import top.fifthlight.combine.data.ItemStack
import top.fifthlight.combine.data.Texture
import top.fifthlight.combine.paint.Canvas
import top.fifthlight.combine.paint.ClipStack
import top.fifthlight.combine.paint.Color
import top.fifthlight.combine.paint.Colors
import top.fifthlight.data.*
import top.fifthlight.touchcontroller.assets.Textures
import top.fifthlight.touchcontroller.ext.color
import top.fifthlight.combine.data.Text as CombineText

class CanvasImpl : Canvas, Gui() {
    companion object {
        private val IDENTIFIER_ATLAS = ResourceLocation("touchcontroller", "textures/gui/atlas.png")
    }

    private val client = Minecraft.getMinecraft()
    private val fontRenderer = client.fontRenderer
    override val textLineHeight: Int = fontRenderer.FONT_HEIGHT
    private val scaledResolution by lazy { ScaledResolution(client) }
    private val itemRenderer = client.renderItem
    private val matrixBuffer = GLAllocation.createDirectFloatBuffer(16)
    private val matrixStack = arrayListOf<Matrix4f>(run {
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, matrixBuffer)
        Matrix4f().apply { set(matrixBuffer) }
    })

    private fun applyMatrix(matrix: Matrix4f) {
        matrix.get(matrixBuffer)
        GL11.glLoadMatrix(matrixBuffer)
    }

    override fun pushState() {
        val matrix = Matrix4f(matrixStack.last())
        matrixStack.add(matrix)
    }

    override fun popState() {
        matrixStack.removeLast<Matrix4f>()
        applyMatrix(matrixStack.last())
    }

    override fun translate(x: Int, y: Int) {
        matrixStack.last().apply {
            translate(x.toFloat(), y.toFloat(), 0f)
            applyMatrix(this)
        }
    }

    override fun translate(x: Float, y: Float) {
        matrixStack.last().apply {
            translate(x, y, 0f)
            applyMatrix(this)
        }
    }

    override fun rotate(degrees: Float) {
        matrixStack.last().apply {
            rotate(degrees, 0f, 0f, 1f)
            applyMatrix(this)
        }
    }

    override fun scale(x: Float, y: Float) {
        matrixStack.last().apply {
            scale(x, y, 1f)
            applyMatrix(this)
        }
    }

    override fun fillRect(offset: IntOffset, size: IntSize, color: Color) {
        drawRect(offset.x, offset.y, offset.x + size.width, offset.y + size.height, color.value)
        GlStateManager.enableBlend()
    }

    override fun fillGradientRect(
        offset: Offset,
        size: Size,
        leftTopColor: Color,
        leftBottomColor: Color,
        rightTopColor: Color,
        rightBottomColor: Color
    ) {
        val tessellator = Tessellator.getInstance()
        val bufferBuilder = tessellator.buffer
        GlStateManager.disableTexture2D()
        GlStateManager.disableAlpha()
        GlStateManager.enableBlend()
        GlStateManager.shadeModel(GL11.GL_SMOOTH)
        bufferBuilder.begin(7, DefaultVertexFormats.POSITION_COLOR)
        val dstRect = Rect(offset, size)
        bufferBuilder
            .pos(dstRect.left.toDouble(), dstRect.top.toDouble(), 0.0)
            .color(leftTopColor)
            .endVertex()
        bufferBuilder
            .pos(dstRect.left.toDouble(), dstRect.bottom.toDouble(), 0.0)
            .color(leftBottomColor)
            .endVertex()
        bufferBuilder
            .pos(dstRect.right.toDouble(), dstRect.bottom.toDouble(), 0.0)
            .color(rightBottomColor)
            .endVertex()
        bufferBuilder
            .pos(dstRect.right.toDouble(), dstRect.top.toDouble(), 0.0)
            .color(rightTopColor)
            .endVertex()
        tessellator.draw()
        GlStateManager.shadeModel(GL11.GL_FLAT)
        GlStateManager.enableAlpha()
        GlStateManager.enableTexture2D()
    }

    override fun drawRect(offset: IntOffset, size: IntSize, color: Color) {
        //  1 -> 2  |
        //  |    |  |
        //  4 -> 3 \|/

        val strokeSize = 1

        // 1 to 2
        fillRect(
            offset = offset,
            size = IntSize(
                width = size.width - strokeSize,
                height = strokeSize,
            ),
            color = color,
        )

        // 2 to 3
        fillRect(
            offset = IntOffset(
                x = offset.x + size.width - strokeSize,
                y = offset.y,
            ),
            size = IntSize(
                width = strokeSize,
                height = size.height - strokeSize,
            ),
            color = color,
        )

        // 4 to 3
        fillRect(
            offset = IntOffset(
                x = offset.x + strokeSize,
                y = offset.y + size.height - strokeSize,
            ),
            size = IntSize(
                width = size.width - strokeSize,
                height = strokeSize,
            ),
            color = color,
        )

        // 4 to 1
        fillRect(
            offset = IntOffset(
                x = offset.x,
                y = offset.y + strokeSize,
            ),
            size = IntSize(
                width = strokeSize,
                height = size.height - strokeSize,
            ),
            color = color,
        )
    }

    override fun drawText(offset: IntOffset, text: String, color: Color) {
        val x = offset.x
        var y = offset.y
        for (line in text.lineSequence()) {
            fontRenderer.drawString(line, x, y, color.value)
            y += fontRenderer.FONT_HEIGHT
        }
    }

    override fun drawText(offset: IntOffset, width: Int, text: String, color: Color) {
        if (width >= 16) {
            fontRenderer.drawSplitString(text, offset.x, offset.y, width, color.value)
        }
    }

    override fun drawText(offset: IntOffset, text: CombineText, color: Color) =
        drawText(offset, text.toMinecraft().formattedText, color)

    override fun drawText(offset: IntOffset, width: Int, text: CombineText, color: Color) {
        if (width > 0) {
            drawText(offset, width, text.toMinecraft().formattedText, color)
        }
    }

    private fun drawTexture(
        identifier: ResourceLocation,
        dstRect: Rect,
        uvRect: Rect,
        tint: Color = Colors.WHITE,
    ) {
        this.client.textureManager.bindTexture(identifier)
        GlStateManager.color(tint.r / 256f, tint.g / 256f, tint.b / 256f, tint.a / 256f)
        val tessellator = Tessellator.getInstance()
        val bufferBuilder = tessellator.buffer
        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX)
        bufferBuilder
            .pos(dstRect.left.toDouble(), dstRect.top.toDouble(), 0.0)
            .tex(uvRect.left.toDouble(), uvRect.top.toDouble())
            .endVertex()
        bufferBuilder
            .pos(dstRect.left.toDouble(), dstRect.bottom.toDouble(), 0.0)
            .tex(uvRect.left.toDouble(), uvRect.bottom.toDouble())
            .endVertex()
        bufferBuilder
            .pos(dstRect.right.toDouble(), dstRect.bottom.toDouble(), 0.0)
            .tex(uvRect.right.toDouble(), uvRect.bottom.toDouble())
            .endVertex()
        bufferBuilder
            .pos(dstRect.right.toDouble(), dstRect.top.toDouble(), 0.0)
            .tex(uvRect.right.toDouble(), uvRect.top.toDouble())
            .endVertex()
        tessellator.draw()
    }

    override fun drawTexture(
        identifier: Identifier,
        dstRect: Rect,
        uvRect: Rect,
        tint: Color,
    ) = drawTexture(
        identifier = identifier.toMinecraft(),
        dstRect = dstRect,
        uvRect = uvRect,
        tint = tint,
    )

    override fun drawTexture(
        texture: Texture,
        dstRect: Rect,
        srcRect: IntRect,
        tint: Color,
    ) = drawTexture(
        identifier = IDENTIFIER_ATLAS,
        dstRect = dstRect,
        uvRect = Rect(
            offset = (texture.atlasOffset + srcRect.offset).toOffset() / Textures.atlasSize.toSize(),
            size = srcRect.size.toSize() / Textures.atlasSize.toSize(),
        ),
        tint = tint,
    )

    override fun drawBackgroundTexture(
        texture: BackgroundTexture,
        scale: Float,
        dstRect: Rect,
        tint: Color,
    ) = drawTexture(
        identifier = texture.identifier.toMinecraft(),
        dstRect = dstRect,
        uvRect = Rect(
            offset = Offset.ZERO,
            size = dstRect.size / texture.size.toSize() / scale,
        ),
        tint = tint,
    )

    override fun drawItemStack(offset: IntOffset, size: IntSize, stack: ItemStack) {
        val minecraftStack = ((stack as? ItemStackImpl) ?: return).inner
        scale(size.width.toFloat() / 16f, size.height.toFloat() / 16f)
        GlStateManager.pushMatrix()
        GlStateManager.enableDepth()
        RenderHelper.enableGUIStandardItemLighting()
        itemRenderer.renderItemAndEffectIntoGUI(minecraftStack, offset.x, offset.y)
        RenderHelper.disableStandardItemLighting()
        GlStateManager.disableDepth()
        GlStateManager.popMatrix()
        GlStateManager.enableBlend()
        GlStateManager.enableAlpha()
    }

    private val clipStack = ClipStack()

    override fun pushClip(absoluteArea: IntRect, relativeArea: IntRect) {
        val scaleFactor = scaledResolution.scaleFactor
        val rect = IntRect(
            offset = absoluteArea.offset * scaleFactor,
            size = absoluteArea.size * scaleFactor,
        )
        val clipRect = clipStack.pushClip(rect)
        GL11.glEnable(GL11.GL_SCISSOR_TEST)
        GL11.glScissor(clipRect.left, client.displayHeight - clipRect.bottom, clipRect.size.width, clipRect.size.height)
    }

    override fun popClip() {
        val clipRect = clipStack.popClip()
        if (clipRect != null) {
            GL11.glScissor(clipRect.left, client.displayHeight - clipRect.bottom, clipRect.size.width, clipRect.size.height)
        } else {
            GL11.glDisable(GL11.GL_SCISSOR_TEST)
        }
    }
}