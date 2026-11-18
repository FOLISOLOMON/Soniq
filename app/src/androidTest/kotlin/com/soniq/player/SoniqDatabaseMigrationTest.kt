package com.soniq.player

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.soniq.player.core.database.SoniqDatabase
import com.soniq.player.core.database.SoniqDatabaseMigrations
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SoniqDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        SoniqDatabase::class.java,
    )

    @Test
    fun migrate1To2PreservesUserThemePresetsAndCreatesStage5Tables() {
        val dbName = "soniq_migration_test.db"

        helper.createDatabase(dbName, 1).apply {
            execSQL(
                """
                INSERT INTO theme_presets (
                    name, theme_mode, amoled_black, dynamic_color, accent_argb,
                    font_family, text_scale, background_style, background_image_uri,
                    player_layout, created_at
                ) VALUES (
                    'Midnight OLED', 'DARK', 1, 0, -8626945,
                    'SYSTEM', 'DEFAULT', 'SOLID', NULL,
                    'CLASSIC', 1700000000000
                )
                """.trimIndent(),
            )
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(
            dbName,
            2,
            true,
            SoniqDatabaseMigrations.MIGRATION_1_2,
        )

        migratedDb.query("SELECT name, amoled_black, theme_mode FROM theme_presets").use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getString(0)).isEqualTo("Midnight OLED")
            assertThat(cursor.getInt(1)).isEqualTo(1)
            assertThat(cursor.getString(2)).isEqualTo("DARK")
        }

        // Verify new v2 tables accept writes cleanly.
        migratedDb.execSQL(
            "INSERT INTO playlists (name, created_at, updated_at) VALUES ('Road Trip', 100, 100)",
        )
        migratedDb.query("SELECT name FROM playlists").use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getString(0)).isEqualTo("Road Trip")
        }
        migratedDb.close()
    }
}
