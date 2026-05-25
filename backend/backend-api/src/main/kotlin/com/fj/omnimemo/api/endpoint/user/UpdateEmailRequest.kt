/*
 * UpdateEmailRequest.kt
 *
 * $Since: 2026-05-25T00:00:00Z
 */
package com.fj.omnimemo.api.endpoint.user

/**
 * Request body for PUT /api/users/{id}/email.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
data class UpdateEmailRequest(val email: String)
