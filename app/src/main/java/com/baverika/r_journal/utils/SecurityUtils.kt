package com.baverika.r_journal.utils

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.util.Base64
import java.nio.charset.StandardCharsets

object SecurityUtils {
    private const val ANDROID_KEY_STORE = "AndroidKeyStore"
    private const val ALIAS = "r_journal_password_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    init {
        createKey()
    }

    private fun createKey() {
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
    }

    private fun getKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
        keyStore.load(null)
        val secretKeyEntry = keyStore.getEntry(ALIAS, null) as KeyStore.SecretKeyEntry
        return secretKeyEntry.secretKey
    }

    fun encrypt(data: String): String {
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getKey())
            val iv = cipher.iv
            val encryption = cipher.doFinal(data.toByteArray(StandardCharsets.UTF_8))
            
            // Combine IV and encrypted data
            val combined = ByteArray(iv.size + encryption.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryption, 0, combined, iv.size, encryption.size)
            
            return Base64.encodeToString(combined, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            return data // Fallback to storing plaintext if encryption fails (should not happen)
        }
    }

    fun decrypt(encryptedData: String): String {
        try {
            val combined = Base64.decode(encryptedData, Base64.DEFAULT)
            
            // Extract IV (GCM standard is 12 bytes)
            val iv = ByteArray(12)
            System.arraycopy(combined, 0, iv, 0, 12)
            
            // Extract encrypted part
            val encrypted = ByteArray(combined.size - 12)
            System.arraycopy(combined, 12, encrypted, 0, encrypted.size)
            
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getKey(), spec)
            
            val decoded = cipher.doFinal(encrypted)
            return String(decoded, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            // e.printStackTrace() 
            // If decryption fails (e.g., old hashed data or plaintext), return original
            return encryptedData 
        }
    }
}
