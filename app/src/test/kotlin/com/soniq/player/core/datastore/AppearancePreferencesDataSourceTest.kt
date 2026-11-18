package com.soniq.player.core.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import com.google.common.truth.Truth.assertThat
import com.soniq.player.core.model.appearance.AppearanceConfig
import com.soniq.player.core.model.appearance.BackgroundStyle
import com.soniq.player.core.model.appearance.FontFamilyOption
import com.soniq.player.core.model.appearance.PlayerLayout
import com.soniq.player.core.model.appearance.TextScale
import com.soniq.player.core.model.appearance.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppearancePreferencesDataSourceTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun emitsDefaultsWhenEmptyAndPersistsFullConfigRoundTrip() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { File(tempFolder.newFolder(), "appearance.preferences_pb") },
        )
        val dataSource = AppearancePreferencesDataSource(dataStore)

        assertThat(dataSource.appearance.first()).isEqualTo(AppearanceConfig())

        val customized = AppearanceConfig(
            themeMode = ThemeMode.DARK,
            useAmoledBlack = true,
            useDynamicColor = false,
            accentColorArgb = 0xFF1DB98A.toInt(),
            fontFamily = FontFamilyOption.MONOSPACE,
            textScale = TextScale.LARGE,
            backgroundStyle = BackgroundStyle.IMAGE,
            backgroundImageUri = "content://com.android.providers.media/image/42",
            playerLayout = PlayerLayout.ARTWORK_FOCUS,
        )
        dataSource.replace(customized)

        assertThat(dataSource.appearance.first()).isEqualTo(customized)

        // Clearing the image URI removes the key and degrades effective style to SOLID.
        dataSource.update { it.copy(backgroundImageUri = null) }
        val cleared = dataSource.appearance.first()
        assertThat(cleared.backgroundImageUri).isNull()
        assertThat(cleared.effectiveBackgroundStyle).isEqualTo(BackgroundStyle.SOLID)
    }

    @Test
    fun fallsBackToDefaultsWhenPersistedEnumValuesAreUnknown() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { File(tempFolder.newFolder(), "corrupt_enums.preferences_pb") },
        )
        dataStore.edit { prefs ->
            prefs[AppearancePreferencesDataSource.Keys.THEME_MODE] = "REMOVED_MODE"
            prefs[AppearancePreferencesDataSource.Keys.FONT_FAMILY] = "FUTURE_FONT"
            prefs[AppearancePreferencesDataSource.Keys.TEXT_SCALE] = "EXTRA_HUGE"
            prefs[AppearancePreferencesDataSource.Keys.BACKGROUND_STYLE] = "VIDEO_LOOP"
            prefs[AppearancePreferencesDataSource.Keys.PLAYER_LAYOUT] = "HOLOGRAPHIC"
        }

        val dataSource = AppearancePreferencesDataSource(dataStore)
        assertThat(dataSource.appearance.first()).isEqualTo(AppearanceConfig())
    }
}
