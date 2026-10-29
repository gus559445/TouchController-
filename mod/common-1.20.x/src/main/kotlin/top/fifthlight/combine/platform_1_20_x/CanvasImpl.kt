package top.fifthlight.combine.platform_1_20_x

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferUploader
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import org.joml.Quaternionf
import top.fifthlight.combine.data.BackgroundTexture
import top.fifthlight.combine.data.Identifier
import top.fifthlight.combine.data.ItemStack
import top.fifthlight.combine.data.Texture
import top.fifthlight.combine.paint.Canvas
import top.fifthlight.combine.paint.Color
import top.fifthlight.combine.paint.Colors
import top.fifthlight.data.*
import top.fifthlight.touchcontroller.assets.Textures
import java.util.function.Supplier
import top.fifthlight.combine.data.Text as CombineText

inline fun withShader(program: Supplier<ShaderInstance>, crossinline block: () -> Unit) {
    val originalShader = RenderSystem.getShader()
    RenderSystem.setShader(program)
    block()
    originalShader?.let {
        RenderSystem.setShader { originalShader }
    }
}

class CanvasImpl(
    val drawContext: GuiGraphics,
) : Canvas {
    companion object {
        private val IDENTIFIER_ATLAS = ResourceLocation("touchcontroller", "textures/gui/atlas.png")
    }

    private val client = Minecraft.getInstance()
    private val textRenderer = client.font
    override val textLineHeight: Int = textRenderer.lineHeight

    override fun pushState() {
        drawContext.pose().pushPose()
    }

    override fun popState() {
        drawContext.pose().popPose()
    }

    override fun translate(x: Int, y: Int) {
        drawContext.pose().translate(x.toDouble(), y.toDouble(), 0.0)
    }

    override fun translate(x: Float, y: Float) {
        drawContext.pose().translate(x.toDouble(), y.toDouble(), 0.0)
    }

    override fun rotate(degrees: Float) {
        Quaternionf().apply {
            rotateZ(Math.toRadians(degrees.toDouble()).toFloat())
            drawContext.pose().mulPose(this)
        }
    }

    override fun scale(x: Float, y: Float) {
        drawContext.pose().scale(x, y, 1f)
    }

    override fun fillRect(offset: IntOffset, size: IntSize, color: Color) {
        drawContext.fill(offset.x, offset.y, offset.x + size.width, offset.y + size.height, color.value)
    }

    override fun fillGradientRect(
        offset: Offset,
        size: Size,
        leftTopColor: Color,
        leftBottomColor: Color,
        rightTopColor: Color,
        rightBottomColor: Color
    ) {
        val renderLayer = RenderType.gui()
        val matrix = drawContext.pose().last().pose()
        val vertexConsumer = drawContext.bufferSource().getBuffer(renderLayer)
        val dstRect = Rect(offset, size)
        vertexConsumer
            .vertex(matrix, dstRect.left, dstRect.top, 0f)
            .color(leftTopColor.value)
            .endVertex()
        vertexConsumer
            .vertex(matrix, dstRect.left, dstRect.bottom, 0f)
            .color(leftBottomColor.value)
            .endVertex()
        vertexConsumer
            .vertex(matrix, dstRect.right, dstRect.bottom, 0f)
            .color(rightBottomColor.value)
            .endVertex()
        vertexConsumer
            .vertex(matrix, dstRect.right, dstRect.top, 0f)
            .color(rightTopColor.value)
            .endVertex()
        drawContext.flush()
    }

    override fun drawRect(offset: IntOffset, size: IntSize, color: Color) {
        drawContext.renderOutline(offset.x, offset.y, size.width, size.height, color.value)
    }

    override fun drawText(offset: IntOffset, text: String, color: Color) {
        drawContext.drawString(textRenderer, text, offset.x, offset.y, color.value, false)
    }

    override fun drawText(offset: IntOffset, width: Int, text: String, color: Color) {
        drawContext.drawWordWrap(textRenderer, Component.literal(text), offset.x, offset.y, width, color.value)
    }

    override fun drawText(offset: IntOffset, text: CombineText, color: Color) {
        drawContext.drawString(textRenderer, text.toMinecraft(), offset.x, offset.y, color.value, false)
    }

    override fun drawText(offset: IntOffset, width: Int, text: CombineText, color: Color) {
        drawContext.drawWordWrap(textRenderer, text.toMinecraft(), offset.x, offset.y, width, color.value)
    }

    private fun drawTexture(
        identifier: ResourceLocation,
        dstRect: Rect,
        uvRect: Rect,
        tint: Color = Colors.WHITE,
    ) {
        drawContext.flush()
        RenderSystem.setShaderTexture(0, identifier)
        withShader({ GameRenderer.getPositionTexColorShader()!! }) {
            val matrix = drawContext.pose().last().pose()
            val bufferBuilder = Tesselator.getInstance().builder
            bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR)
            bufferBuilder
                .vertex(matrix, dstRect.left, dstRect.top, 0f)
                .uv(uvRect.left, uvRect.top)
                .color(tint.value)
                .endVertex()
            bufferBuilder
                .vertex(matrix, dstRect.left, dstRect.bottom, 0f)
                .uv(uvRect.left, uvRect.bottom)
                .color(tint.value)
                .endVertex()
            bufferBuilder
                .vertex(matrix, dstRect.right, dstRect.bottom, 0f)
                .uv(uvRect.right, uvRect.bottom)
                .color(tint.value)
                .endVertex()
            bufferBuilder
                .vertex(matrix, dstRect.right, dstRect.top, 0f)
                .uv(uvRect.right, uvRect.top)
                .color(tint.value)
                .endVertex()
            RenderSystem.enableBlend()
            BufferUploader.drawWithShader(bufferBuilder.end())
        }
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
        val minecraftStack = ((stack as? AbstractItemStackImpl) ?: return).inner
        drawContext.pose().scale(size.width.toFloat() / 16f, size.height.toFloat() / 16f, 1f)
        pushState()
        drawContext.renderItem(minecraftStack, offset.x, offset.y)
        popState()
    }

    override fun pushClip(absoluteArea: IntRect, relativeArea: IntRect) {
        drawContext.enableScissor(absoluteArea.left, absoluteArea.top, absoluteArea.right, absoluteArea.bottom)
    }

    override fun popClip() {
        drawContext.disableScissor()
    }
}