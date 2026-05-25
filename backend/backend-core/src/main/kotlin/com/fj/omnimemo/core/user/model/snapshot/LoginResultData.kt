/*
 * LoginResultData.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package com.fj.omnimemo.core.user.model.snapshot

import com.fj.omnimemo.core.user.model.LoginResult

/**
 * Immutable snapshot of a [LoginResult].
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
internal data class LoginResultData(
    override val accessToken: String,
    override val refreshToken: String,
) : LoginResult
