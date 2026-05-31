/*
 * NoteLanguage.kt
 *
 * $Since: 2026-05-30T00:00:00Z
 */
package com.fj.omnimemo.core.note.model

/**
 * Supported note languages. Each language defines its own title-index extraction
 * strategy for directory navigation grouping.
 *
 * Korean: groups by the 14 standard initial consonants (ㄱ-ㅎ).
 * Double consonants (ㄲ ㄸ ㅃ ㅆ ㅉ) are normalised to their unvoiced equivalents.
 * Non-syllable characters fall back to "#".
 *
 * English: groups by the leading letter A-Z (case-insensitive).
 * Non-alphabetic characters fall back to "#".
 *
 * @author Francesco Jo
 * @since 0.2.0
 * @version 0.2.0
 */
enum class NoteLanguage(val code: String) {
    KO("ko") {
        override fun extractTitleIndex(title: String): String {
            val first = title.firstOrNull() ?: return FALLBACK
            val charCode = first.code
            if (charCode !in HANGUL_BASE..HANGUL_END) return FALLBACK
            val consonantIndex = (charCode - HANGUL_BASE) / SYLLABLE_BLOCK_SIZE
            return INITIAL_CONSONANT_MAP[consonantIndex].toString()
        }
    },
    EN("en") {
        override fun extractTitleIndex(title: String): String {
            val first = title.firstOrNull()?.uppercaseChar() ?: return FALLBACK
            return if (first in 'A'..'Z') first.toString() else FALLBACK
        }
    };

    abstract fun extractTitleIndex(title: String): String

    companion object {
        private const val HANGUL_BASE = 0xAC00
        private const val HANGUL_END = 0xD7A3
        private const val SYLLABLE_BLOCK_SIZE = 21 * 28
        private const val FALLBACK = "#"

        // Maps initial consonant index (0-18) to 14 standard groupings.
        // ㄲ→ㄱ, ㄸ→ㄷ, ㅃ→ㅂ, ㅆ→ㅅ, ㅉ→ㅈ
        private val INITIAL_CONSONANT_MAP = charArrayOf(
            'ㄱ', 'ㄱ', 'ㄴ', 'ㄷ', 'ㄷ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅂ',
            'ㅅ', 'ㅅ', 'ㅇ', 'ㅈ', 'ㅈ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
        )

        fun fromCode(code: String): NoteLanguage =
            entries.firstOrNull { it.code == code }
                ?: throw IllegalArgumentException("Unknown language code: $code")
    }
}
