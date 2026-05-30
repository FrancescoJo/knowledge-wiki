/*
 * markdown.ts
 *
 * $Since: 2026-05-31T00:00:00Z
 */

import {marked} from 'marked'

/**
 * Renders a Markdown string to an HTML string using GFM defaults.
 *
 * Configuration must remain identical to textedit's renderMarkdown so that
 * edit-mode (textedit RAW mode) and view-mode (omnimemo note viewer) produce
 * the same output.
 *
 * @param markdown raw Markdown source
 * @return rendered HTML string
 * @since 0.2.0
 */
export function renderMarkdown(markdown: string): string {
  return marked.parse(markdown) as string
}
