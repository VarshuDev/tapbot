package com.tapbot.core.network

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BundledPackagesCatalogTest {

    private fun resolveAssetsPackagesDir(): File {
        val candidates = listOf(
            File("app/src/main/assets/packages"),
            File("../../app/src/main/assets/packages"),
            File("../app/src/main/assets/packages")
        )
        return candidates.firstOrNull { it.exists() && it.isDirectory }
            ?: error("Could not locate app/src/main/assets/packages from ${File(".").absolutePath}")
    }

    @Test
    fun allMockCatalogBots_haveValidBundledPackagesAndMatchingChecksums() = runTest {
        val packagesDir = resolveAssetsPackagesDir()
        val mockApi = MockCatalogApi()
        val verifier = Sha256PackageVerifier()
        val validator = ManifestValidator(appVersionCode = 230)

        val bots = mockApi.getBots().getOrThrow()
        assertTrue("Expected built-in bots in MockCatalogApi", bots.size >= 8)

        for (bot in bots) {
            val pkgFilename = bot.packageInfo.packageUrl.substringAfterLast('/')
            val pkgFile = File(packagesDir, pkgFilename)
            assertTrue("Missing bundled package '$pkgFilename' for bot '${bot.id}'", pkgFile.exists())
            assertEquals("Package size mismatch for '${bot.id}' ($pkgFilename)", pkgFile.length(), bot.packageInfo.packageSizeBytes)

            val shaRes = verifier.verifyPackage(pkgFile, bot.packageInfo.sha256Checksum)
            assertTrue(
                "SHA-256 verification failed for '${bot.id}' ($pkgFilename): ${shaRes.exceptionOrNull()?.message}",
                shaRes.isSuccess
            )

            val manifestRes = validator.validatePackage(pkgFile, bot.id, bot.version)
            assertTrue(
                "Manifest validation failed for '${bot.id}' v${bot.version} ($pkgFilename): ${manifestRes.exceptionOrNull()?.message}",
                manifestRes.isSuccess
            )

            // Verify OkHttpBotPackageDownloader serves offline cache hit from seeded directory without network
            val tempOut = File.createTempFile("dl_${bot.id}_", ".botpkg")
            try {
                val downloader = OkHttpBotPackageDownloader(cacheDir = packagesDir)
                val dlRes = downloader.downloadPackage(
                    packageUrl = bot.packageInfo.packageUrl,
                    expectedSha256 = bot.packageInfo.sha256Checksum,
                    targetFile = tempOut
                )
                assertTrue("Cached download failed for '${bot.id}': ${dlRes.exceptionOrNull()?.message}", dlRes.isSuccess)
                assertEquals(pkgFile.length(), tempOut.length())
            } finally {
                tempOut.delete()
            }

            // Also verify every version listed in sampleVersions for this bot
            val versions = mockApi.getBotVersions(bot.id).getOrThrow()
            assertTrue("Expected published versions for '${bot.id}'", versions.isNotEmpty())
            for (ver in versions) {
                val verFile = File(packagesDir, ver.packageUrl.substringAfterLast('/'))
                assertTrue("Missing bundled package '${verFile.name}' for '${bot.id}' v${ver.version}", verFile.exists())
                assertEquals("Version size mismatch for '${bot.id}' v${ver.version}", verFile.length(), ver.packageSize)
                assertTrue(
                    "Version SHA mismatch for '${bot.id}' v${ver.version}",
                    verifier.verifyPackage(verFile, ver.sha256).isSuccess
                )
                assertTrue(
                    "Version manifest mismatch for '${bot.id}' v${ver.version}",
                    validator.validatePackage(verFile, bot.id, ver.version).isSuccess
                )
            }
        }
    }
}
