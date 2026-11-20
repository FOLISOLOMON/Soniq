package com.soniq.player.core.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.Signature
import android.content.pm.SigningInfo
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

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }

    @Suppress("DEPRECATION")
    private fun legacyPackageInfo(
        signer: Signature?,
        versionCode: Int = 4,
    ): PackageInfo = PackageInfo().apply {
        packageName = context.packageName
        this.versionCode = versionCode
        signatures = if (signer != null) arrayOf(signer) else emptyArray()
    }

    @Test
    fun verifierRejectsSizeMismatchChecksumMismatchAndCorruptedApk() = runTest {
        val temp = File(context.cacheDir, "test-corrupt.apk").apply {
            writeText("not-a-valid-apk-archive")
        }
        val sha = sha256Hex(temp.readBytes())

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

    @Test
    fun extractSignerDigestsHandlesV2OnlyAndFallbackAndEmptySigners() {
        val certA = Signature(byteArrayOf(0x01, 0x02, 0x03, 0x04))
        val certB = Signature(byteArrayOf(0x05, 0x06, 0x07, 0x08))
        val digestA = sha256Hex(certA.toByteArray())
        val digestB = sha256Hex(certB.toByteArray())

        // v2-only APK: apkContentsSigners populated, signingCertificateHistory null
        assertThat(
            verifier.extractSignerDigestsFromParts(
                apkContentsSigners = arrayOf(certA),
                signingCertificateHistory = null,
                hasMultipleSigners = false,
                legacySignatures = null,
                isApi28Plus = true,
            ),
        ).containsExactly(digestA)

        // v3 rotated APK: prefers active apkContentsSigners (certB) over signingCertificateHistory
        assertThat(
            verifier.extractSignerDigestsFromParts(
                apkContentsSigners = arrayOf(certB),
                signingCertificateHistory = arrayOf(certA, certB),
                hasMultipleSigners = false,
                legacySignatures = null,
                isApi28Plus = true,
            ),
        ).containsExactly(digestB)

        // Empty signingCertificateHistory array with populated apkContentsSigners
        assertThat(
            verifier.extractSignerDigestsFromParts(
                apkContentsSigners = arrayOf(certA),
                signingCertificateHistory = emptyArray(),
                hasMultipleSigners = false,
                legacySignatures = null,
                isApi28Plus = true,
            ),
        ).containsExactly(digestA)

        // Fallback to signingCertificateHistory when apkContentsSigners is empty
        assertThat(
            verifier.extractSignerDigestsFromParts(
                apkContentsSigners = emptyArray(),
                signingCertificateHistory = arrayOf(certB),
                hasMultipleSigners = false,
                legacySignatures = null,
                isApi28Plus = true,
            ),
        ).containsExactly(digestB)

        // Multiple signers with empty apkContentsSigners does not fall back to history
        assertThat(
            verifier.extractSignerDigestsFromParts(
                apkContentsSigners = emptyArray(),
                signingCertificateHistory = arrayOf(certB),
                hasMultipleSigners = true,
                legacySignatures = null,
                isApi28Plus = true,
            ),
        ).isEmpty()

        // Unavailable / empty signer information on API 28+
        val noSigningInfo = PackageInfo().apply {
            packageName = context.packageName
            signingInfo = null
        }
        assertThat(verifier.extractSignerDigests(noSigningInfo)).isEmpty()

        val emptySigningInfo = PackageInfo().apply {
            packageName = context.packageName
            signingInfo = SigningInfo()
        }
        assertThat(verifier.extractSignerDigests(emptySigningInfo)).isEmpty()
    }

    @Test
    @Config(sdk = [26])
    fun extractSignerDigestsSupportsLegacyApiBelow28() {
        val certA = Signature(byteArrayOf(0x01, 0x02, 0x03, 0x04))
        val digestA = sha256Hex(certA.toByteArray())

        @Suppress("DEPRECATION")
        val legacyInfo = PackageInfo().apply {
            packageName = context.packageName
            signatures = arrayOf(certA)
        }
        assertThat(verifier.extractSignerDigests(legacyInfo)).containsExactly(digestA)

        @Suppress("DEPRECATION")
        val legacyEmpty = PackageInfo().apply {
            packageName = context.packageName
            signatures = emptyArray()
        }
        assertThat(verifier.extractSignerDigests(legacyEmpty)).isEmpty()
    }

    @Test
    @Config(sdk = [26])
    fun verifyArchiveMetadataDistinguishesUnreadableCertificateMismatchAndMatch() {
        val certA = Signature(byteArrayOf(0x11, 0x22, 0x33))
        val certB = Signature(byteArrayOf(0x44, 0x55, 0x66))
        val digestA = sha256Hex(certA.toByteArray())
        val digestB = sha256Hex(certB.toByteArray())
        val release = UpdateRelease(1, 4, "0.3.1", "", "https://example.com/a.apk", 100L, "a".repeat(64), 1, false, "")

        val validArchive = legacyPackageInfo(signer = certA, versionCode = 4)
        assertThat(verifier.verifyArchiveMetadata(validArchive, release, setOf(digestA)))
            .isEqualTo(UpdateVerificationResult.Valid)

        val mismatchResult = verifier.verifyArchiveMetadata(validArchive, release, setOf(digestB))
        assertThat(mismatchResult).isInstanceOf(UpdateVerificationResult.Invalid::class.java)
        assertThat((mismatchResult as UpdateVerificationResult.Invalid).reason)
            .contains("does not match the installed SONIQ signature")

        val unreadableArchive = legacyPackageInfo(signer = null, versionCode = 4)
        val unreadableResult = verifier.verifyArchiveMetadata(unreadableArchive, release, setOf(digestA))
        assertThat(unreadableResult).isInstanceOf(UpdateVerificationResult.Invalid::class.java)
        assertThat((unreadableResult as UpdateVerificationResult.Invalid).reason)
            .isEqualTo("Unable to read the APK signing certificate.")

        val unreadableInstalledResult = verifier.verifyArchiveMetadata(validArchive, release, emptySet())
        assertThat(unreadableInstalledResult).isInstanceOf(UpdateVerificationResult.Invalid::class.java)
        assertThat((unreadableInstalledResult as UpdateVerificationResult.Invalid).reason)
            .isEqualTo("Unable to read the installed SONIQ signing certificate.")
    }
}
