package com.soniq.player.core.playback

import android.content.Context
import android.graphics.Bitmap
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutionException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SoniqOfflineBitmapLoaderTest {

    @Test
    fun rejectsRemoteSchemesAndAllowsLocalSchemes() {
        val resolver = ApplicationProvider.getApplicationContext<Context>().contentResolver
        val loader = SoniqOfflineBitmapLoader(resolver, Dispatchers.Unconfined)

        assertThat(loader.isAllowedLocalScheme("content")).isTrue()
        assertThat(loader.isAllowedLocalScheme("file")).isTrue()
        assertThat(loader.isAllowedLocalScheme("android.resource")).isTrue()
        assertThat(loader.isAllowedLocalScheme("http")).isFalse()
        assertThat(loader.isAllowedLocalScheme("https")).isFalse()

        val httpFuture = loader.loadBitmap("https://example.com/cover.jpg".toUri())
        val error = assertThrows(ExecutionException::class.java) { httpFuture.get() }
        assertThat(error.cause).isInstanceOf(SecurityException::class.java)
    }

    @Test
    fun decodesEmbeddedBytesAndCalculatesDownsamplingFactor() {
        val resolver = ApplicationProvider.getApplicationContext<Context>().contentResolver
        val loader = SoniqOfflineBitmapLoader(resolver, Dispatchers.Unconfined)

        assertThat(SoniqOfflineBitmapLoader.calculateInSampleSize(2048, 2048, 512)).isEqualTo(4)
        assertThat(SoniqOfflineBitmapLoader.calculateInSampleSize(400, 400, 512)).isEqualTo(1)

        val stream = ByteArrayOutputStream()
        Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
            .compress(Bitmap.CompressFormat.PNG, 100, stream)

        val decoded = loader.decodeBitmap(stream.toByteArray()).get()
        assertThat(decoded.width).isEqualTo(64)
        assertThat(decoded.height).isEqualTo(64)
    }
}
