/*
 * JwtTokenService.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package com.fj.omnimemo.infrastructure.security

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jose.crypto.MACVerifier
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import java.time.Instant
import java.util.Date
import javax.crypto.SecretKey

/**
 * Issues and verifies HS256-signed JWTs.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
class JwtTokenService(private val signingKey: SecretKey) {

    fun issue(subject: String, expiresAt: Instant): String {
        val jwt = SignedJWT(
            JWSHeader(JWSAlgorithm.HS256),
            JWTClaimsSet.Builder()
                .subject(subject)
                .issueTime(Date())
                .expirationTime(Date.from(expiresAt))
                .build()
        )
        jwt.sign(MACSigner(signingKey))
        return jwt.serialize()
    }

    // Each early return corresponds to a distinct, named JWT failure mode (signature invalid,
    // missing expiry, expired). Collapsing them would obscure the failure semantics.
    @Suppress("ReturnCount")
    fun verify(token: String): String? {
        return try {
            val jwt = SignedJWT.parse(token)
            if (!jwt.verify(MACVerifier(signingKey))) return null
            val expiry = jwt.jwtClaimsSet.expirationTime ?: return null
            if (Date().after(expiry)) return null
            jwt.jwtClaimsSet.subject
        } catch (e: Exception) {
            null
        }
    }
}
