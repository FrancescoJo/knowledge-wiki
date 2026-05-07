/**
 * extensions.ts
 *
 * $Since: 2026-05-07
 */

import StarterKit from '@tiptap/starter-kit'
import Link from '@tiptap/extension-link'
import Placeholder from '@tiptap/extension-placeholder'
import Table from '@tiptap/extension-table'
import TableRow from '@tiptap/extension-table-row'
import TableCell from '@tiptap/extension-table-cell'
import TableHeader from '@tiptap/extension-table-header'
import TaskList from '@tiptap/extension-task-list'
import TaskItem from '@tiptap/extension-task-item'
import type { Extensions } from '@tiptap/core'

const PLACEHOLDER_TEXT = 'Begin writing…'

const TABLE_DEFAULT_ROWS = 3
const TABLE_DEFAULT_COLS = 3

/**
 * Assembles the full set of TipTap extensions used by TextEdit.
 *
 * @return configured array of TipTap extension instances
 * @since 0.1.0
 * @version 0.1.0
 */
export function buildExtensions(): Extensions {
  return [
    StarterKit,
    Link.configure({ openOnClick: false }),
    Placeholder.configure({ placeholder: PLACEHOLDER_TEXT }),
    Table.configure({ resizable: true }),
    TableRow,
    TableCell,
    TableHeader,
    TaskList,
    TaskItem.configure({ nested: true }),
  ]
}

export { TABLE_DEFAULT_ROWS, TABLE_DEFAULT_COLS }
