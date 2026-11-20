package com.soniq.player.core.update

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AndroidApkVerifierTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val verifier = AndroidApkVerifier(context, Dispatchers.Unconfined)

    @Test
    fun verifierRejectsSizeMismatchChecksumMismatchAndCorruptedApk() = runTest {
        val temp = File(context.cacheDir, "test-corrupt.apk").apply {
            writeText("not-a-valid-apk-archive")
        }
        val sha = MessageDigest.getInstance("SHA-256")
            .digest(temp.readBytes())
            .joinToString("") { "%02x".format(it) }

        val sizeMismatch = UpdateRelease(1, 2, "0.2.0", "", "https://example.com/a.apk", 9999L, sha, 1, false, "")
        assertThat(verifier.verifyDownloadedApk(temp, sizeMismatch)).isInstanceOf(UpdateVerificationResult.Invalid::class.java)

        temp.writeText("not-a-valid-apk-archive")
        val shaMismatch = UpdateRelease(1, 2, "0.2.0", "", "https://example.com/a.apk", temp.length(), "b".repeat(64), 1, false, "")
        assertThat(verifier.verifyDownloadedApk(temp, shaMismatch)).isInstanceOf(UpdateVerificationResult.Invalid::class.java)

        temp.writeText("not-a-valid-apk-archive")
        val corruptApk = UpdateRelease(1, 2, "0.2.0", "", "https://example.com/a.apk", temp.length(), sha, 1, false, "")
        val res = verifier.verifyDownloadedApk(temp, corruptApk)
        assertThat(res).isInstanceOf(UpdateVerificationResult.Invalid::class.java)
        assertThat((res as UpdateVerificationResult.Invalid).reason).contains("not a valid Android APK")
    }
}
