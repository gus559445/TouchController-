package top.fifthlight.combine.data

import androidx.compose.runtime.Immutable
import top.fifthlight.combine.paint.Canvas
import top.fifthlight.combine.paint.Color
import top.fifthlight.combine.paint.Drawable
import top.fifthlight.combine.paint.drawNinePatchTexture
import top.fifthlight.data.IntOffset
import top.fifthlight.data.IntPadding
import top.fifthlight.data.IntRect
import top.fifthlight.data.IntSize

@Immutable
interface Texture : Drawable {
    override val size: IntSize
    val atlasOffset: IntOffset
}

@Immutable
interface NinePatchTexture : Texture {
    val scaleArea: IntRect
    override val padding: IntPadding
}

@Immutable
data class BackgroundTexture(
    val identifier: Identifier,
    override val size: IntSize,
) : Drawable {
    override val padding: IntPadding
        get() = IntPadding.ZERO

    override fun Canvas.draw(rect: IntRect, tint: Color) {
        drawBackgroundTexture(this@BackgroundTexture, dstRect = rect.toRect(), tint = tint)
    }
}

private data class TextureImpl(
    override val size: IntSize,
    override val atlasOffset: IntOffset,
) : Texture {
    override val padding
        get() = IntPadding.ZERO

    override fun Canvas.draw(rect: IntRect, tint: Color) {
        drawTexture(this@TextureImpl, dstRect = rect.toRect(), tint = tint)
    }
}

private data class NinePatchTextureImpl(
    override val size: IntSize,
    override val atlasOffset: IntOffset,
    override val scaleArea: IntRect,
    override val padding: IntPadding,
) : NinePatchTexture {
    override fun Canvas.draw(rect: IntRect, tint: Color) {
        drawNinePatchTexture(this@NinePatchTextureImpl, dstRect = rect, tint = tint)
    }
}

fun Texture(
    size: IntSize,
    atlasOffset: IntOffset
): Texture = TextureImpl(
    size = size,
    atlasOffset = atlasOffset,
)

fun NinePatchTexture(
    size: IntSize,
    atlasOffset: IntOffset,
    scaleArea: IntRect,
    padding: IntPadding,
): NinePatchTexture = NinePatchTextureImpl(
    size = size,
    atlasOffset = atlasOffset,
    scaleArea = scaleArea,
    padding = padding,
)
