/**
 * test-utils.ts
 *
 * Shared helpers used across multiple test files.
 *
 * $Since: 2026-05-18
 */

import {NodeSelection, TextSelection} from '@tiptap/pm/state'
import {GapCursor} from '@tiptap/pm/gapcursor'
import {TextEdit} from '@src/TextEdit'
import {NodeType, type TextEditContent} from '@src/types'
import {CellSelection, TableMap} from '@tiptap/pm/tables'

// -- Test data -----------------------------------------------------------------

export const EMPTY_DOC: TextEditContent = {
  type: NodeType.Doc,
  content: [{type: NodeType.Paragraph}],
}

export const PARAGRAPH_DOC: TextEditContent = {
  type: NodeType.Doc,
  content: [{
    type: NodeType.Paragraph,
    content: [{type: NodeType.Text, text: 'Hello, world.'}],
  }],
}

export const HEADING_2_DOC: TextEditContent = {
  type: NodeType.Doc,
  content: [{
    type: NodeType.Heading,
    attrs: {level: 2},
    content: [{type: NodeType.Text, text: 'Section heading'}],
  }],
}

export const CODEBLOCK_DOC: TextEditContent = {
  type: NodeType.Doc,
  content: [
    {
      type: NodeType.CodeBlock,
      attrs: {language: null},
      content: [{type: NodeType.Text, text: 'const x = 1'}],
    },
    {
      type: NodeType.Paragraph,
      content: [{type: NodeType.Text, text: 'After code'}],
    },
  ],
}

export const BLOCKQUOTE_DOC: TextEditContent = {
  type: NodeType.Doc,
  content: [
    {
      type: NodeType.Blockquote,
      content: [{type: NodeType.Paragraph, content: [{type: NodeType.Text, text: 'Quoted text'}]}],
    },
    {
      type: NodeType.Paragraph,
      content: [{type: NodeType.Text, text: 'After quote'}],
    },
  ],
}

const TABLE_CONTENT = {
  type: NodeType.Table,
  content: [
    {
      type: NodeType.TableRow,
      content: [
        {
          type: NodeType.TableHeader,
          content: [{type: NodeType.Paragraph, content: [{type: NodeType.Text, text: 'A'}]}]
        },
        {
          type: NodeType.TableHeader,
          content: [{type: NodeType.Paragraph, content: [{type: NodeType.Text, text: 'B'}]}]
        },
      ],
    },
    {
      type: NodeType.TableRow,
      content: [
        {type: NodeType.TableCell, content: [{type: NodeType.Paragraph, content: [{type: NodeType.Text, text: '1'}]}]},
        {type: NodeType.TableCell, content: [{type: NodeType.Paragraph, content: [{type: NodeType.Text, text: '2'}]}]},
      ],
    },
  ],
}

export const TABLE_DOC: TextEditContent = {
  type: NodeType.Doc,
  content: [TABLE_CONTENT],
}

export const PARA_DOC: TextEditContent = {
  type: NodeType.Doc,
  content: [{type: NodeType.Paragraph, content: [{type: NodeType.Text, text: 'Hello'}]}],
}

// Table first so setCursorInCell works; paragraph appended so the cursor can
// be moved outside the table without leaving the document.
export const TABLE_AND_PARA_DOC: TextEditContent = {
  type: NodeType.Doc,
  content: [
    TABLE_CONTENT,
    {type: NodeType.Paragraph, content: [{type: NodeType.Text, text: 'Hi'}]},
  ],
}

export const TWO_TABLE_DOC: TextEditContent = {
  type: NodeType.Doc,
  content: [
    {
      type: NodeType.Table,
      content: [{
        type: NodeType.TableRow,
        content: [
          {
            type: NodeType.TableHeader,
            content: [{type: NodeType.Paragraph, content: [{type: NodeType.Text, text: 'A'}]}]
          },
        ],
      }],
    },
    {
      type: NodeType.Table,
      content: [{
        type: NodeType.TableRow,
        content: [
          {
            type: NodeType.TableCell,
            content: [{type: NodeType.Paragraph, content: [{type: NodeType.Text, text: '1'}]}]
          },
        ],
      }],
    },
  ],
}

// colspan=2 header — map.map contains the header offset twice; the second
// occurrence fires the visited.has(offset) guard in distributeColumns.
export const COLSPAN_TABLE_DOC: TextEditContent = {
  type: NodeType.Doc,
  content: [{
    type: NodeType.Table,
    content: [
      {
        type: NodeType.TableRow,
        content: [{
          type: NodeType.TableHeader,
          attrs: {colspan: 2, rowspan: 1, colwidth: null},
          content: [{type: NodeType.Paragraph, content: [{type: NodeType.Text, text: 'AB'}]}],
        }],
      },
      {
        type: NodeType.TableRow,
        content: [
          {
            type: NodeType.TableCell,
            content: [{type: NodeType.Paragraph, content: [{type: NodeType.Text, text: '1'}]}]
          },
          {
            type: NodeType.TableCell,
            content: [{type: NodeType.Paragraph, content: [{type: NodeType.Text, text: '2'}]}]
          },
        ],
      },
    ],
  }],
}

// -- Helpers -------------------------------------------------------------------

export function proseMirrorEl(host: HTMLElement): Element | null {
  return host.querySelector('.ProseMirror')
}

export function isEditable(host: HTMLElement): boolean {
  return proseMirrorEl(host)?.getAttribute('contenteditable') === 'true'
}

export function docNodes(editor: TextEdit): Record<string, unknown>[] {
  return (editor.getContent().content as Record<string, unknown>[]) ?? []
}

export function tableRows(editor: TextEdit): Record<string, unknown>[] {
  const table = docNodes(editor).find(n => n['type'] === 'table') as Record<string, unknown>
  return (table?.['content'] as Record<string, unknown>[]) ?? []
}

export function rowCells(editor: TextEdit, rowIndex: number): Record<string, unknown>[] {
  const row = tableRows(editor)[rowIndex] as Record<string, unknown>
  return (row?.['content'] as Record<string, unknown>[]) ?? []
}

export function mountElement(): HTMLElement {
  const el = document.createElement('div')
  document.body.appendChild(el)
  return el
}

// Creates a CellSelection spanning cells (r0,c0) to (r1,c1).
// Anchor/head positions are immediately before each cell's opening token:
//   tableStart(=1) + map.positionAt(r, c, table)
export function setCellSelection(editor: TextEdit, r0: number, c0: number, r1: number, c1: number): void {
  const view = pmView(editor)
  const {state} = view
  const tableNode = state.doc.firstChild!
  const map = TableMap.get(tableNode)
  const anchor = 1 + map.positionAt(r0, c0, tableNode)
  const head = 1 + map.positionAt(r1, c1, tableNode)
  view.dispatch(state.tr.setSelection(CellSelection.create(state.doc, anchor, head)))
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export function pmView(editor: TextEdit) {
  return (editor as any).editor.view
}

export function pmState(editor: TextEdit) {
  return pmView(editor).state
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export function getDoc(editor: TextEdit) {
  return editor.getContent() as any
}

export function setPmSelection(editor: TextEdit, sel: TextSelection | GapCursor | NodeSelection): void {
  const view = pmView(editor)
  view.dispatch(view.state.tr.setSelection(sel))
}

export function dispatchKeydown(editor: TextEdit, key: string, shiftKey = false): void {
  pmView(editor).dom.dispatchEvent(
    new KeyboardEvent('keydown', {key, shiftKey, bubbles: true, cancelable: true}),
  )
}

export function setCursorInCell(editor: TextEdit, row: number, col: number): void {
  const view = pmView(editor)
  const {state} = view
  const tableNode = state.doc.firstChild!
  const map = TableMap.get(tableNode)
  const pos = 1 + map.positionAt(row, col, tableNode) + 2
  view.dispatch(state.tr.setSelection(TextSelection.create(state.doc, pos)))
}

export function setFixedColumnWidths(editor: TextEdit, fixed: boolean): void {
  const view = pmView(editor)
  const {state} = view
  view.dispatch(state.tr.setNodeMarkup(0, null, {...state.doc.firstChild!.attrs, fixedColumnWidths: fixed}))
}

export function rect(x: number, y: number, w: number, h: number): DOMRect {
  return {
    left: x, top: y, right: x + w, bottom: y + h, width: w, height: h, x, y, toJSON() {
      return this
    }
  } as DOMRect
}

export function setTextSelectedAtCell0_0(editor: TextEdit): number {
  setCursorInCell(editor, 0, 0)
  const rowsBefore = getDoc(editor).content[0].content.length
  const view = pmView(editor)
  view.dispatch(view.state.tr.setSelection(TextSelection.atEnd(view.state.doc)))

  return rowsBefore
}
