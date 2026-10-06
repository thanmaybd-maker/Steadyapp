package com.thanu.steady.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable fun CircadianWaveChart(modifier: Modifier = Modifier) {
    var tooltipPosition by remember { mutableStateOf<Offset?>(null) }
    Canvas(modifier = modifier.fillMaxWidth().height(140.dp).padding(16.dp).pointerInput(Unit) {
        detectTapGestures(onPress = { offset ->
            tooltipPosition = offset
            tryAwaitRelease()
            tooltipPosition = null
        })
    }) {
        val path = Path()
        val width = size.width
        val height = size.height
        path.moveTo(0f, height * 0.8f)
        path.cubicTo(width * 0.2f, height * 0.9f, width * 0.4f, height * 0.2f, width * 0.6f, height * 0.3f)
        path.cubicTo(width * 0.8f, height * 0.4f, width * 0.9f, height * 0.7f, width, height * 0.6f)

        drawPath(path, color = Color(0xFF4648D4), style = Stroke(width = 8f, cap = StrokeCap.Round))

        // Draw current time indicator
        drawCircle(color = Color(0xFF006C49), radius = 12f, center = Offset(width * 0.5f, height * 0.25f))

        tooltipPosition?.let {
            drawCircle(color = Color.White, radius = 24f, center = it)
            drawCircle(color = Color(0xFF4648D4), radius = 24f, center = it, style = Stroke(width = 4f))
        }
    }
}

@Composable fun HydrationCadenceRing(targetLiters: Float, currentLiters: Float, onLogWater: () -> Unit = {}) {
    val progress = (currentLiters / targetLiters).coerceIn(0f, 1f)
    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(160.dp).pointerInput(Unit) {
            detectTapGestures(onTap = { onLogWater() })
        }) {
            drawArc(color = Color(0xFFE0E0E0), startAngle = 135f, sweepAngle = 270f, useCenter = false, style = Stroke(width = 24f, cap = StrokeCap.Round))
            drawArc(color = Color(0xFF006C49), startAngle = 135f, sweepAngle = 270f * progress, useCenter = false, style = Stroke(width = 24f, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${currentLiters}L", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Text("of ${targetLiters}L Goal", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable fun MotionStudioCard(cadence: Int, steps: Int) {
    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp)).padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Movement Activity", style = MaterialTheme.typography.labelSmall, color = Color(0xFF006C49))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(cadence.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("CADENCE (SPM)", style = MaterialTheme.typography.labelSmall)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(steps.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("TODAY'S STEPS", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable fun CognitiveNutritionCard() {
    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp)).padding(16.dp)) {
        Text("Cognitive Nutrition", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Meals Logged: 3", style = MaterialTheme.typography.bodyMedium)
            Text("Healthy Mix", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF006C49))
        }
        Spacer(Modifier.height(8.dp))
        // Macro bar mock
        Row(modifier = Modifier.fillMaxWidth().height(12.dp).background(Color(0xFFE0E0E0), shape = RoundedCornerShape(50))) {
            Box(modifier = Modifier.weight(0.4f).fillMaxHeight().background(Color(0xFF4648D4)))
            Box(modifier = Modifier.weight(0.3f).fillMaxHeight().background(Color(0xFF006C49)))
            Box(modifier = Modifier.weight(0.3f).fillMaxHeight().background(Color(0xFFFFB300)))
        }
    }
}
