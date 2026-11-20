package com.soniq.player.feature.player

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EqualizerGraphMathTest {

    @Test
    fun gainAndYPositionConversionsClampToSupportedRange() {
        assertThat(EqualizerGraphMath.gainMbToFraction(-1500, -1500, 1500)).isEqualTo(0f)
        assertThat(EqualizerGraphMath.gainMbToFraction(0, -1500, 1500)).isEqualTo(0.5f)
        assertThat(EqualizerGraphMath.gainMbToFraction(1500, -1500, 1500)).isEqualTo(1f)

        // Top of graph = maxMb, bottom = minMb, middle = 0
        assertThat(EqualizerGraphMath.yPositionToGainMb(10f, 10f, 200f, -1500, 1500)).isEqualTo(1500)
        assertThat(EqualizerGraphMath.yPositionToGainMb(110f, 10f, 200f, -1500, 1500)).isEqualTo(0)
        assertThat(EqualizerGraphMath.yPositionToGainMb(210f, 10f, 200f, -1500, 1500)).isEqualTo(-1500)
    }

    @Test
    fun bandPositionsAdaptToDifferentBandCountsAndNearestBandLookup() {
        val single = EqualizerGraphMath.bandXPositions(1, 20f, 200f)
        assertThat(single).containsExactly(120f)

        val five = EqualizerGraphMath.bandXPositions(5, 20f, 200f)
        assertThat(five).containsExactly(20f, 70f, 120f, 170f, 220f).inOrder()

        assertThat(EqualizerGraphMath.findNearestBandIndex(68f, five)).isEqualTo(1)
        assertThat(EqualizerGraphMath.findNearestBandIndex(215f, five)).isEqualTo(4)

        assertThat(EqualizerGraphMath.formatFrequencyLabel(60)).isEqualTo("60")
        assertThat(EqualizerGraphMath.formatFrequencyLabel(3600)).isEqualTo("3.6k")
        assertThat(EqualizerGraphMath.formatGainDb(400)).isEqualTo("+4 dB")
        assertThat(EqualizerGraphMath.formatGainDb(-300)).isEqualTo("-3 dB")
    }
}
