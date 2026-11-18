package com.soniq.player.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.soniq.player.core.database.entity.ThemePresetEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ThemePresetDaoTest {

    private lateinit var database: SoniqDatabase

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
    fun insertsObservesNewestFirstGetsAndDeletes() = runTest {
        val dao = database.themePresetDao()

        val firstId = dao.insert(sampleEntity(name = "Daylight", createdAt = 1_000L))
        val secondId = dao.insert(sampleEntity(name = "Midnight", createdAt = 2_000L))

        val observed = dao.observeAll().first()
        assertThat(observed.map { it.name }).containsExactly("Midnight", "Daylight").inOrder()

        val fetched = dao.getById(firstId)
        assertThat(fetched?.name).isEqualTo("Daylight")

        assertThat(dao.deleteById(secondId)).isEqualTo(1)
        assertThat(dao.deleteById(999L)).isEqualTo(0)
        assertThat(dao.observeAll().first().map { it.id }).containsExactly(firstId)
    }

    private fun sampleEntity(name: String, createdAt: Long) = ThemePresetEntity(
        name = name,
        themeMode = "DARK",
        amoledBlack = true,
        dynamicColor = false,
        accentArgb = 0xFF7C5CFF.toInt(),
        fontFamily = "SYSTEM",
        textScale = "DEFAULT",
        backgroundStyle = "GRADIENT",
        backgroundImageUri = null,
        playerLayout = "CLASSIC",
        createdAtEpochMillis = createdAt,
    )
}
