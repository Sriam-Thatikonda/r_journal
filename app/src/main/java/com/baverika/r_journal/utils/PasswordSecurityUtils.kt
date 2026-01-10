package com.baverika.r_journal.utils

import java.security.MessageDigest

object PasswordSecurityUtils {
    /**
     * Hashes a plaintext password using SHA-256.
     */
    fun hashPassword(plaintext: String): String {
        val bytes = plaintext.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}
