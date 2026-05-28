/*
 * PasswordMismatchException.kt
 *
 * $Since: 2026-05-26T00:00:00Z
 */
package com.fj.omnimemo.core.user.exception

import com.fj.omnimemo.core.exception.OmniMemoErrorCode
import com.fj.omnimemo.core.exception.OmniMemoExternalException

/**
 * Thrown when login fails due to invalid credentials.
 *
 * The message is intentionally generic — it covers both "email not found" and
 * "password wrong" to prevent user enumeration via distinct error messages.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
class PasswordMismatchException : OmniMemoExternalException("Invalid credentials") {
    override val errorCode = OmniMemoErrorCode.PASSWORD_MISMATCH
}
