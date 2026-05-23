/*
 * AesGcmCipher.kt
 *
 * $Since: 2026-05-23T00:00:00Z
 */
package com.fj.omnimemo.infrastructure.security

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM symmetric cipher for encrypting and decrypting string values.
 *
 * Each call to [generateIv] produces a fresh 12-byte IV that must be stored
 * alongside the ciphertext so that [decrypt] can reconstruct the plaintext.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
class AesGcmCipher(key: ByteArray) {

    private val secretKey = SecretKeySpec(key, AES_ALGORITHM)
    private val secureRandom = SecureRandom()

    fun generateIv(): ByteArray {
        val iv = ByteArray(GCM_IV_BYTES)
        secureRandom.nextBytes(iv)
        return iv
    }

    fun encrypt(plaintext: String, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
    }

    fun decrypt(ciphertext: ByteArray, iv: ByteArray): String {
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    companion object {
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val AES_ALGORITHM = "AES"
        private const val GCM_IV_BYTES = 12
        private const val GCM_TAG_BITS = 128
    }
}
