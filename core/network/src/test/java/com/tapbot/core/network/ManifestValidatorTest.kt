package com.tapbot.core.network

import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ManifestValidatorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `validatePackage accepts valid signed zip containing manifest`() {
        val pkg = tempFolder.newFile("valid.botpkg")
        ZipOutputStream(pkg.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write("""{"id":"bot_ping_pong","version":"1.0.0","runtime":"native_art","minimumAppVersion":1}""".toByteArray())
            zip.closeEntry()
        }

        val res = ManifestValidator(appVersionCode = 240).validatePackage(pkg, "bot_ping_pong", "1.0.0")
        assertTrue(res.isSuccess)
    }

    @Test
    fun `validatePackage rejects plain text non-zip spoofed file`() {
        val spoofed = tempFolder.newFile("spoofed.botpkg")
        spoofed.writeText("NOT_A_ZIP_ARCHIVE")

        val res = ManifestValidator().validatePackage(spoofed, "bot_ping_pong", "1.0.0")
        assertTrue("Non-zip archive must fail closed instead of falling back", res.isFailure)
    }

    @Test
    fun `validatePackage rejects Zip-Slip traversal entries`() {
        val maliciousZip = tempFolder.newFile("zipslip.botpkg")
        ZipOutputStream(maliciousZip.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("../../etc/evil.txt"))
            zip.write("pwned".toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write("""{"id":"bot_ping_pong","version":"1.0.0"}""".toByteArray())
            zip.closeEntry()
        }

        val res = ManifestValidator().validatePackage(maliciousZip, "bot_ping_pong", "1.0.0")
        assertTrue(res.isFailure)
        assertTrue(res.exceptionOrNull() is SecurityException)
    }
}
