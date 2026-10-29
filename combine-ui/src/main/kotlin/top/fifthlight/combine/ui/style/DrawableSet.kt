package top.fifthlight.combine.ui.style

import top.fifthlight.combine.paint.Drawable

data class DrawableSet(
    val normal: Drawable,
    val focus: Drawable = normal,
    val hover: Drawable = focus,
    val active: Drawable = hover,
    val disabled: Drawable = normal,
)
