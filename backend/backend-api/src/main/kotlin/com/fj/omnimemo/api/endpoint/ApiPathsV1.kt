/*
 * ApiPathsV1.kt
 *
 * $Since: 2026-05-26T00:00:00Z
 */
package com.fj.omnimemo.api.endpoint

import com.fj.omnimemo.api.endpoint.ApiPathsV1.BASE

/**
 * Compile-time URL path constants for API version 1.
 *
 * All REST API endpoints are prefixed with [BASE]. Use these constants in
 * `@RequestMapping` and related annotations to avoid scattering raw strings
 * across controller interfaces.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
object ApiPathsV1 {
    const val BASE = "/api/v1"
    const val USERS = "$BASE/users"
    const val NOTES = "$BASE/notes"
    const val AUTH = "$BASE/auth"
    const val HEALTH = "$BASE/health"
    const val BOOTSTRAP = "$BASE/bootstrap"
    const val BOOTSTRAP_USERS = "$BOOTSTRAP/users"
}
