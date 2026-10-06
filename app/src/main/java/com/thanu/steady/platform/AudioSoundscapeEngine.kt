package com.thanu.steady.platform

import android.content.Context
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.media.*
import android.os.Handler
import android.os.Looper
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.thanu.steady.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class AmbientSoundType(@StringRes val title: Int) {
    FOREST(R.string.sound_forest), RAIN(R.string.sound_rain),
    BROWN_NOISE(R.string.sound_brown), WHITE_NOISE(R.string.sound_white),
    ALPHA_BINAURAL(R.string.sound_tone), CAMPFIRE(R.string.sound_campfire)
}

/** Foreground-only PCM playback. One writer owns every AudioTrack through release. */
class AudioSoundscapeEngine(context: Context) {
    private val appContext = context.applicationContext
    private var noisyRegistered = false
    private val noisy = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) stop()
        }
    }
    private val manager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(attributes).setWillPauseWhenDucked(true)
        .setOnAudioFocusChangeListener({ change -> if (change <= 0) stop() }, Handler(Looper.getMainLooper())).build()
    private val writer = Mutex()
    private val control = Any()
    private var playbackJob: Job? = null
    @Volatile private var generation = 0
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying
    private val _currentSoundscape = MutableStateFlow<String?>(AmbientSoundType.FOREST.name)
    val currentSoundscape: StateFlow<String?> = _currentSoundscape
    private val _volume = MutableStateFlow(0.25f)
    val volume: StateFlow<Float> = _volume
    private val _modulation = MutableStateFlow(10.0)
    val modulation: StateFlow<Double> = _modulation
    private val _error = MutableStateFlow<Int?>(null)
    val error: StateFlow<Int?> = _error
    private val _framesWritten = MutableStateFlow(0L)
    val framesWritten: StateFlow<Long> = _framesWritten
    @Volatile var currentSound = AmbientSoundType.FOREST
        private set

    fun playSoundscape(type: String) {
        val sound = AmbientSoundType.entries.firstOrNull { it.name == type }
        if (sound == null) { _error.value = R.string.audio_unavailable; return }
        currentSound = sound
        _currentSoundscape.value = sound.name
    }
    fun setVolume(value: Float) { if (value.isFinite()) _volume.value = value.coerceIn(0f, 1f) }
    fun setModulation(value: Double) { if (value.isFinite()) _modulation.value = value.coerceIn(1.0, 40.0) }

    fun start(scope: CoroutineScope) {
        if (playbackJob?.isActive == true || !scope.isActive) return
        _error.value = null
        val token = synchronized(control) { _framesWritten.value = 0; ++generation }
        if (manager.requestAudioFocus(focus) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            _error.value = R.string.audio_focus_unavailable
            return
        }
        if (!noisyRegistered) {
            ContextCompat.registerReceiver(appContext, noisy, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY), ContextCompat.RECEIVER_NOT_EXPORTED)
            noisyRegistered = true
        }
        playbackJob = scope.launch(Dispatchers.IO) {
            writer.withLock {
                ensureActive()
                var track: AudioTrack? = null
                try {
                    val minimum = AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
                    check(minimum > 0)
                    val output = AudioTrack.Builder().setAudioAttributes(attributes)
                        .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(44100).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                        .setBufferSizeInBytes(maxOf(minimum, 22050)).setTransferMode(AudioTrack.MODE_STREAM).build()
                    track = output
                    check(output.state == AudioTrack.STATE_INITIALIZED)
                    ensureActive()
                    synchronized(control) {
                        if (token != generation) throw CancellationException("Playback stopped")
                        output.play()
                        _isPlaying.value = output.playState == AudioTrack.PLAYSTATE_PLAYING
                    }
                    val generator = PcmSoundGenerator()
                    val buffer = ShortArray(2048)
                    while (isActive && token == generation) {
                        generator.fill(currentSound, _volume.value, buffer, _modulation.value)
                        var offset = 0
                        while (offset < buffer.size && isActive && token == generation) {
                            val written = output.write(buffer, offset, buffer.size - offset, AudioTrack.WRITE_NON_BLOCKING)
                            check(written >= 0)
                            if (written == 0) delay(5) else {
                                offset += written
                                synchronized(control) { if (token == generation) _framesWritten.value += written }
                            }
                        }
                    }
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: RuntimeException) { if (token == generation) _error.value = R.string.audio_unavailable }
                finally {
                    try { track?.pause(); track?.flush() }
                    catch (_: RuntimeException) { if (token == generation) _error.value = R.string.audio_unavailable }
                    finally {
                        try { track?.release() }
                        catch (_: RuntimeException) { if (token == generation) _error.value = R.string.audio_unavailable }
                    }
                }
            }
        }
        playbackJob?.invokeOnCompletion {
            synchronized(control) {
                if (token == generation) {
                    _isPlaying.value = false
                    manager.abandonAudioFocusRequest(focus)
                }
            }
        }
    }
    fun stop() {
        synchronized(control) {
            generation++
            playbackJob?.cancel()
            _isPlaying.value = false
        }
        manager.abandonAudioFocusRequest(focus)
    }
    fun release() {
        stop()
        if (noisyRegistered) { appContext.unregisterReceiver(noisy); noisyRegistered = false }
    }
}
