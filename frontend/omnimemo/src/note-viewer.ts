/*
 * note-viewer.ts
 *
 * $Since: 2026-05-31T00:00:00Z
 */

import {renderMarkdown} from '@src/markdown'

/**
 * Initialises the Markdown viewer on note pages.
 *
 * Looks for `#note-body` in the document. If found, reads the raw Markdown
 * from `#note-editor-content` (the edit textarea, always present in the DOM)
 * and renders it into `#note-body` as HTML.
 *
 * No-ops on pages that do not contain the note viewer structure.
 *
 * @since 0.2.0
 */
export function initNoteViewer(): void {
  const noteBody = document.getElementById('note-body')
  if (noteBody === null) return

  const textarea = document.getElementById('note-editor-content') as HTMLTextAreaElement | null
  if (textarea === null) return

  noteBody.innerHTML = renderMarkdown(textarea.value)
}
