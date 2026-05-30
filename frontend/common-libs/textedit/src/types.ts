/**
 * types.ts
 *
 * $Since: 2026-05-07
 */

import type {LanguageFn} from 'highlight.js'

/**
 * A syntax-highlighting language available in the code-block language picker.
 *
 * Built-in languages (Markdown, JavaScript, TypeScript) are registered
 * automatically. Pass extra entries via {@link TextEditOptions.codeLanguages}
 * to register additional languages.
 *
 * @since 0.2.0
 */
export interface CodeLanguage {
  /** Display name shown in the language picker (e.g. 'Python'). */
  label: string
  /** Language identifier used by lowlight (e.g. 'python'). */
  value: string
  /** Grammar function. Import the default export from 'highlight.js/lib/languages/<name>'. */
  grammar: LanguageFn
}

/** Serialised document content in TipTap's ProseMirror JSON format. */
export type TextEditContent = Record<string, unknown>

/** Node type names used in TipTap's ProseMirror JSON format. */
export const NodeType = {
  Doc: 'doc',
  Paragraph: 'paragraph',
  Heading: 'heading',
  Text: 'text',
  BulletList: 'bulletList',
  OrderedList: 'orderedList',
  ListItem: 'listItem',
  TaskList: 'taskList',
  TaskItem: 'taskItem',
  Blockquote: 'blockquote',
  CodeBlock: 'codeBlock',
  Table: 'table',
  TableRow: 'tableRow',
  TableHeader: 'tableHeader',
  TableCell: 'tableCell',
} as const

/** Mark type names used in TipTap's ProseMirror JSON format. */
export const MarkType = {
  Bold: 'bold',
  Italic: 'italic',
  Strike: 'strike',
  Underline: 'underline',
  Code: 'code',
  TextStyle: 'textStyle',
  Highlight: 'highlight',
  Superscript: 'superscript',
  Subscript: 'subscript',
  Link: 'link',
} as const

/** Valid heading levels supported by the editor. */
export type HeadingLevel = 1 | 2 | 3 | 4 | 5 | 6

/** Text alignment options for block-level nodes. */
export type TextAlignment = 'left' | 'centre' | 'right'

/** Options accepted by {@link TextEditHandle.insertTable}. */
export interface InsertTableOptions {
  /** Total number of rows (including the header row when withHeaderRow is true). Defaults to 2. */
  rows?: number
  /** Number of columns. Defaults to 2. */
  cols?: number
  /** When true the first row is rendered as a header row. Defaults to true. */
  withHeaderRow?: boolean
}

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
  /**
   * Additional syntax-highlighting languages to register in the code-block
   * language picker, beyond the built-in set (Markdown, JavaScript, TypeScript).
   */
  codeLanguages?: CodeLanguage[]
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

  /** Returns true when any mark or node matching the given attributes is active. */
  isActive(attributes: Record<string, unknown>): boolean

  /**
   * Returns true when the document has been modified since the editor was created.
   * Compares the current document against its initial state using a content hash.
   */
  isDirty(): boolean

  /**
   * Returns true when a GapCursor is positioned immediately after a block object
   * (codeBlock, table, or blockquote). Use this to reflect the boundary state in
   * toolbar or status indicators.
   */
  isAtObjectBoundary(): boolean

  // -- Table state queries ------------------------------------------------------

  /** Returns true when the cursor is inside a table cell. */
  isInTable(): boolean

  /** Returns true when the selection spans multiple cells that can be merged. */
  canMergeCells(): boolean

  /** Returns true when the cursor is in a merged cell that can be split. */
  canSplitCell(): boolean

  /** Returns true when the current table has fixed column widths enabled. */
  isTableFixedColumnWidths(): boolean

  /** Returns true when the first row of the current table is a header row. */
  isTableHeaderRow(): boolean

  /** Returns true when the first column of the current table is a header column. */
  isTableHeaderColumn(): boolean
}
