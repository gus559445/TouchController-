package top.fifthlight.touchcontroller.gal

import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import org.lwjgl.opengl.GL11
import top.fifthlight.combine.paint.Canvas
import top.fifthlight.data.Offset
import top.fifthlight.touchcontroller.common.config.TouchRingConfig
import top.fifthlight.touchcontroller.common.gal.CrosshairRenderer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val CROSSHAIR_CIRCLE_PARTS = 24
private const val CROSSHAIR_CIRCLE_ANGLE = 2 * PI.toFloat() / CROSSHAIR_CIRCLE_PARTS

private fun point(angle: Float, radius: Float) = Offset(
    x = cos(angle) * radius,
    y = sin(angle) * radius
)

object CrosshairRendererImpl : CrosshairRenderer {
    override fun renderOuter(canvas: Canvas, config: TouchRingConfig) {
        val tessellator = Tessellator.getInstance()
        val bufferBuilder = tessellator.buffer
        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION)
        val innerRadius = config.radius.toFloat()
        val outerRadius = (config.radius + config.outerRadius).toFloat()
        var angle = -PI.toFloat() / 2f
        for (i in 0 until CROSSHAIR_CIRCLE_PARTS) {
            val endAngle = angle + CROSSHAIR_CIRCLE_ANGLE
            val point0 = point(angle, outerRadius)
            val point1 = point(endAngle, outerRadius)
            val point2 = point(angle, innerRadius)
            val point3 = point(endAngle, innerRadius)
            angle = endAngle

            bufferBuilder.pos(point0.x.toDouble(), point0.y.toDouble(), 0.0).endVertex()
            bufferBuilder.pos(point2.x.toDouble(), point2.y.toDouble(), 0.0).endVertex()
            bufferBuilder.pos(point3.x.toDouble(), point3.y.toDouble(), 0.0).endVertex()
            bufferBuilder.pos(point1.x.toDouble(), point1.y.toDouble(), 0.0).endVertex()
        }

        GlStateManager.disableTexture2D()
        GlStateManager.color(1f, 1f, 1f, 1f)
        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR,
            GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO,
        )
        tessellator.draw()
        GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO,
        )
        GlStateManager.enableTexture2D()
    }

    override fun renderInner(canvas: Canvas, config: TouchRingConfig, progress: Float) {
        val tessellator = Tessellator.getInstance()
        val bufferBuilder = tessellator.buffer
        bufferBuilder.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION)
        bufferBuilder.pos(0.0, 0.0, 0.0).endVertex()

        var angle = 0f
        for (i in 0..CROSSHAIR_CIRCLE_PARTS) {
            val point = point(angle, config.radius * progress)
            angle -= CROSSHAIR_CIRCLE_ANGLE

            bufferBuilder.pos(point.x.toDouble(), point.y.toDouble(), 0.0).endVertex()
        }

        GlStateManager.disableTexture2D()
        GlStateManager.color(1f, 1f, 1f, 1f)
        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR,
            GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO,
        )
        tessellator.draw()
        GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO,
        )
        GlStateManager.enableTexture2D()
    }
}