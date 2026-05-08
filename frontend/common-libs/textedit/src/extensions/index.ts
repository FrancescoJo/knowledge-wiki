/**
 * extensions/index.ts
 *
 * $Since: 2026-05-07
 */

import StarterKit from '@tiptap/starter-kit'
import Link from '@tiptap/extension-link'
import Placeholder from '@tiptap/extension-placeholder'
import TableRow from '@tiptap/extension-table-row'
import TaskList from '@tiptap/extension-task-list'
import TaskItem from '@tiptap/extension-task-item'
import Underline from '@tiptap/extension-underline'
import TextStyle from '@tiptap/extension-text-style'
import Color from '@tiptap/extension-color'
import Highlight from '@tiptap/extension-highlight'
import Superscript from '@tiptap/extension-superscript'
import Subscript from '@tiptap/extension-subscript'
import TextAlign from '@tiptap/extension-text-align'

const TextAlignNoShortcuts = TextAlign.extend({ addKeyboardShortcuts: () => ({}) })
import type { Extensions } from '@tiptap/core'
import { ObjectExitCursor } from './object-exit-cursor'
import { BlockDefaultShortcuts } from './block-default-shortcuts'
import { CellSelectionEscape } from './cell-selection-escape'
import { CustomTable, CustomTableCell, CustomTableHeader } from './table-extensions'
import { buildCodeBlockExtension } from './code-block-extension'
import { TableOptionsOverlay } from '../overlays/table-options-overlay'
import { CellOptionsOverlay } from '../overlays/cell-options-overlay'
import { CodeLanguageOverlay } from '../overlays/code-language-overlay'
import { ResizeTableHandle } from '../overlays/resize-table-handle'
import { InsertColumnHandle } from '../overlays/insert-column-handle'
import { InsertRowHandle } from '../overlays/insert-row-handle'
import { ColumnHandle } from '../overlays/column-handle'
import { RowHandle } from '../overlays/row-handle'
import { LinkTooltipOverlay } from '../overlays/link-tooltip-overlay'
import type { CodeLanguage } from '../types'

export { BLOCK_OBJECT_TYPES } from './object-exit-cursor'
export type { BlockObjectType } from './object-exit-cursor'
export { BUILT_IN_LANGUAGES } from './code-block-extension'

const PLACEHOLDER_TEXT = 'Begin writing…'

const TABLE_DEFAULT_ROWS = 2
const TABLE_DEFAULT_COLS = 2

/**
 * Assembles the full set of TipTap extensions used by TextEdit.
 *
 * @param extraLanguages additional syntax-highlighting languages to register
 * @return configured array of TipTap extension instances
 * @since 0.1.0
 * @version 0.1.0
 */
export function buildExtensions(extraLanguages: CodeLanguage[] = []): Extensions {
  const { extension: codeBlockExt, languageItems } = buildCodeBlockExtension(extraLanguages)
  return [
    BlockDefaultShortcuts,
    ObjectExitCursor,
    CellSelectionEscape,
    StarterKit.configure({ codeBlock: false }),
    codeBlockExt,
    Link.configure({ openOnClick: false }),
    Placeholder.configure({ placeholder: PLACEHOLDER_TEXT }),
    CustomTable.configure({ resizable: true }),
    TableRow,
    CustomTableCell,
    CustomTableHeader,
    TaskList,
    TaskItem.configure({ nested: true }),
    Underline,
    TextStyle,
    Color,
    Highlight.configure({ multicolor: true }),
    Superscript,
    Subscript,
    TextAlignNoShortcuts.configure({ types: ['heading', 'paragraph'] }),
    TableOptionsOverlay,
    CellOptionsOverlay,
    CodeLanguageOverlay.configure({ languages: languageItems }),
    ResizeTableHandle,
    InsertColumnHandle,
    InsertRowHandle,
    ColumnHandle,
    RowHandle,
    LinkTooltipOverlay,
  ]
}

export { TABLE_DEFAULT_ROWS, TABLE_DEFAULT_COLS }
