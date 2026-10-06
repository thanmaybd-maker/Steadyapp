package com.thanu.steady.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.thanu.steady.R
import com.thanu.steady.platform.AmbientSoundType
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable fun AmbientSoundscape(model: ExpandedViewModel, state: ExpandedUiState, onSafety: () -> Unit) {
    val engine = model.audioSoundscapeEngine
    val playing by engine.isPlaying.collectAsState()
    val error by engine.error.collectAsState()
    val settings by model.ambient.collectAsState()
    val saved by model.ambientSaved.collectAsState()
    val scope = rememberCoroutineScope()
    var portal by remember { mutableStateOf(false) }
    val current = state.period?.active?.firstOrNull()
    DisposableEffect(engine) { onDispose { engine.release() } }
    LaunchedEffect(current?.id, current?.state, settings.autoPlay) {
        if (settings.autoPlay) {
            if (current?.state == "RUNNING") engine.start(scope) else engine.stop()
        }
    }
    @Composable fun controls() {
        error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
        PrimaryAction(if (playing) R.string.audio_stop else R.string.audio_play) {
            if (playing) engine.stop() else engine.start(scope)
        }
        ChoiceList(settings.sound, AmbientSoundType.entries.map { it.name to it.title }) { value ->
            model.ambient { it.copy(sound = value) }
        }
        val volumeLabel = stringResource(R.string.audio_volume_label)
        Text(stringResource(R.string.audio_volume, (settings.volume * 100).toInt()), color = MaterialTheme.colorScheme.onSurface)
        Slider(settings.volume, { value -> model.ambient { it.copy(volume = value) } },
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).semantics { contentDescription = volumeLabel })
        if (settings.sound == AmbientSoundType.ALPHA_BINAURAL.name) {
            val modulationLabel = stringResource(R.string.audio_modulation_label)
            RotaryModulation(settings.modulation.toFloat(), modulationLabel) { value -> model.ambient { it.copy(modulation = value.toDouble()) } }
            Text(stringResource(R.string.audio_modulation, settings.modulation), color = MaterialTheme.colorScheme.onSurface)
            Slider(settings.modulation.toFloat(), { value -> model.ambient { it.copy(modulation = value.toDouble()) } },
                valueRange = 1f..40f, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).semantics { contentDescription = modulationLabel })
            Text(stringResource(R.string.audio_tone_description), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ToggleRow(R.string.audio_auto_play, settings.autoPlay) { value -> model.ambient { it.copy(autoPlay = value) } }
        Text(stringResource(if (saved) R.string.saved else R.string.saving))
    }
    val scene = if (settings.scene != "AUTO") settings.scene else when (settings.sound) {
        "RAIN" -> "RAIN"; "CAMPFIRE" -> "CAMPFIRE"; else -> "FOREST"
    }
    val sceneResource = when (scene) { "RAIN" -> R.drawable.img_rain_portal; "CAMPFIRE" -> R.drawable.img_campfire_portal; else -> R.drawable.img_nature_portal }
    val sceneLabel = when (scene) { "RAIN" -> R.string.audio_scene_rain; "CAMPFIRE" -> R.string.audio_scene_campfire; else -> R.string.audio_scene_forest }
    SectionCard(R.string.ambient_audio_title) {
        Text(stringResource(R.string.ambient_audio_description), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Image(painterResource(sceneResource), stringResource(sceneLabel), Modifier.fillMaxWidth().height(180.dp), contentScale = ContentScale.Crop)
        controls()
        SecondaryAction(R.string.audio_portal) { portal = true }
    }
    if (portal) Dialog(onDismissRequest = { portal = false }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        DialogSurface { ExpandedPage {
            SecondaryAction(R.string.safety_action) { engine.stop(); portal = false; onSafety() }
            SecondaryAction(R.string.close_action) { portal = false }
            SectionCard(R.string.audio_scene_title) {
                Image(painterResource(sceneResource), stringResource(sceneLabel), Modifier.fillMaxWidth().height(240.dp), contentScale = ContentScale.Crop)
                ChoiceList(settings.scene, listOf("AUTO" to R.string.audio_scene_auto, "FOREST" to R.string.audio_scene_forest,
                    "RAIN" to R.string.audio_scene_rain, "CAMPFIRE" to R.string.audio_scene_campfire)) { value -> model.ambient { it.copy(scene = value) } }
                controls()
            }
        } }
    }
}

/** The dial and the labelled slider adjust the same actual modulation frequency. */
@Composable private fun RotaryModulation(value: Float, label: String, onChange: (Float) -> Unit) {
    val latest by rememberUpdatedState(onChange)
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.outline
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(176.dp).semantics {
            contentDescription = label
            progressBarRangeInfo = ProgressBarRangeInfo(value, 1f..40f)
            setProgress { latest(it.coerceIn(1f, 40f)); true }
        }.pointerInput(Unit) {
            detectDragGestures { change, _ ->
                val angle = atan2((change.position.y - size.height / 2).toDouble(), (change.position.x - size.width / 2).toDouble())
                val fraction = ((angle + PI / 2 + 2 * PI) % (2 * PI)) / (2 * PI)
                latest((1 + fraction * 39).toFloat()); change.consume()
            }
        }) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2 - 12.dp.toPx()
            drawCircle(track, radius, center, style = Stroke(3.dp.toPx()))
            val angle = (value - 1) / 39 * 2 * PI - PI / 2
            val point = Offset(center.x + cos(angle).toFloat() * radius, center.y + sin(angle).toFloat() * radius)
            drawLine(primary, center, point, 8.dp.toPx(), StrokeCap.Round)
            drawCircle(primary, 8.dp.toPx(), point)
        }
    }
}
