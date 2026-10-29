package top.fifthlight.combine.ui.style

import top.fifthlight.combine.data.Texture

data class TextureSet(
    val normal: Texture,
    val focus: Texture = normal,
    val hover: Texture = focus,
    val active: Texture = hover,
    val disabled: Texture = normal,
)
