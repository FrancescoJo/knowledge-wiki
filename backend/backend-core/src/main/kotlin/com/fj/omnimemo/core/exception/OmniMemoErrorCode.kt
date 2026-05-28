/*
 * OmniMemoErrorCode.kt
 *
 * $Since: 2026-05-28T00:00:00Z
 */
package com.fj.omnimemo.core.exception

import com.fj.omnimemo.core.user.exception.PasswordMismatchException
import com.fj.omnimemo.core.user.exception.RedundantBootstrapProhibitedException
import com.fj.omnimemo.core.user.exception.RefreshTokenNotFoundException
import com.fj.omnimemo.core.user.exception.TokenExpiredException
import com.fj.omnimemo.core.user.exception.UserNotFoundException
import kotlin.reflect.KClass

/**
 * Canonical registry mapping every [OmniMemoException] subtype to a stable hex error code.
 *
 * The default [code] for each entry is `"0x%08X".format(canonicalName.hashCode())`, derived
 * from the exception class's canonical name, making each code traceable without a lookup table.
 * Individual entries may override [code] when a stable, collision-free value is required.
 * Uniqueness across all entries is verified at class initialisation.
 *
 * HTTP status mapping is intentionally absent here; that policy lives in the API layer.
 *
 * Lookup by exception instance : [of]
 * Lookup by hex code string    : [byCode]
 *
 * @author Francesco Jo
 * @since 0.1.1
 */
enum class OmniMemoErrorCode(val exceptionClass: KClass<out OmniMemoException>) {
    PASSWORD_MISMATCH(PasswordMismatchException::class),
    REFRESH_TOKEN_NOT_FOUND(RefreshTokenNotFoundException::class),
    TOKEN_EXPIRED(TokenExpiredException::class),
    USER_NOT_FOUND(UserNotFoundException::class),
    REDUNDANT_BOOTSTRAP_PROHIBITED(RedundantBootstrapProhibitedException::class),
    ;

    open val code: String
        get() = "0x%08X".format(exceptionClass.java.canonicalName!!.hashCode())

    companion object {
        init {
            val duplicates = entries
                .groupBy { it.code }
                .filter { (_, group) -> group.size > 1 }
                .map { (code, group) -> "$code -> [${group.joinToString { it.name }}]" }
            require(duplicates.isEmpty()) {
                "Duplicate OmniMemoErrorCode.code values detected — hash collision must be resolved manually: $duplicates"
            }
        }

        fun of(exception: OmniMemoException): OmniMemoErrorCode =
            entries.first { it.exceptionClass == exception::class }

        fun byCode(code: String): OmniMemoErrorCode? =
            entries.firstOrNull { it.code == code }
    }
}
