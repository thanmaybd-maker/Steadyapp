package com.thanu.steady.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.thanu.steady.R
import com.thanu.steady.domain.ChartRules
import kotlin.math.PI
import kotlin.math.sin

/** Adapts OG's concentric progress and hydration motifs to real destination records. */
@Composable fun ConcentricSummary(values: List<Pair<Double?, Double?>>, reducedMotion: Boolean) {
    val colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.tertiary)
    val track = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val fractions = values.take(3).mapIndexed { index, (value, target) ->
        key(index) { animateFloatAsState(ChartRules.progress(value, target) ?: 0f,
            tween(if (reducedMotion) 0 else 650), label = "summary-$index").value }
    }
    val description = stringResource(R.string.activity_rings_description)
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.size(216.dp).semantics { contentDescription = description }) {
            val center = Offset(size.width / 2, size.height / 2)
            fractions.forEachIndexed { index, fraction ->
                val radius = size.minDimension / 2 - (12 + 24 * index).dp.toPx()
                val bounds = Offset(center.x - radius, center.y - radius)
                val arcSize = Size(radius * 2, radius * 2)
                val stroke = Stroke(12.dp.toPx(), cap = StrokeCap.Round)
                drawArc(track, -90f, 360f, false, bounds, arcSize, style = stroke)
                if (fraction > 0) drawArc(colors[index], -90f, 360f * fraction, false, bounds, arcSize, style = stroke)
            }
        }
        Text(stringResource(R.string.daily_momentum), style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable fun AnimatedWaterFill(currentMl: Long, targetMl: Int?, unit: String, reducedMotion: Boolean) {
    val fraction by animateFloatAsState(ChartRules.progress(currentMl.toDouble(), targetMl?.toDouble()) ?: 0f,
        tween(if (reducedMotion) 0 else 650), label = "water-fill")
    val phase = if (reducedMotion) 0f else {
        val transition = rememberInfiniteTransition(label = "water-wave")
        val angle by transition.animateFloat(0f, (2 * PI).toFloat(),
            infiniteRepeatable(tween(3200, easing = LinearEasing)), label = "water-phase")
        angle
    }
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val track = MaterialTheme.colorScheme.surfaceVariant
    val outline = MaterialTheme.colorScheme.outline
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.size(176.dp)) {
            val circle = Path().apply { addOval(androidx.compose.ui.geometry.Rect(4.dp.toPx(), 4.dp.toPx(), size.width - 4.dp.toPx(), size.height - 4.dp.toPx())) }
            clipPath(circle) {
                drawRect(track)
                if (fraction > 0) {
                    fun wave(offset: Float, amplitude: Float): Path = Path().apply {
                        val level = size.height * (1 - fraction)
                        moveTo(0f, size.height)
                        for (step in 0..100) {
                            val x = size.width * step / 100
                            val y = level + sin(x / size.width * 2 * PI + phase + offset).toFloat() * amplitude
                            lineTo(x, y)
                        }
                        lineTo(size.width, size.height); close()
                    }
                    val amplitude = if (fraction >= 1f) 0f else 5.dp.toPx()
                    drawPath(wave(1.7f, amplitude), secondary.copy(alpha = 0.35f))
                    drawPath(wave(0f, amplitude), primary.copy(alpha = 0.75f))
                }
            }
            drawPath(circle, outline, style = Stroke(2.dp.toPx()))
        }
        Text(waterAmount(currentMl, unit), style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface)
        Text(if (targetMl == null) stringResource(R.string.no_optional_target) else
            stringResource(R.string.water_target_summary, waterAmount(targetMl.toLong(), unit)),
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable fun HabitProgressPetal(title: String, quantity: Double, target: Double, unit: String, reducedMotion: Boolean) {
    val fraction by animateFloatAsState(ChartRules.progress(quantity, target) ?: 0f,
        tween(if (reducedMotion) 0 else 240), label = "habit-petal")
    val color = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant
    val description = stringResource(R.string.habit_progress_description, title, quantity.toString(), target.toString(), unit)
    Canvas(Modifier.fillMaxWidth().height(48.dp).semantics { contentDescription = description }) {
        repeat(8) { index ->
            val radius = minOf(size.height / 3, size.width / 24)
            val center = Offset(size.width * (index + 0.5f) / 8, size.height / 2)
            drawCircle(track, radius, center)
            val petal = (fraction * 8 - index).coerceIn(0f, 1f)
            if (petal > 0) drawArc(color, -90f, petal * 360f, true,
                Offset(center.x - radius, center.y - radius), Size(radius * 2, radius * 2))
        }
    }
}
