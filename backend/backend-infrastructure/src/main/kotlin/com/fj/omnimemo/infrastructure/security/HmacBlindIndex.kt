/*
 * HmacBlindIndex.kt
 *
 * $Since: 2026-05-23T00:00:00Z
 */
package com.fj.omnimemo.infrastructure.security

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * HMAC-SHA256 blind index for deterministic, privacy-preserving lookups.
 *
 * The same plaintext always produces the same output, enabling indexed
 * equality queries without storing plaintext in the database.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
class HmacBlindIndex(key: ByteArray) {

    private val secretKey = SecretKeySpec(key, ALGORITHM)

    fun compute(plaintext: String): ByteArray {
        val mac = Mac.getInstance(ALGORITHM)
        mac.init(secretKey)
        return mac.doFinal(plaintext.toByteArray(Charsets.UTF_8))
    }

    companion object {
        private const val ALGORITHM = "HmacSHA256"
    }
}
