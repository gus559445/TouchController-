package top.fifthlight.touchcontroller.common.layout

import org.koin.core.component.get
import top.fifthlight.combine.paint.*
import top.fifthlight.data.Offset
import top.fifthlight.touchcontroller.common.gal.CrosshairRenderer
import top.fifthlight.touchcontroller.common.gal.TweakState
import top.fifthlight.touchcontroller.common.helper.shouldHideTouchRing

data class CrosshairStatus(
    val position: Offset,
    val breakPercent: Float,
) {
    val positionX
        get() = position.x

    val positionY
        get() = position.y
}

fun Context.Crosshair() {
    val status = result.crosshairStatus ?: return

    // 蓄力准星：拉弓 / 上弩弦 / 弩已装弹待发时，屏幕中心已经亮起原版准星，
    // 触控圈再杵在手指底下就是重复信息，这一帧直接不画（判断和原版准星共用同一个方法）。
    if (shouldHideTouchRing(config.tweaks, TweakState.provider)) {
        return
    }

    val crosshairRenderer: CrosshairRenderer = get()

    val config = config.touchRing
    drawQueue.enqueue { canvas ->
        canvas.withTranslate(status.position * windowScaledSize) {
            if (status.breakPercent > 0f) {
                val progress = status.breakPercent * (1f - config.initialProgress) + config.initialProgress
                crosshairRenderer.renderInner(canvas, config, progress)
            }
            crosshairRenderer.renderOuter(canvas, config)
        }
    }
}
