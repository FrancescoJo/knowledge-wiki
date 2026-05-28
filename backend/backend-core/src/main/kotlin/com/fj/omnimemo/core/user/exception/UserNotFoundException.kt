/*
 * UserNotFoundException.kt
 *
 * $Since: 2026-05-26T00:00:00Z
 */
package com.fj.omnimemo.core.user.exception

import com.fj.omnimemo.core.exception.OmniMemoErrorCode
import com.fj.omnimemo.core.exception.OmniMemoExternalException
import com.fj.omnimemo.core.user.model.UserId

/**
 * Thrown when an operation targets a [UserId] that does not exist in the repository.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
class UserNotFoundException(id: UserId) : OmniMemoExternalException("User not found: ${id.value}") {
    override val errorCode = OmniMemoErrorCode.USER_NOT_FOUND
}
