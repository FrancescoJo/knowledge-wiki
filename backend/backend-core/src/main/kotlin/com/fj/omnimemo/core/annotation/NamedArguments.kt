/*
 * NamedArguments.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.core.annotation

/**
 * Marks a function or constructor whose parameter list is intentionally long
 * because all call sites are required to use named-argument syntax.
 *
 * When callers use named arguments, the readability concern behind
 * [LongParameterList][io.gitlab.arturbosch.detekt.rules.complexity.LongParameterList]
 * is mitigated: each argument is self-documenting at the call site, and accidental
 * positional swaps are caught by the compiler.
 *
 * Typical candidates:
 * - Domain `reconstitute()` factory methods that mirror a persistence row
 * - Audit / audit-creation helpers that capture actor context
 * - Use-case methods pending refactoring to a command-object parameter
 *
 * Adding this annotation does **not** enforce named-argument usage at the language
 * level; that is a code-review and convention concern.
 *
 * @author Francesco Jo
 * @since 0.2.0
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CONSTRUCTOR)
@Retention(AnnotationRetention.SOURCE)
annotation class NamedArguments
