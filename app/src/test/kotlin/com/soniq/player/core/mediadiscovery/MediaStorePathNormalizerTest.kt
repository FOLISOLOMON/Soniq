package com.soniq.player.core.mediadiscovery

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MediaStorePathNormalizerTest {

    @Test
    fun normalizesRelativePaths() {
        val nested = MediaStorePathNormalizer.fromRelativePath("Music/Ambient/2026/")
        assertThat(nested.path).isEqualTo("Music/Ambient/2026")
        assertThat(nested.name).isEqualTo("2026")

        val root = MediaStorePathNormalizer.fromRelativePath(" / ")
        assertThat(root.path).isEmpty()
        assertThat(root.name).isEqualTo(MediaStorePathNormalizer.ROOT_FOLDER_NAME)
    }

    @Test
    fun normalizesLegacyAbsoluteDataPaths() {
        val info = MediaStorePathNormalizer.fromLegacyDataPath("/storage/emulated/0/Movies/Trips/clip.mp4")
        assertThat(info.path).isEqualTo("/storage/emulated/0/Movies/Trips")
        assertThat(info.name).isEqualTo("Trips")
    }

    @Test
    fun stripsExtensionsAndNormalizesUnknownMediaStoreTags() {
        assertThat(MediaStorePathNormalizer.stripExtension("01. Midnight Echo.flac"))
            .isEqualTo("01. Midnight Echo")
        assertThat(MediaStorePathNormalizer.stripExtension("README")).isEqualTo("README")

        assertThat(MediaStorePathNormalizer.normalizeTag("<unknown>", "Unknown Artist"))
            .isEqualTo("Unknown Artist")
        assertThat(MediaStorePathNormalizer.normalizeTag("  ", "Unknown Album"))
            .isEqualTo("Unknown Album")
        assertThat(MediaStorePathNormalizer.normalizeTag("  Bonobo ", "Unknown Artist"))
            .isEqualTo("Bonobo")
    }
}
