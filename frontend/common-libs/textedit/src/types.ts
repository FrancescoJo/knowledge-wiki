/**
 * types.ts
 *
 * $Since: 2026-05-07
 */

/** Serialised document content in TipTap's ProseMirror JSON format. */
export type TextEditContent = Record<string, unknown>

/** Valid heading levels supported by the editor. */
export type HeadingLevel = 1 | 2 | 3 | 4 | 5 | 6

/**
 * Configuration options for initialising a TextEdit instance.
 *
 * @author Hwan Jo
 * @since 0.1.0
 * @version 0.1.0
 */
export interface TextEditOptions {
  /** The DOM element into which the editor will be mounted. */
  element: HTMLElement
  /** Initial document content. Omit to start with an empty document. */
  content?: TextEditContent
  /** Called whenever the document content changes. */
  onChange?: (content: TextEditContent) => void
  /**
   * Called after every editor transaction (cursor move, selection change, formatting toggle).
   * Use this to keep a toolbar's active states in sync with the current selection.
   */
  onSelectionChange?: (editor: TextEditHandle) => void
  /** When true, the editor is non-interactive. Defaults to false. */
  readOnly?: boolean
}

/**
 * Read-only view of a TextEdit instance passed to onSelectionChange callbacks.
 * Allows toolbar code to query active formatting without holding a full reference.
 *
 * @since 0.1.0
 * @version 0.1.0
 */
export interface TextEditHandle {
  /** Returns true when the named mark or node is active at the current selection. */
  isActive(name: string, attributes?: Record<string, unknown>): boolean
}
