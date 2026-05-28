/*
 * OmniMemoException.kt
 *
 * $Since: 2026-05-26T00:00:00Z
 */
package com.fj.omnimemo.core.exception

/**
 * Sealed root of the OmniMemo domain exception hierarchy.
 *
 * Exists solely as a placeholder type for exhaustive, uniform handling of
 * all domain exceptions at the API layer. Extend [OmniMemoInternalException]
 * or [OmniMemoExternalException] rather than this class directly.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
sealed class OmniMemoException(message: String) : RuntimeException(message) {
    abstract val errorCode: OmniMemoErrorCode
}
