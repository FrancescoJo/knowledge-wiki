/*
 * MarkdownPatchCodec.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.infrastructure.note.persistence

import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils

/**
 * Produces and applies unified diff patches for Markdown content.
 *
 * Patches are generated line-by-line in the unified diff format and can be
 * applied back to the original text to reconstruct any delta version.
 *
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
object MarkdownPatchCodec {
    private const val CONTEXT_LINES = 3

    fun generatePatch(base: String, revised: String): String {
        val baseLines = base.lines()
        val revisedLines = revised.lines()
        val patch = DiffUtils.diff(baseLines, revisedLines)
        return UnifiedDiffUtils.generateUnifiedDiff("base", "revised", baseLines, patch, CONTEXT_LINES)
            .joinToString("\n")
    }

    fun applyPatch(base: String, patch: String): String {
        val baseLines = base.lines()
        val patchLines = patch.lines()
        val parsedPatch = UnifiedDiffUtils.parseUnifiedDiff(patchLines)
        return DiffUtils.patch(baseLines, parsedPatch).joinToString("\n")
    }
}
