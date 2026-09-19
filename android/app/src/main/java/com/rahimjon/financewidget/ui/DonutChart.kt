package com.rahimjon.financewidget.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rahimjon.financewidget.data.DerivedHolding
import kotlin.math.max

data class Slice(val label: String, val value: Double, val color: Color)

private const val MAX_SLICES = 8

/** Biggest holdings by value get a colour each; everything past that folds into "Other". */
fun buildSlices(holdings: List<DerivedHolding>, palette: List<Color>): List<Slice> {
    val sorted = holdings.filter { (it.marketValueUsd ?: 0.0) > 0 }.sortedByDescending { it.marketValueUsd }
    // Same ticker held in several lots is one slice.
    val byTicker = sorted.groupBy { it.holding.ticker }
        .map { (ticker, lots) -> ticker to lots.sumOf { it.marketValueUsd ?: 0.0 } }
        .sortedByDescending { it.second }

    val slices = byTicker.take(MAX_SLICES).mapIndexed { i, (ticker, value) -> Slice(ticker, value, palette[i % palette.size]) }
    val otherValue = byTicker.drop(MAX_SLICES).sumOf { it.second }
    return if (otherValue > 0) slices + Slice("Other", otherValue, ChartOtherColor) else slices
}

@Composable
fun DonutChart(slices: List<Slice>, centerLabel: String, modifier: Modifier = Modifier) {
    val total = slices.sumOf { it.value }
    val description = slices.joinToString(", ") { "${it.label} ${"%.0f".format(it.value / total * 100)} percent" }

    Box(modifier.size(184.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(184.dp).semantics { contentDescription = "Allocation: $description" }) {
            val stroke = 28.dp.toPx()
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(stroke / 2, stroke / 2)
            // A small surface-coloured gap between slices, as thin as the chart allows.
            val gap = if (slices.size > 1) 2f else 0f
            var start = -90f
            for (slice in slices) {
                val sweep = (slice.value / total * 360).toFloat()
                drawArc(
                    color = slice.color,
                    startAngle = start + gap / 2,
                    sweepAngle = max(sweep - gap, 0.5f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke),
                )
                start += sweep
            }
        }
        Text(centerLabel, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
