/*
 * MarkdownPatchCodecTest.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.infrastructure.note.persistence

import com.fj.omnimemo.core.test.annotation.SmallTest
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

@SmallTest
class MarkdownPatchCodecTest {

    @Test
    fun `applyPatch should reproduce the revised text`() {
        val base = "# Hello\n\nThis is original content."
        val revised = "# Hello\n\nThis is revised content.\n\nNew paragraph added."

        val patch = MarkdownPatchCodec.generatePatch(base, revised)
        val result = MarkdownPatchCodec.applyPatch(base, patch)

        result shouldBe revised
    }

    @Test
    fun `generatePatch should produce empty-ish patch when content is unchanged`() {
        val content = "# No Change\n\nSame content."

        val patch = MarkdownPatchCodec.generatePatch(content, content)
        val result = MarkdownPatchCodec.applyPatch(content, patch)

        result shouldBe content
    }

    @Test
    fun `applyPatch should handle addition of lines`() {
        val base = "line 1\nline 2"
        val revised = "line 1\nline 1.5\nline 2"

        val patch = MarkdownPatchCodec.generatePatch(base, revised)

        MarkdownPatchCodec.applyPatch(base, patch) shouldBe revised
    }

    @Test
    fun `applyPatch should handle deletion of lines`() {
        val base = "line 1\nline 2\nline 3"
        val revised = "line 1\nline 3"

        val patch = MarkdownPatchCodec.generatePatch(base, revised)

        MarkdownPatchCodec.applyPatch(base, patch) shouldBe revised
    }
}
