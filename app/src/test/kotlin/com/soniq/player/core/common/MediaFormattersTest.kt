package com.soniq.player.core.common

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MediaFormattersTest {

    @Test
    fun formatsDurationsAcrossSecondsMinutesAndHours() {
        assertThat(MediaFormatters.formatDuration(-500L)).isEqualTo("0:00")
        assertThat(MediaFormatters.formatDuration(0L)).isEqualTo("0:00")
        assertThat(MediaFormatters.formatDuration(65_000L)).isEqualTo("1:05")
        assertThat(MediaFormatters.formatDuration(3_729_000L)).isEqualTo("1:02:09")
    }

    @Test
    fun formatsFileSizesAcrossUnits() {
        assertThat(MediaFormatters.formatFileSize(512L)).isEqualTo("512 B")
        assertThat(MediaFormatters.formatFileSize(20_480L)).isEqualTo("20 KB")
        assertThat(MediaFormatters.formatFileSize(15_728_640L)).isEqualTo("15.0 MB")
        assertThat(MediaFormatters.formatFileSize(2_147_483_648L)).isEqualTo("2.00 GB")
    }

    @Test
    fun formatsLandscapePortraitAndUnknownResolutions() {
        assertThat(MediaFormatters.formatResolution(0, 1080)).isNull()
        assertThat(MediaFormatters.formatResolution(3840, 2160)).isEqualTo("4K")
        assertThat(MediaFormatters.formatResolution(1920, 1080)).isEqualTo("1080p")
        assertThat(MediaFormatters.formatResolution(1080, 1920)).isEqualTo("1080p")
        assertThat(MediaFormatters.formatResolution(1280, 720)).isEqualTo("720p")
        assertThat(MediaFormatters.formatResolution(640, 360)).isEqualTo("640×360")
    }
}
