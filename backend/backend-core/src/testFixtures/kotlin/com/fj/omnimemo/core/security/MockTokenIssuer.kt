/*
 * MockTokenIssuer.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package com.fj.omnimemo.core.security

/**
 * In-memory fake of [TokenIssuer] for use in Small tests.
 *
 * Prefixes the subject with "token:" so tests can assert on the
 * returned value without running real JWT signing.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
class MockTokenIssuer : TokenIssuer {
    override fun issue(subject: String): String = "token:$subject"
}
