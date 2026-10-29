package top.fifthlight.combine.platform

import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.IVertexBuilder
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.AbstractGui
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.WorldVertexBufferUploader
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.vector.Quaternion
import net.minecraft.util.math.vector.Vector3f
import net.minecraft.util.text.ITextComponent
import org.lwjgl.opengl.GL11
import top.fifthlight.combine.data.BackgroundTexture
import top.fifthlight.combine.data.Identifier
import top.fifthlight.combine.data.ItemStack
import top.fifthlight.combine.data.Texture
import top.fifthlight.combine.paint.*
import top.fifthlight.data.*
import top.fifthlight.touchcontroller.assets.Textures
import top.fifthlight.touchcontroller.mixin.Matrix4fAccessor
import top.fifthlight.combine.data.Text as CombineText

fun IVertexBuilder.color(color: Color): IVertexBuilder = color(color.r, color.g, color.b, color.a)

class CanvasImpl(
    val matrices: MatrixStack,
) : Canvas, AbstractGui() {
    companion object {
        private val IDENTIFIER_ATLAS = ResourceLocation("touchcontroller", "textures/gui/atlas.png")
    }

    private val client = Minecraft.getInstance()
    private val textRenderer = client.font
    override val textLineHeight: Int = textRenderer.lineHeight

    override fun pushState() {
        matrices.pushPose()
    }

    override fun popState() {
        matrices.popPose()
    }

    override fun translate(x: Int, y: Int) {
        matrices.translate(x.toDouble(), y.toDouble(), 0.0)
    }

    override fun translate(x: Float, y: Float) {
        matrices.translate(x.toDouble(), y.toDouble(), 0.0)
    }

    override fun rotate(degrees: Float) {
        matrices.mulPose(Quaternion(Vector3f.ZP, degrees, true))
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
        val matrix = matrices.last().pose()
        val bufferBuilder = Tessellator.getInstance().builder
        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR)
        val dstRect = Rect(offset, size)
        bufferBuilder
            .vertex(matrix, dstRect.left, dstRect.top, 0f)
            .color(leftTopColor)
            .endVertex()
        bufferBuilder
            .vertex(matrix, dstRect.left, dstRect.bottom, 0f)
            .color(leftBottomColor)
            .endVertex()
        bufferBuilder
            .vertex(matrix, dstRect.right, dstRect.bottom, 0f)
            .color(rightBottomColor)
            .endVertex()
        bufferBuilder
            .vertex(matrix, dstRect.right, dstRect.top, 0f)
            .color(rightTopColor)
            .endVertex()
        bufferBuilder.end()
        WorldVertexBufferUploader.end(bufferBuilder)
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
        for (line in textRenderer.split(ITextComponent.nullToEmpty(text), width)) {
            textRenderer.draw(matrices, line, offset.x.toFloat(), y, color.value)
            y += textRenderer.lineHeight
        }
        RenderSystem.enableBlend()
    }

    override fun drawText(offset: IntOffset, text: CombineText, color: Color) {
        textRenderer.draw(matrices, text.toMinecraft(), offset.x.toFloat(), offset.y.toFloat(), color.value)
        RenderSystem.enableBlend()
    }

    override fun drawText(offset: IntOffset, width: Int, text: CombineText, color: Color) {
        var y = offset.y.toFloat()
        for (line in textRenderer.split(text.toMinecraft(), width)) {
            textRenderer.draw(matrices, line, offset.x.toFloat(), y, color.value)
            y += textRenderer.lineHeight
        }
        RenderSystem.enableBlend()
    }

    private fun drawTexture(
        identifier: ResourceLocation,
        dstRect: Rect,
        uvRect: Rect,
        tint: Color = Colors.WHITE,
    ) {
        this.client.textureManager.bind(identifier)
        val matrix = matrices.last().pose()
        val bufferBuilder = Tessellator.getInstance().builder
        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX)
        bufferBuilder
            .vertex(matrix, dstRect.left, dstRect.top, 0f)
            .color(tint)
            .uv(uvRect.left, uvRect.top)
            .endVertex()
        bufferBuilder
            .vertex(matrix, dstRect.left, dstRect.bottom, 0f)
            .color(tint)
            .uv(uvRect.left, uvRect.bottom)
            .endVertex()
        bufferBuilder
            .vertex(matrix, dstRect.right, dstRect.bottom, 0f)
            .color(tint)
            .uv(uvRect.right, uvRect.bottom)
            .endVertex()
        bufferBuilder
            .vertex(matrix, dstRect.right, dstRect.top, 0f)
            .color(tint)
            .uv(uvRect.right, uvRect.top)
            .endVertex()
        bufferBuilder.end()
        WorldVertexBufferUploader.end(bufferBuilder)
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
        val matrix = matrices.last().pose()
        // TODO apply matrix when rendering
        @Suppress("CAST_NEVER_SUCCEEDS")
        val accessor = matrix as Matrix4fAccessor
        client.itemRenderer.renderGuiItem(
            stack.toVanilla(),
            accessor.a03.toInt() + offset.left,
            accessor.a13.toInt() + offset.top
        )
    }

    private val clipStack = ClipStack()

    override fun pushClip(absoluteArea: IntRect, relativeArea: IntRect) {
        val scaleFactor = client.window.guiScale.toInt()
        val rect = IntRect(
            offset = absoluteArea.offset * scaleFactor,
            size = absoluteArea.size * scaleFactor,
        )
        val clipRect = clipStack.pushClip(rect)
        RenderSystem.enableScissor(clipRect.left, client.window.height - clipRect.bottom, clipRect.size.width, clipRect.size.height)
    }

    override fun popClip() {
        val clipRect = clipStack.popClip()
        if (clipRect != null) {
            RenderSystem.enableScissor(clipRect.left, client.window.height - clipRect.bottom, clipRect.size.width, clipRect.size.height)
        } else {
            RenderSystem.disableScissor()
        }
    }
}