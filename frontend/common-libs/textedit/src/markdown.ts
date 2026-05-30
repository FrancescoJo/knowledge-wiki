/**
 * markdown.ts
 *
 * $Since: 2026-05-31
 */

import {marked} from 'marked'

/**
 * Renders a Markdown string to an HTML string using GFM defaults.
 *
 * This function is the single source of truth for Markdown rendering within
 * the textedit package. The omnimemo viewer uses an identical configuration
 * so that edit-mode and view-mode output are always the same.
 *
 * @param markdown raw Markdown source
 * @return rendered HTML string
 * @since 0.2.0
 */
export function renderMarkdown(markdown: string): string {
  return marked.parse(markdown) as string
}
