/*
 * TokenExpiredException.kt
 *
 * $Since: 2026-05-26T00:00:00Z
 */
package com.fj.omnimemo.core.user.exception

import com.fj.omnimemo.core.exception.OmniMemoExternalException

/**
 * Thrown when the provided token exists but has passed its expiry time.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
class TokenExpiredException : OmniMemoExternalException("Token has expired")
