/*
 * OmniMemoInternalException.kt
 *
 * $Since: 2026-05-26T00:00:00Z
 */
package com.fj.omnimemo.core.exception

/**
 * Base class for domain-logic exceptions that are anticipated and explicitly thrown.
 *
 * Use this when a domain invariant is violated — a condition the domain itself
 * prohibits regardless of any external input.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
abstract class OmniMemoInternalException(message: String) : OmniMemoException(message)
