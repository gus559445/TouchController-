package top.fifthlight.combine.modifier

import top.fifthlight.combine.layout.Measurable
import top.fifthlight.combine.layout.MeasureResult
import top.fifthlight.combine.layout.MeasureScope

interface LayoutModifierNode {
    fun measure(measurable: Measurable, constraints: Constraints) =
        with(MeasureScope) { this.measure(measurable, constraints) }

    fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult

    fun MeasureScope.minIntrinsicWidth(measurable: Measurable, height: Int): Int =
        throw UnsupportedOperationException("minIntrinsicWidth not implemented")

    fun MeasureScope.minIntrinsicHeight(measurable: Measurable, width: Int): Int =
        throw UnsupportedOperationException("minIntrinsicHeight not implemented")

    fun MeasureScope.maxIntrinsicWidth(measurable: Measurable, height: Int): Int =
        throw UnsupportedOperationException("maxIntrinsicWidth not implemented")

    fun MeasureScope.maxIntrinsicHeight(measurable: Measurable, width: Int): Int =
        throw UnsupportedOperationException("maxIntrinsicHeight not implemented")

    fun minIntrinsicWidth(measurable: Measurable, height: Int): Int =
        with(MeasureScope) { this.minIntrinsicWidth(measurable, height) }

    fun maxIntrinsicWidth(measurable: Measurable, height: Int): Int =
        with(MeasureScope) { this.maxIntrinsicWidth(measurable, height) }

    fun minIntrinsicHeight(measurable: Measurable, width: Int): Int =
        with(MeasureScope) { this.minIntrinsicHeight(measurable, width) }

    fun maxIntrinsicHeight(measurable: Measurable, width: Int): Int =
        with(MeasureScope) { this.maxIntrinsicHeight(measurable, width) }
}
