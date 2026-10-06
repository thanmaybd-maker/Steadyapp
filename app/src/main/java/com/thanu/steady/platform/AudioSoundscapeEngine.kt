package com.thanu.steady.platform

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

enum class AmbientSoundType(
    val title: String,
    val iconEmoji: String,
    val description: String
) {
    FOREST("Forest & Breeze", "🌲", "Undulating pine wind with distant birds"),
    RAIN("Gentle Rain", "🌧️", "Pink noise waterfall and steady raindrops"),
    BROWN_NOISE("Deep Brown Noise", "☕", "Warm low-frequency rumble for ADHD focus"),
    WHITE_NOISE("Crisp White Noise", "💨", "Even static masking library & cafe chatter"),
    ALPHA_BINAURAL("Alpha Focus 432Hz", "🧠", "Harmonic calm tone for flow states"),
    CAMPFIRE("Twilight Campfire", "🪵", "Crackling embers & gentle evening breeze")
}

class AudioSoundscapeEngine(private val context: Context) {

    private val sampleRate = 44100
    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentSoundscape = MutableStateFlow<String?>(null)
    val currentSoundscape: StateFlow<String?> = _currentSoundscape

    private val _volume = MutableStateFlow(0.68f)
    val volume: StateFlow<Float> = _volume

    fun playSoundscape(type: String) {
        val mappedType = try { AmbientSoundType.valueOf(type) } catch (e: Exception) { AmbientSoundType.FOREST }
        _currentSoundscape.value = type
        currentSound = mappedType
        if (!_isPlaying.value) {
            // we will need a scope to launch this.
        }
    }

    @Volatile
    var currentSound: AmbientSoundType = AmbientSoundType.FOREST
        private set

    fun setVolume(vol: Float) {
        _volume.value = vol.coerceIn(0f, 1f)
    }

    fun start(scope: CoroutineScope) {
        if (_isPlaying.value) return
        _isPlaying.value = true

        initAudioTrack()

        playbackJob = scope.launch(Dispatchers.Default) {
            val bufferSize = 2048
            val buffer = ShortArray(bufferSize)

            // Filter states for pink noise (Kellet filter)
            var b0 = 0f
            var b1 = 0f
            var b2 = 0f
            var b3 = 0f
            var b4 = 0f
            var b5 = 0f
            var b6 = 0f

            // Filter state for brown noise
            var brown = 0f

            // States for forest wind LFO and bird chirps
            var forestLfoPhase = 0.0
            var birdSamplesLeft = 0
            var birdFreq = 2800f
            var birdPhase = 0.0

            // States for Alpha tone
            var alphaPhase = 0.0
            var alphaLfoPhase = 0.0

            // States for campfire crackle
            var crackleCoolDown = 0

            audioTrack?.play()

            while (isActive && _isPlaying.value) {
                val sound = currentSound
                val currentVol = _volume.value

                for (i in 0 until bufferSize) {
                    val rawSample: Float = when (sound) {
                        AmbientSoundType.WHITE_NOISE -> {
                            (Random.nextFloat() * 2f - 1f) * 0.35f
                        }

                        AmbientSoundType.RAIN -> {
                            val white = Random.nextFloat() * 2f - 1f
                            b0 = 0.99886f * b0 + white * 0.0555179f
                            b1 = 0.99332f * b1 + white * 0.0750759f
                            b2 = 0.96900f * b2 + white * 0.1538520f
                            b3 = 0.86650f * b3 + white * 0.3104856f
                            b4 = 0.55000f * b4 + white * 0.5329522f
                            b5 = -0.7616f * b5 - white * 0.0168980f
                            val pink = (b0 + b1 + b2 + b3 + b4 + b5 + b6 + white * 0.5362f) * 0.11f
                            b6 = white * 0.115926f

                            // Add gentle droplet texture
                            val droplet = if (Random.nextFloat() < 0.002f) (Random.nextFloat() * 0.3f) else 0f
                            (pink * 0.75f + droplet) * 0.6f
                        }

                        AmbientSoundType.BROWN_NOISE -> {
                            val white = Random.nextFloat() * 2f - 1f
                            brown = (brown + (0.025f * white)) / 1.025f
                            brown * 1.6f
                        }

                        AmbientSoundType.FOREST -> {
                            // Undulating wind breeze via LFO
                            forestLfoPhase += (2.0 * PI * 0.18) / sampleRate
                            if (forestLfoPhase > 2.0 * PI) forestLfoPhase -= 2.0 * PI
                            val windMod = 0.65f + 0.35f * sin(forestLfoPhase).toFloat()

                            // Pink noise base for leaf rustle
                            val white = Random.nextFloat() * 2f - 1f
                            b0 = 0.99886f * b0 + white * 0.0555179f
                            b1 = 0.99332f * b1 + white * 0.0750759f
                            b2 = 0.96900f * b2 + white * 0.1538520f
                            val gentleBreeze = (b0 + b1 + b2) * 0.18f * windMod

                            // Occasional bird whistle in the canopy
                            var birdSound = 0f
                            if (birdSamplesLeft > 0) {
                                birdPhase += (2.0 * PI * birdFreq) / sampleRate
                                birdSound = (sin(birdPhase) * 0.22).toFloat()
                                birdFreq += (Random.nextFloat() * 80f - 40f)
                                birdSamplesLeft--
                            } else if (Random.nextFloat() < 0.00012f) {
                                birdSamplesLeft = (sampleRate * 0.18f).toInt()
                                birdFreq = 2500f + Random.nextFloat() * 700f
                                birdPhase = 0.0
                            }

                            (gentleBreeze * 0.7f + birdSound) * 0.75f
                        }

                        AmbientSoundType.ALPHA_BINAURAL -> {
                            alphaPhase += (2.0 * PI * 432.0) / sampleRate
                            if (alphaPhase > 2.0 * PI) alphaPhase -= 2.0 * PI

                            alphaLfoPhase += (2.0 * PI * 10.0) / sampleRate
                            if (alphaLfoPhase > 2.0 * PI) alphaLfoPhase -= 2.0 * PI

                            val alphaMod = 0.75f + 0.25f * sin(alphaLfoPhase).toFloat()
                            val tone = sin(alphaPhase).toFloat() * alphaMod * 0.32f

                            // Gentle pink cushion
                            val white = Random.nextFloat() * 2f - 1f
                            b0 = 0.99886f * b0 + white * 0.0555179f
                            val softBed = b0 * 0.08f

                            tone + softBed
                        }

                        AmbientSoundType.CAMPFIRE -> {
                            // Warm brown noise base
                            val white = Random.nextFloat() * 2f - 1f
                            brown = (brown + (0.022f * white)) / 1.022f
                            val warmHum = brown * 0.9f

                            // Cracking spark impulses
                            var crackle = 0f
                            if (crackleCoolDown <= 0 && Random.nextFloat() < 0.003f) {
                                crackle = (Random.nextFloat() * 2f - 1f) * 0.6f
                                crackleCoolDown = (Random.nextFloat() * 600f).toInt() + 150
                            } else if (crackleCoolDown > 0) {
                                crackleCoolDown--
                            }

                            (warmHum + crackle) * 0.7f
                        }
                    }

                    // Apply volume and scale to 16-bit PCM range
                    val scaledSample = (rawSample * currentVol * 32767f).coerceIn(-32768f, 32767f)
                    buffer[i] = scaledSample.toInt().toShort()
                }

                audioTrack?.write(buffer, 0, bufferSize)
            }
        }
    }

    fun stop() {
        _isPlaying.value = false
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.pause()
            audioTrack?.flush()
        } catch (_: Exception) {
        }
    }

    fun release() {
        stop()
        try {
            audioTrack?.release()
            audioTrack = null
        } catch (_: Exception) {
        }
    }

    private fun initAudioTrack() {
        if (audioTrack != null) return

        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(minBufferSize, sampleRate / 2)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }
}
