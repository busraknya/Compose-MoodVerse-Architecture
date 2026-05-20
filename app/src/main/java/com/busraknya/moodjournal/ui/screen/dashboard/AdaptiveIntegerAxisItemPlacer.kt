package com.busraknya.moodjournal.ui.screen.dashboard

import com.patrykandpatrick.vico.core.axis.AxisItemPlacer
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.vertical.VerticalAxis
import com.patrykandpatrick.vico.core.chart.draw.ChartDrawContext
import com.patrykandpatrick.vico.core.context.MeasureContext
import kotlin.math.ceil
import kotlin.math.max

/**
 * An adaptive [AxisItemPlacer] for Vico charts that intelligently adjusts the Y-axis labels
 * based on the maximum value in the chart data.
 *
 * This custom implementation solves a common problem in charting libraries where axis labels
 * become cluttered or unreadable when dealing with a wide range of dynamic data.
 *
 * ### Logic:
 * 1.  It determines an appropriate step size (1, 2, 5, etc.) based on the max Y-value.
 * 2.  It generates a list of evenly-spaced labels based on this step.
 * 3.  It always includes the true maximum value as the top label to provide a clear ceiling.
 */
class AdaptiveIntegerAxisItemPlacer(private val maxY: Float) : AxisItemPlacer.Vertical {

    // The final, pre-calculated list of Y-values for the axis labels and grid lines.
    // This is calculated only once in the init block for maximum performance.
    private val labels: List<Float>

    init {
        // Ensure the max value is at least 0 to prevent negative ranges.
        val nonNegativeMaxY = max(0f, maxY)
        // Determine the top of the axis by rounding up the max data value.
        val maxLabelValue = ceil(nonNegativeMaxY).toInt()
        val labelsList = mutableListOf<Float>()

        // 1. Select a step size based on the scale of the data.
        // This ensures the axis remains readable without being too cluttered.
        val step = when {
            maxLabelValue <= 12 -> 1  // For small numbers, show every integer.
            maxLabelValue <= 24 -> 2  // As numbers get larger, increase the step size.
            maxLabelValue <= 50 -> 5
            else -> 10
        }

        // 2. Generate the base list of labels using the chosen step.
        for (i in 0..maxLabelValue step step) {
            labelsList.add(i.toFloat())
        }

        // 3. Ensure the true maximum value is always visible on the axis.
        // This provides a clear ceiling for the chart, even if it's not a multiple of the step.
        if (maxLabelValue > 0 && (labelsList.isEmpty() || labelsList.last() != maxLabelValue.toFloat())) {
            labelsList.add(maxLabelValue.toFloat())
        }

        // If the list is still empty (e.g., maxY was 0), add 0 to have a baseline.
        if (labelsList.isEmpty()) {
            labelsList.add(0f)
        }

        // Use distinct() to prevent duplicates and then sort for a guaranteed ascending order.
        this.labels = labelsList.distinct().sorted()
    }

    // The following methods simply provide the pre-calculated list of labels to Vico
    // whenever it needs to draw or measure the axis, giving us full and predictable control.

    override fun getLabelValues(
        context: ChartDrawContext,
        axisHeight: Float,
        maxLabelHeight: Float,
        position: AxisPosition.Vertical
    ): List<Float> = labels

    override fun getHeightMeasurementLabelValues(
        context: MeasureContext,
        position: AxisPosition.Vertical
    ): List<Float> = labels

    override fun getWidthMeasurementLabelValues(
        context: MeasureContext,
        axisHeight: Float,
        maxLabelHeight: Float,
        position: AxisPosition.Vertical
    ): List<Float> = labels

    // By returning the same list for lines, we ensure grid lines and labels are perfectly aligned.
    override fun getLineValues(
        context: ChartDrawContext,
        axisHeight: Float,
        maxLabelHeight: Float,
        position: AxisPosition.Vertical
    ): List<Float> = labels

    // These methods are for calculating extra spacing (insets) around the axis.
    // We don't require any, so we return 0f.
    override fun getBottomVerticalAxisInset(
        verticalLabelPosition: VerticalAxis.VerticalLabelPosition,
        maxLabelHeight: Float,
        maxLineThickness: Float
    ): Float = 0f

    override fun getTopVerticalAxisInset(
        verticalLabelPosition: VerticalAxis.VerticalLabelPosition,
        maxLabelHeight: Float,
        maxLineThickness: Float
    ): Float = 0f
}