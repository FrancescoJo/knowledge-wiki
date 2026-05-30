/*
 * ViewControllerSupport.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.view

import com.fj.omnimemo.core.user.model.UserId
import com.fj.omnimemo.core.util.parseUuidOrNull
import org.springframework.security.core.context.SecurityContextHolder

internal fun resolveCurrentUserId(): UserId? {
    val auth = SecurityContextHolder.getContext().authentication ?: return null
    if (!auth.isAuthenticated || auth.principal == "anonymousUser") return null
    return parseUuidOrNull(auth.principal as? String ?: return null)?.let { UserId(it) }
}
