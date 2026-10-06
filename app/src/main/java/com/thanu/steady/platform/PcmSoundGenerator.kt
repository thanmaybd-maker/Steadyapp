package com.thanu.steady.platform

import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Sample generation adapted from the owner-supplied OG AmbientSoundEngine.
 * Mono procedural textures; no claim of binaural or therapeutic effects.
 * Filter/oscillator state survives buffer boundaries.
 */
class PcmSoundGenerator(private val random: Random = Random.Default) {
    private val sampleRate = 44100
    private var b0 = 0f; private var b1 = 0f; private var b2 = 0f
    private var b3 = 0f; private var b4 = 0f; private var b5 = 0f; private var b6 = 0f
    private var brown = 0f
    private var forestLfoPhase = 0.0
    private var birdSamplesLeft = 0
    private var birdFreq = 2800f
    private var birdPhase = 0.0
    private var alphaPhase = 0.0
    private var alphaLfoPhase = 0.0
    private var crackleCoolDown = 0
    private var appliedVolume = 0f

    fun fill(sound: AmbientSoundType, volume: Float, buffer: ShortArray, modulationHz: Double = 10.0) {
        require(volume.isFinite() && volume in 0f..1f)
        require(modulationHz.isFinite() && modulationHz in 1.0..40.0)
        for (i in buffer.indices) {
            val rawSample: Float = when (sound) {
                AmbientSoundType.WHITE_NOISE -> {
                    (random.nextFloat() * 2f - 1f) * 0.35f
                }

                AmbientSoundType.RAIN -> {
                    val white = random.nextFloat() * 2f - 1f
                    b0 = 0.99886f * b0 + white * 0.0555179f
                    b1 = 0.99332f * b1 + white * 0.0750759f
                    b2 = 0.96900f * b2 + white * 0.1538520f
                    b3 = 0.86650f * b3 + white * 0.3104856f
                    b4 = 0.55000f * b4 + white * 0.5329522f
                    b5 = -0.7616f * b5 - white * 0.0168980f
                    val pink = (b0 + b1 + b2 + b3 + b4 + b5 + b6 + white * 0.5362f) * 0.11f
                    b6 = white * 0.115926f

                    // Add gentle droplet texture
                    val droplet = if (random.nextFloat() < 0.002f) (random.nextFloat() * 0.3f) else 0f
                    (pink * 0.75f + droplet) * 0.6f
                }

                AmbientSoundType.BROWN_NOISE -> {
                    val white = random.nextFloat() * 2f - 1f
                    brown = (brown + (0.025f * white)) / 1.025f
                    brown * 1.6f
                }

                AmbientSoundType.FOREST -> {
                    // Undulating wind breeze via LFO
                    forestLfoPhase += (2.0 * PI * 0.18) / sampleRate
                    if (forestLfoPhase > 2.0 * PI) forestLfoPhase -= 2.0 * PI
                    val windMod = 0.65f + 0.35f * sin(forestLfoPhase).toFloat()

                    // Pink noise base for leaf rustle
                    val white = random.nextFloat() * 2f - 1f
                    b0 = 0.99886f * b0 + white * 0.0555179f
                    b1 = 0.99332f * b1 + white * 0.0750759f
                    b2 = 0.96900f * b2 + white * 0.1538520f
                    val gentleBreeze = (b0 + b1 + b2) * 0.18f * windMod

                    // Occasional bird whistle in the canopy
                    var birdSound = 0f
                    if (birdSamplesLeft > 0) {
                        birdPhase += (2.0 * PI * birdFreq) / sampleRate
                        birdSound = (sin(birdPhase) * 0.22).toFloat()
                        birdFreq += (random.nextFloat() * 80f - 40f)
                        birdSamplesLeft--
                    } else if (random.nextFloat() < 0.00012f) {
                        birdSamplesLeft = (sampleRate * 0.18f).toInt()
                        birdFreq = 2500f + random.nextFloat() * 700f
                        birdPhase = 0.0
                    }

                    (gentleBreeze * 0.7f + birdSound) * 0.75f
                }

                AmbientSoundType.ALPHA_BINAURAL -> {
                    alphaPhase += (2.0 * PI * 432.0) / sampleRate
                    if (alphaPhase > 2.0 * PI) alphaPhase -= 2.0 * PI

                    alphaLfoPhase += (2.0 * PI * modulationHz) / sampleRate
                    if (alphaLfoPhase > 2.0 * PI) alphaLfoPhase -= 2.0 * PI

                    val alphaMod = 0.75f + 0.25f * sin(alphaLfoPhase).toFloat()
                    val tone = sin(alphaPhase).toFloat() * alphaMod * 0.32f

                    // Gentle pink cushion
                    val white = random.nextFloat() * 2f - 1f
                    b0 = 0.99886f * b0 + white * 0.0555179f
                    val softBed = b0 * 0.08f

                    tone + softBed
                }

                AmbientSoundType.CAMPFIRE -> {
                    // Warm brown noise base
                    val white = random.nextFloat() * 2f - 1f
                    brown = (brown + (0.022f * white)) / 1.022f
                    val warmHum = brown * 0.9f

                    // Cracking spark impulses
                    var crackle = 0f
                    if (crackleCoolDown <= 0 && random.nextFloat() < 0.003f) {
                        crackle = (random.nextFloat() * 2f - 1f) * 0.6f
                        crackleCoolDown = (random.nextFloat() * 600f).toInt() + 150
                    } else if (crackleCoolDown > 0) {
                        crackleCoolDown--
                    }

                    (warmHum + crackle) * 0.7f
                }
            }

            // Apply volume and scale to 16-bit PCM range
            appliedVolume += (volume - appliedVolume).coerceIn(-0.0005f, 0.0005f)
            val scaledSample = (rawSample * appliedVolume * 32767f).coerceIn(-32768f, 32767f)
            buffer[i] = scaledSample.toInt().toShort()
        }
    }
}
