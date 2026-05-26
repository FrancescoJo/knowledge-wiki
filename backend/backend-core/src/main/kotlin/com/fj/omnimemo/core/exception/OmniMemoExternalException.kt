/*
 * OmniMemoExternalException.kt
 *
 * $Since: 2026-05-26T00:00:00Z
 */
package com.fj.omnimemo.core.exception

/**
 * Base class for exceptions caused by external input or external device failures.
 *
 * Use this when the failure originates outside the domain — invalid credentials,
 * expired tokens, or references to entities that do not exist.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
abstract class OmniMemoExternalException(message: String) : OmniMemoException(message)
