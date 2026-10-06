package com.thanu.steady.domain

import com.thanu.steady.platform.AmbientSoundType
import com.thanu.steady.platform.PcmSoundGenerator
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class PcmSoundGeneratorTest {
    @Test fun everySoundGeneratesNonSilentSamplesAndMutedStartIsSilent() {
        AmbientSoundType.entries.forEach { type ->
            val samples = ShortArray(44100)
            PcmSoundGenerator(Random(13)).fill(type, 0.25f, samples)
            assertTrue(type.name, samples.any { it.toInt() != 0 })
            PcmSoundGenerator(Random(13)).fill(type, 0f, samples)
            assertTrue(type.name, samples.all { it.toInt() == 0 })
        }
    }
    @Test fun SplittingBuffersPreservesOscillatorFilterAndRampContinuity() {
        AmbientSoundType.entries.forEach { type ->
            val continuous = ShortArray(4096)
            PcmSoundGenerator(Random(42)).fill(type, 0.4f, continuous, 40.0)
            val generator = PcmSoundGenerator(Random(42))
            val first = ShortArray(2048); val second = ShortArray(2048)
            generator.fill(type, 0.4f, first, 40.0); generator.fill(type, 0.4f, second, 40.0)
            assertArrayEquals(type.name, continuous, first + second)
        }
    }
    @Test fun invalidSignalParametersAreRejected() {
        val generator = PcmSoundGenerator(Random(42))
        assertThrows(IllegalArgumentException::class.java) { generator.fill(AmbientSoundType.FOREST, Float.NaN, ShortArray(10)) }
        assertThrows(IllegalArgumentException::class.java) { generator.fill(AmbientSoundType.FOREST, 2f, ShortArray(10)) }
        assertThrows(IllegalArgumentException::class.java) { generator.fill(AmbientSoundType.ALPHA_BINAURAL, 0.1f, ShortArray(10), Double.POSITIVE_INFINITY) }
    }
}
