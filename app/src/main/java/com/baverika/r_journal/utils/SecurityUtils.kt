package com.baverika.r_journal.utils

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object SecurityUtils {
    private const val ANDROID_KEY_STORE = "AndroidKeyStore"
    private const val ALIAS = "r_journal_password_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH_BITS = 128

    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val PBKDF2_ITERATIONS = 120_000
    private const val PBKDF2_KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH = 16

    private val secureRandom = SecureRandom()

    @Synchronized
    private fun ensureKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
        keyStore.load(null)
        if (!keyStore.containsAlias(ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        }
        val secretKeyEntry = keyStore.getEntry(ALIAS, null) as KeyStore.SecretKeyEntry
        return secretKeyEntry.secretKey
    }

    /**
     * Encrypts [data] using the Android Keystore AES-GCM key.
     * Throws [SecurityException] if encryption fails instead of silently falling back to plaintext.
     */
    fun encrypt(data: String): String {
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, ensureKey())
            val iv = cipher.iv
            val encryption = cipher.doFinal(data.toByteArray(StandardCharsets.UTF_8))

            val combined = ByteArray(iv.size + encryption.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryption, 0, combined, iv.size, encryption.size)

            return Base64.getEncoder().encodeToString(combined)
        } catch (e: Exception) {
            throw SecurityException("Encryption failed: ${e.message}", e)
        }
    }

    /**
     * Decrypts [encryptedData] produced by [encrypt].
     * Preserves legacy unencrypted fallback only if the input is not valid AES-GCM ciphertext.
     */
    fun decrypt(encryptedData: String): String {
        if (encryptedData.isEmpty()) return encryptedData
        return try {
            val combined = Base64.getMimeDecoder().decode(encryptedData)
            if (combined.size <= GCM_IV_LENGTH) {
                return encryptedData
            }

            val iv = ByteArray(GCM_IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)

            val encrypted = ByteArray(combined.size - GCM_IV_LENGTH)
            System.arraycopy(combined, GCM_IV_LENGTH, encrypted, 0, encrypted.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, ensureKey(), spec)

            val decoded = cipher.doFinal(encrypted)
            String(decoded, StandardCharsets.UTF_8)
        } catch (_: Exception) {
            // Backward compatibility for any legacy pre-encryption records
            encryptedData
        }
    }

    /**
     * Encrypts portable backup payloads using PBKDF2WithHmacSHA256 + AES-256-GCM.
     * Format: Base64(16-byte salt || 12-byte IV || ciphertext)
     */
    fun encryptWithPassphrase(data: String, passphrase: String): String {
        require(passphrase.isNotEmpty()) { "Passphrase cannot be empty" }
        val salt = ByteArray(SALT_LENGTH).also { secureRandom.nextBytes(it) }
        val key = deriveKey(passphrase, salt)
        val iv = ByteArray(GCM_IV_LENGTH).also { secureRandom.nextBytes(it) }

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        val ciphertext = cipher.doFinal(data.toByteArray(StandardCharsets.UTF_8))

        val combined = ByteArray(salt.size + iv.size + ciphertext.size)
        System.arraycopy(salt, 0, combined, 0, salt.size)
        System.arraycopy(iv, 0, combined, salt.size, iv.size)
        System.arraycopy(ciphertext, 0, combined, salt.size + iv.size, ciphertext.size)

        return Base64.getEncoder().encodeToString(combined)
    }

    /**
     * Decrypts a payload produced by [encryptWithPassphrase].
     * Throws [SecurityException] if the passphrase is wrong or the payload is corrupted.
     */
    fun decryptWithPassphrase(encryptedData: String, passphrase: String): String {
        if (passphrase.isEmpty()) throw SecurityException("Passphrase is required")
        try {
            val combined = Base64.getMimeDecoder().decode(encryptedData)
            if (combined.size <= SALT_LENGTH + GCM_IV_LENGTH) {
                throw SecurityException("Invalid encrypted backup payload")
            }

            val salt = combined.copyOfRange(0, SALT_LENGTH)
            val iv = combined.copyOfRange(SALT_LENGTH, SALT_LENGTH + GCM_IV_LENGTH)
            val ciphertext = combined.copyOfRange(SALT_LENGTH + GCM_IV_LENGTH, combined.size)

            val key = deriveKey(passphrase, salt)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            val plaintext = cipher.doFinal(ciphertext)
            return String(plaintext, StandardCharsets.UTF_8)
        } catch (e: SecurityException) {
            throw e
        } catch (e: Exception) {
            throw SecurityException("Incorrect passphrase or corrupted password backup", e)
        }
    }

    private fun deriveKey(passphrase: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH_BITS)
        val secret = factory.generateSecret(spec)
        return SecretKeySpec(secret.encoded, "AES")
    }
}
