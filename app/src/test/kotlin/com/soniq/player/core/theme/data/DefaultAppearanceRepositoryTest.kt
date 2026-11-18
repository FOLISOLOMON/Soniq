package com.soniq.player.core.theme.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.soniq.player.core.database.SoniqDatabase
import com.soniq.player.core.datastore.AppearancePreferencesDataSource
import com.soniq.player.core.model.appearance.BackgroundStyle
import com.soniq.player.core.model.appearance.FontFamilyOption
import com.soniq.player.core.model.appearance.PlayerLayout
import com.soniq.player.core.model.appearance.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DefaultAppearanceRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var database: SoniqDatabase
    private val fixedClock: Clock = Clock.fixed(Instant.ofEpochMilli(1_700_000_000_000L), ZoneOffset.UTC)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SoniqDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun savesAppliesAndDeletesThemePresetsDeterministically() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { File(tempFolder.newFolder(), "repo_appearance.preferences_pb") },
        )
        val repository = DefaultAppearanceRepository(
            preferences = AppearancePreferencesDataSource(dataStore),
            presetDao = database.themePresetDao(),
            clock = fixedClock,
        )

        repository.updateAppearance {
            it.copy(
                themeMode = ThemeMode.DARK,
                useAmoledBlack = true,
                fontFamily = FontFamilyOption.SERIF,
                backgroundStyle = BackgroundStyle.GRADIENT,
                backgroundScrimAlpha = 0.55f,
                backgroundBlurDp = 12,
                playerLayout = PlayerLayout.COMPACT,
            )
        }

        val presetId = repository.saveCurrentAsPreset("  Studio Black  ")
        val savedPresets = repository.presets.first()
        assertThat(savedPresets).hasSize(1)
        assertThat(savedPresets.first().name).isEqualTo("Studio Black")
        assertThat(savedPresets.first().createdAtEpochMillis).isEqualTo(1_700_000_000_000L)

        // Mutate current appearance away from the preset, then apply the preset back.
        repository.updateAppearance {
            it.copy(
                themeMode = ThemeMode.LIGHT,
                useAmoledBlack = false,
                fontFamily = FontFamilyOption.SYSTEM,
            )
        }
        assertThat(repository.appearance.first().themeMode).isEqualTo(ThemeMode.LIGHT)

        assertThat(repository.applyPreset(presetId)).isTrue()
        assertThat(repository.applyPreset(999L)).isFalse()

        val restored = repository.appearance.first()
        assertThat(restored.themeMode).isEqualTo(ThemeMode.DARK)
        assertThat(restored.useAmoledBlack).isTrue()
        assertThat(restored.fontFamily).isEqualTo(FontFamilyOption.SERIF)
        assertThat(restored.backgroundScrimAlpha).isWithin(0.01f).of(0.55f)
        assertThat(restored.backgroundBlurDp).isEqualTo(12)
        assertThat(restored.playerLayout).isEqualTo(PlayerLayout.COMPACT)

        assertThat(repository.renamePreset(presetId, "Midnight Studio")).isTrue()
        assertThat(repository.presets.first().first().name).isEqualTo("Midnight Studio")

        val copyId = repository.duplicatePreset(presetId)
        assertThat(copyId).isNotNull()
        assertThat(repository.presets.first().map { it.name })
            .containsExactly("Midnight Studio Copy", "Midnight Studio")

        repository.deletePreset(presetId)
        repository.deletePreset(copyId!!)
        assertThat(repository.presets.first()).isEmpty()

        // Starter presets install cleanly and skip duplicates on repeat calls.
        assertThat(repository.installStarterPresets()).isEqualTo(SoniqStarterPresets.all.size)
        assertThat(repository.installStarterPresets()).isEqualTo(0)
        assertThat(repository.presets.first()).hasSize(SoniqStarterPresets.all.size)
    }

    @Test
    fun rejectsBlankOrOversizedPresetNames() {
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { File(tempFolder.newFolder(), "validation.preferences_pb") },
        )
        val repository = DefaultAppearanceRepository(
            preferences = AppearancePreferencesDataSource(dataStore),
            presetDao = database.themePresetDao(),
            clock = fixedClock,
        )

        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { repository.saveCurrentAsPreset("   ") }
        }
        assertThrows(IllegalArgumentException::class.java) {
            val oversized = "a".repeat(DefaultAppearanceRepository.MAX_PRESET_NAME_LENGTH + 1)
            kotlinx.coroutines.runBlocking { repository.saveCurrentAsPreset(oversized) }
        }
    }
}
