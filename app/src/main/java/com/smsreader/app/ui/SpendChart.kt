package com.smsreader.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.smsreader.app.analysis.Bucket
import com.smsreader.app.analysis.Step
import com.smsreader.app.analysis.Timeline
import kotlin.math.min

/**
 * Spending per day, week or month as columns. The caption names the biggest column; tap a
 * column to see its own amount, tap it again to go back.
 */
@Composable
fun SpendChart(timeline: Timeline, color: Color, mutedColor: Color, modifier: Modifier = Modifier) {
    val buckets = timeline.buckets
    if (buckets.none { it.paise > 0 }) return
    var selected by remember(timeline) { mutableStateOf<Int?>(null) }
    val peak = buckets.indices.maxBy { buckets[it].paise }
    val shown = selected ?: peak
    val max = buckets[peak].paise

    val caption = if (selected == null) "Most spent ${inText(buckets[peak], timeline.step)}" else nameOf(buckets[shown], timeline.step)
    Column(modifier.semantics { contentDescription = "Spending ${byText(timeline.step)}. $caption: ${rupees(buckets[shown].paise)}" }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(caption, style = MaterialTheme.typography.bodyMedium, color = mutedColor, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            Amount(buckets[shown].paise, style = MaterialTheme.typography.titleSmall, color = color)
        }
        val barColor = color.copy(alpha = 0.55f)
        Canvas(
            Modifier
                .padding(top = 10.dp)
                .fillMaxWidth()
                .height(72.dp)
                .pointerInput(timeline) {
                    detectTapGestures { offset ->
                        val i = (offset.x / size.width * buckets.size).toInt().coerceIn(0, buckets.lastIndex)
                        selected = if (selected == i) null else i
                    }
                },
        ) {
            val n = buckets.size
            val gap = (if (n > 40) 1.dp else 2.dp).toPx()
            val barWidth = ((size.width - gap * (n - 1)) / n).coerceAtLeast(1f)
            val radius = min(4.dp.toPx(), barWidth / 2)
            buckets.forEachIndexed { i, bucket ->
                if (bucket.paise <= 0) return@forEachIndexed
                val h = (bucket.paise.toFloat() / max * size.height).coerceAtLeast(3.dp.toPx())
                val x = i * (barWidth + gap)
                // Rounded at the data end, square on the baseline; one shape so translucency stays even.
                val bar = RoundRect(
                    left = x,
                    top = size.height - h,
                    right = x + barWidth,
                    bottom = size.height,
                    topLeftCornerRadius = CornerRadius(radius),
                    topRightCornerRadius = CornerRadius(radius),
                )
                drawPath(Path().apply { addRoundRect(bar) }, if (i == shown) color else barColor)
            }
            drawRect(mutedColor.copy(alpha = 0.5f), Offset(0f, size.height), Size(size.width, 1.dp.toPx()))
        }
        Row(Modifier.padding(top = 6.dp)) {
            Text(axisLabel(buckets.first(), timeline.step), style = MaterialTheme.typography.labelSmall, color = mutedColor)
            Spacer(Modifier.weight(1f))
            Text(axisLabel(buckets.last(), timeline.step), style = MaterialTheme.typography.labelSmall, color = mutedColor)
        }
    }
}

private fun byText(step: Step) = when (step) {
    Step.DAY -> "by day"
    Step.WEEK -> "by week"
    Step.MONTH -> "by month"
}

private fun inText(b: Bucket, step: Step) = when (step) {
    Step.DAY -> "on ${formatShortDate(b.start)}"
    Step.WEEK -> "in the week of ${formatShortDate(b.start)}"
    Step.MONTH -> "in ${formatMonth(b.start)}"
}

private fun nameOf(b: Bucket, step: Step) = when (step) {
    Step.DAY -> formatShortDate(b.start)
    Step.WEEK -> "Week of ${formatShortDate(b.start)}"
    Step.MONTH -> formatMonth(b.start)
}

private fun axisLabel(b: Bucket, step: Step) = if (step == Step.MONTH) formatMonth(b.start) else formatShortDate(b.start)
