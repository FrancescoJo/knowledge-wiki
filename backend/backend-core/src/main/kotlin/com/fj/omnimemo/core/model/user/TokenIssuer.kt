/*
 * TokenIssuer.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package com.fj.omnimemo.core.model.user

/**
 * Contract for issuing authentication tokens.
 *
 * Decouples the login domain from the token format and signing algorithm,
 * which are infrastructure concerns.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
interface TokenIssuer {
    fun issue(subject: String): String
}
