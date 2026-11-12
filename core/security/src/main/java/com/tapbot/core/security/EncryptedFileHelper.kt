package com.tapbot.core.security

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException

/**
 * Synchronous AES-256-GCM encrypted file helper backed by Android Keystore [MasterKey].
 * Supports safe overwrite via backup/restore (preserving the file name used as Tink AAD)
 * and transparent one-time migration of legacy plaintext JSON files.
 */
object EncryptedFileHelper {

    /**
     * Reads UTF-8 text from [file] using [EncryptedFile].
     * If [file] does not exist or is empty, returns null.
     * If decryption fails and [file] appears to contain legacy plaintext JSON (starting with `{` or `[`),
     * migrates [file] in place to encrypted storage and returns the plaintext once.
     */
    fun readUtf8WithMigration(context: Context, file: File): String? {
        if (!file.exists() || file.length() == 0L) return null

        val masterKey = createMasterKey(context)
        return try {
            readEncryptedUtf8(context, file, masterKey)
        } catch (e: GeneralSecurityException) {
            tryMigrateLegacyPlaintext(context, file, masterKey, e)
        } catch (e: IOException) {
            tryMigrateLegacyPlaintext(context, file, masterKey, e)
        }
    }

    /**
     * Encrypts and writes [content] as UTF-8 to [file] using a `.bak` rollback file so that
     * the target filename stays identical (required by [EncryptedFile] Tink AAD) without risking data loss.
     */
    fun writeEncryptedUtf8(context: Context, file: File, content: String) {
        val parent = file.parentFile
        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }

        val masterKey = createMasterKey(context)
        val backup = File(file.parentFile, "${file.name}.bak")
        if (backup.exists()) {
            backup.delete()
        }

        val hadOriginal = file.exists()
        if (hadOriginal) {
            file.copyTo(backup, overwrite = true)
            if (!file.delete()) {
                backup.delete()
                throw IOException("Unable to replace encrypted file: ${file.name}")
            }
        }

        try {
            val encryptedFile = EncryptedFile.Builder(
                context.applicationContext,
                file,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            encryptedFile.openFileOutput().use { output ->
                output.write(content.toByteArray(Charsets.UTF_8))
                output.flush()
            }
            if (backup.exists()) {
                backup.delete()
            }
        } catch (t: Throwable) {
            if (file.exists()) {
                file.delete()
            }
            if (hadOriginal && backup.exists()) {
                backup.renameTo(file)
            }
            throw t
        }
    }

    private fun createMasterKey(context: Context): MasterKey {
        return MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private fun readEncryptedUtf8(context: Context, file: File, masterKey: MasterKey): String {
        val encryptedFile = EncryptedFile.Builder(
            context.applicationContext,
            file,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        return encryptedFile.openFileInput().use { input ->
            val buffer = ByteArrayOutputStream()
            val chunk = ByteArray(4096)
            while (true) {
                val read = input.read(chunk)
                if (read <= 0) break
                buffer.write(chunk, 0, read)
            }
            buffer.toString(Charsets.UTF_8.name())
        }
    }

    private fun tryMigrateLegacyPlaintext(
        context: Context,
        file: File,
        masterKey: MasterKey,
        originalError: Exception
    ): String? {
        val candidate = runCatching { file.readText(Charsets.UTF_8) }.getOrNull()?.trimStart()
        if (candidate != null && (candidate.startsWith("[") || candidate.startsWith("{"))) {
            writeEncryptedUtf8(context, file, candidate)
            return candidate
        }
        throw originalError
    }
}
