/*
 * RefreshTokenNotFoundException.kt
 *
 * $Since: 2026-05-26T00:00:00Z
 */
package com.fj.omnimemo.core.user.exception

import com.fj.omnimemo.core.exception.OmniMemoExternalException

/**
 * Thrown when the provided refresh token does not exist in the store.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
class RefreshTokenNotFoundException : OmniMemoExternalException("Refresh token not found")
