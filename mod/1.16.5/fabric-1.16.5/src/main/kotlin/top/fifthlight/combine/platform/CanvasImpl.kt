package top.fifthlight.combine.platform

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawableHelper
import net.minecraft.client.render.BufferRenderer
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.VertexFormats
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.Quaternion
import net.minecraft.util.math.Vec3f
import org.lwjgl.opengl.GL11
import top.fifthlight.combine.data.BackgroundTexture
import top.fifthlight.combine.data.ItemStack
import top.fifthlight.combine.data.Texture
import top.fifthlight.combine.paint.Canvas
import top.fifthlight.combine.paint.ClipStack
import top.fifthlight.combine.paint.Color
import top.fifthlight.combine.paint.Colors
import top.fifthlight.data.*
import top.fifthlight.touchcontroller.assets.Textures
import top.fifthlight.touchcontroller.mixin.Matrix4fAccessor
import top.fifthlight.combine.data.Identifier as CombineIdentifier
import top.fifthlight.combine.data.Text as CombineText

fun VertexConsumer.color(color: Color): VertexConsumer = color(color.r, color.g, color.b, color.a)

class CanvasImpl(
    val matrices: MatrixStack,
) : Canvas, DrawableHelper() {
    companion object {
        private val IDENTIFIER_ATLAS = Identifier("touchcontroller", "textures/gui/atlas.png")
    }

    private val client = MinecraftClient.getInstance()
    private val textRenderer = client.textRenderer
    override val textLineHeight: Int = textRenderer.fontHeight

    override fun pushState() {
        matrices.push()
    }

    override fun popState() {
        matrices.pop()
    }

    override fun translate(x: Int, y: Int) {
        matrices.translate(x.toDouble(), y.toDouble(), 0.0)
    }

    override fun translate(x: Float, y: Float) {
        matrices.translate(x.toDouble(), y.toDouble(), 0.0)
    }

    override fun rotate(degrees: Float) {
        matrices.multiply(Quaternion(Vec3f.POSITIVE_Z, degrees, true))
    }

    override fun scale(x: Float, y: Float) {
        matrices.scale(x, y, 1f)
    }

    override fun fillRect(offset: IntOffset, size: IntSize, color: Color) {
        fill(matrices, offset.x, offset.y, offset.x + size.width, offset.y + size.height, color.value)
        RenderSystem.enableBlend()
    }

    @Suppress("DEPRECATION")
    override fun fillGradientRect(
        offset: Offset,
        size: Size,
        leftTopColor: Color,
        leftBottomColor: Color,
        rightTopColor: Color,
        rightBottomColor: Color
    ) {
        RenderSystem.disableAlphaTest()
        RenderSystem.disableTexture()
        RenderSystem.shadeModel(GL11.GL_SMOOTH)
        val matrix = matrices.peek().model
        val bufferBuilder = Tessellator.getInstance().buffer
        bufferBuilder.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR)
        val dstRect = Rect(offset, size)
        bufferBuilder
            .vertex(matrix, dstRect.left, dstRect.top, 0f)
            .color(leftTopColor)
            .next()
        bufferBuilder
            .vertex(matrix, dstRect.left, dstRect.bottom, 0f)
            .color(leftBottomColor)
            .next()
        bufferBuilder
            .vertex(matrix, dstRect.right, dstRect.bottom, 0f)
            .color(rightBottomColor)
            .next()
        bufferBuilder
            .vertex(matrix, dstRect.right, dstRect.top, 0f)
            .color(rightTopColor)
            .next()
        bufferBuilder.end()
        BufferRenderer.draw(bufferBuilder)
        RenderSystem.shadeModel(GL11.GL_FLAT)
        RenderSystem.enableTexture()
        RenderSystem.enableAlphaTest()
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
        textRenderer.draw(matrices, text, offset.x.toFloat(), offset.y.toFloat(), color.value)
        RenderSystem.enableBlend()
    }

    override fun drawText(offset: IntOffset, width: Int, text: String, color: Color) {
        var y = offset.y.toFloat()
        for (line in textRenderer.wrapLines(Text.of(text), width)) {
            textRenderer.draw(matrices, line, offset.x.toFloat(), y, color.value)
            y += textRenderer.fontHeight
        }
        RenderSystem.enableBlend()
    }

    override fun drawText(offset: IntOffset, text: CombineText, color: Color) {
        textRenderer.draw(matrices, text.toMinecraft(), offset.x.toFloat(), offset.y.toFloat(), color.value)
        RenderSystem.enableBlend()
    }

    override fun drawText(offset: IntOffset, width: Int, text: CombineText, color: Color) {
        var y = offset.y.toFloat()
        for (line in textRenderer.wrapLines(text.toMinecraft(), width)) {
            textRenderer.draw(matrices, line, offset.x.toFloat(), y, color.value)
            y += textRenderer.fontHeight
        }
        RenderSystem.enableBlend()
    }

    private fun drawTexture(
        identifier: Identifier,
        dstRect: Rect,
        uvRect: Rect,
        tint: Color = Colors.WHITE,
    ) {
        this.client.textureManager.bindTexture(identifier)
        val matrix = matrices.peek().model
        val bufferBuilder = Tessellator.getInstance().buffer
        bufferBuilder.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR_TEXTURE)
        bufferBuilder
            .vertex(matrix, dstRect.left, dstRect.top, 0f)
            .color(tint)
            .texture(uvRect.left, uvRect.top)
            .next()
        bufferBuilder
            .vertex(matrix, dstRect.left, dstRect.bottom, 0f)
            .color(tint)
            .texture(uvRect.left, uvRect.bottom)
            .next()
        bufferBuilder
            .vertex(matrix, dstRect.right, dstRect.bottom, 0f)
            .color(tint)
            .texture(uvRect.right, uvRect.bottom)
            .next()
        bufferBuilder
            .vertex(matrix, dstRect.right, dstRect.top, 0f)
            .color(tint)
            .texture(uvRect.right, uvRect.top)
            .next()
        bufferBuilder.end()
        BufferRenderer.draw(bufferBuilder)
    }

    override fun drawTexture(
        identifier: CombineIdentifier,
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
        val matrix = matrices.peek().model
        // TODO apply matrix when rendering
        @Suppress("CAST_NEVER_SUCCEEDS")
        val accessor = matrix as Matrix4fAccessor
        client.itemRenderer.renderGuiItemIcon(
            stack.toVanilla(),
            accessor.a03.toInt() + offset.left,
            accessor.a13.toInt() + offset.top
        )
    }

    private val clipStack = ClipStack()

    override fun pushClip(absoluteArea: IntRect, relativeArea: IntRect) {
        val scaleFactor = client.window.scaleFactor.toInt()
        val rect = IntRect(
            offset = absoluteArea.offset * scaleFactor,
            size = absoluteArea.size * scaleFactor,
        )
        val clipRect = clipStack.pushClip(rect)
        RenderSystem.enableScissor(clipRect.left, client.window.framebufferHeight - clipRect.bottom, clipRect.size.width, clipRect.size.height)
    }

    override fun popClip() {
        val clipRect = clipStack.popClip()
        if (clipRect != null) {
            RenderSystem.enableScissor(clipRect.left, client.window.framebufferHeight - clipRect.bottom, clipRect.size.width, clipRect.size.height)
        } else {
            RenderSystem.disableScissor()
        }
    }
}