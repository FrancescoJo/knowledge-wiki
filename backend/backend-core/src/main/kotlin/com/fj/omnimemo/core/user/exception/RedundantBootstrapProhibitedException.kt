/*
 * RedundantBootstrapProhibitedException.kt
 *
 * $Since: 2026-05-26T00:00:00Z
 */
package com.fj.omnimemo.core.user.exception

import com.fj.omnimemo.core.exception.OmniMemoInternalException

/**
 * Thrown when a bootstrap attempt is made but at least one user already exists.
 *
 * Bootstrap is a one-time operation; repeating it is prohibited by domain invariant.
 *
 * @author Francesco Jo
 * @since 0.1.1
 * @version 0.1.1
 */
class RedundantBootstrapProhibitedException :
    OmniMemoInternalException("Bootstrap is prohibited when users already exist")
