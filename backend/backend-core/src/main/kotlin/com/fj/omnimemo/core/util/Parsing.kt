/*
 * Parsing.kt
 *
 * $Since: 2026-05-26T00:00:00Z
 */
package com.fj.omnimemo.core.util

import java.util.UUID

/**
 * Returns the [UUID] represented by [raw], or `null` if the string is not a valid UUID.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
fun parseUuidOrNull(raw: String): UUID? =
    try { UUID.fromString(raw) }
    catch (e: IllegalArgumentException) { null }
