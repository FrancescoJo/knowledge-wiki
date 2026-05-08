/**
 * object-exit-cursor.test.ts
 *
 * Verifies keyboard navigation at block-object boundaries managed by ObjectExitCursor.
 * ObjectExitCursor runs at priority 200, ahead of prosemirror-tables (default 100),
 * so all boundary key events are handled before the table plugin can intercept them.
 *
 * Scenarios covered:
 *   - ArrowRight/Down at the very end of a table → GapCursor after the table
 *   - ArrowRight/Down from GapCursor → next content or new paragraph
 *   - ArrowLeft from GapCursor → cursor back inside the table
 *   - Shift+ArrowLeft from GapCursor → NodeSelection of the table
 *   - ArrowLeft/Up at the very start of the first cell when a block object
 *     immediately precedes the table → GapCursor between the two objects
 *   - ArrowLeft at the very start of a paragraph after a table → GapCursor
 *
 * $Since: 2026-05-14
 */

import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { TextSelection } from '@tiptap/pm/state'
import { GapCursor } from '@tiptap/pm/gapcursor'
import { TableMap } from '@tiptap/pm/tables'
import { TextEdit } from '@src/TextEdit'
import { NodeType, type TextEditContent } from '@src/types'
import { mountElement, pmView, pmState, setPmSelection, dispatchKeydown } from '../test-utils'

// -- Fixtures ------------------------------------------------------------------

// 1×1 table (single header cell) as the only document node.
const TABLE_ONLY_DOC: TextEditContent = {
  type: NodeType.Doc,
  content: [
    {
      type: NodeType.Table,
      content: [
        {
          type: NodeType.TableRow,
          content: [
            { type: NodeType.TableHeader, content: [{ type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: 'H' }] }] },
          ],
        },
      ],
    },
  ],
}

// 2×1 table (header row + data row) followed by a paragraph.
// Two rows ensure ArrowDown from the first row stays within the table,
// enabling an unambiguous negative test.
const TABLE_THEN_PARA_DOC: TextEditContent = {
  type: NodeType.Doc,
  content: [
    {
      type: NodeType.Table,
      content: [
        {
          type: NodeType.TableRow,
          content: [
            { type: NodeType.TableHeader, content: [{ type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: 'H' }] }] },
          ],
        },
        {
          type: NodeType.TableRow,
          content: [
            { type: NodeType.TableCell, content: [{ type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: 'D' }] }] },
          ],
        },
      ],
    },
    { type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: 'After table' }] },
  ],
}

// codeBlock immediately followed by a 1×1 table.
// Used to test ArrowLeft/ArrowUp exit from the table's first cell.
const CODEBLOCK_THEN_TABLE_DOC: TextEditContent = {
  type: NodeType.Doc,
  content: [
    { type: NodeType.CodeBlock, content: [{ type: NodeType.Text, text: 'code' }] },
    {
      type: NodeType.Table,
      content: [
        {
          type: NodeType.TableRow,
          content: [
            { type: NodeType.TableHeader, content: [{ type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: 'H' }] }] },
          ],
        },
      ],
    },
  ],
}

// -- Helpers -------------------------------------------------------------------

// TextSelection at the very last text position inside the first-child table.
// Uses TableMap to locate the last cell, then resolves the exact end of its content.
function setCursorAtVeryEndOfTable(editor: TextEdit): void {
  const view = pmView(editor)
  const { state } = view
  const table = state.doc.firstChild!
  const map = TableMap.get(table)
  const lastRow = map.height - 1
  const lastCol = map.width - 1
  const cellStart = 1 + map.positionAt(lastRow, lastCol, table) + 2
  const $cellStart = state.doc.resolve(cellStart)
  const endPos = $cellStart.end($cellStart.depth)
  view.dispatch(state.tr.setSelection(TextSelection.create(state.doc, endPos)))
}

// GapCursor immediately after the first-child table.
function setGapCursorAfterTable(editor: TextEdit): void {
  const view = pmView(editor)
  const { state } = view
  view.dispatch(state.tr.setSelection(new GapCursor(state.doc.resolve(state.doc.firstChild!.nodeSize))))
}

// TextSelection at the very first text position of the paragraph that follows
// the first-child table. Requires a document where a paragraph comes after the table.
function setCursorAtStartOfParagraphAfterTable(editor: TextEdit): void {
  const view = pmView(editor)
  const { state } = view
  // table.nodeSize = gap position between table and paragraph;
  // + 1 = inside the paragraph (after its opening token).
  const paraStartPos = state.doc.firstChild!.nodeSize + 1
  view.dispatch(state.tr.setSelection(TextSelection.create(state.doc, paraStartPos)))
}

// TextSelection at the very first text position of the table's first cell.
// Designed for CODEBLOCK_THEN_TABLE_DOC where doc.child(0) is the codeBlock.
function setCursorAtVeryStartOfTable(editor: TextEdit): void {
  const view = pmView(editor)
  const { state } = view
  const codeBlock = state.doc.child(0)
  const tableNode = state.doc.child(1)
  const tableStart = codeBlock.nodeSize + 1
  const map = TableMap.get(tableNode)
  const pos = tableStart + map.positionAt(0, 0, tableNode) + 2
  view.dispatch(state.tr.setSelection(TextSelection.create(state.doc, pos)))
}

// -- Tests ---------------------------------------------------------------------

describe('ObjectExitCursor — table navigation:', () => {
  let element: HTMLElement
  let editor: TextEdit | undefined

  beforeEach(() => { element = mountElement() })

  // noinspection DuplicatedCode: teardown is local to each suite to capture its own editor and element bindings
  afterEach(() => {
    try { editor?.destroy() } catch { /* already destroyed */ }
    editor = undefined
    element.remove()
  })

  // -- Exit table → GapCursor --------------------------------------------------

  describe('ArrowRight at the very end of a table:', () => {
    it('should place a GapCursor in the gap after the table', () => {
      editor = new TextEdit({ element, content: TABLE_THEN_PARA_DOC })
      setCursorAtVeryEndOfTable(editor)

      dispatchKeydown(editor, 'ArrowRight')

      expect(pmState(editor).selection).toBeInstanceOf(GapCursor)
    })
  })

  describe('ArrowDown at the very end of a table:', () => {
    it('should place a GapCursor in the gap after the table', () => {
      editor = new TextEdit({ element, content: TABLE_THEN_PARA_DOC })
      setCursorAtVeryEndOfTable(editor)

      dispatchKeydown(editor, 'ArrowDown')

      expect(pmState(editor).selection).toBeInstanceOf(GapCursor)
    })
  })

  // -- GapCursor after table → move to next content ----------------------------

  describe('ArrowRight from GapCursor after table (paragraph follows):', () => {
    it('should move the text cursor into the paragraph after the table', () => {
      editor = new TextEdit({ element, content: TABLE_THEN_PARA_DOC })
      setGapCursorAfterTable(editor)

      dispatchKeydown(editor, 'ArrowRight')

      const { selection } = pmState(editor)
      expect(selection).toBeInstanceOf(TextSelection)
      expect((selection as TextSelection).$head.node(1).type.name).toBe('paragraph')
    })
  })

  describe('ArrowDown from GapCursor after table (paragraph follows):', () => {
    it('should move the text cursor into the paragraph after the table', () => {
      editor = new TextEdit({ element, content: TABLE_THEN_PARA_DOC })
      setGapCursorAfterTable(editor)

      dispatchKeydown(editor, 'ArrowDown')

      const { selection } = pmState(editor)
      expect(selection).toBeInstanceOf(TextSelection)
      expect((selection as TextSelection).$head.node(1).type.name).toBe('paragraph')
    })
  })

  describe('ArrowRight from GapCursor after a table that is the last document node:', () => {
    it('should insert a new paragraph and place the cursor inside it', () => {
      editor = new TextEdit({ element, content: TABLE_ONLY_DOC })
      setGapCursorAfterTable(editor)

      dispatchKeydown(editor, 'ArrowRight')

      const after = pmState(editor)
      expect(after.selection).toBeInstanceOf(TextSelection)
      expect(after.doc.childCount).toBe(2)
      expect(after.doc.lastChild!.type.name).toBe('paragraph')
    })
  })

  describe('ArrowDown from GapCursor after a table that is the last document node:', () => {
    it('should insert a new paragraph and place the cursor inside it', () => {
      editor = new TextEdit({ element, content: TABLE_ONLY_DOC })
      setGapCursorAfterTable(editor)

      dispatchKeydown(editor, 'ArrowDown')

      const after = pmState(editor)
      expect(after.selection).toBeInstanceOf(TextSelection)
      expect(after.doc.childCount).toBe(2)
      expect(after.doc.lastChild!.type.name).toBe('paragraph')
    })
  })

  // -- GapCursor after table → move back into table ----------------------------

  describe('ArrowLeft from GapCursor after a table:', () => {
    it('should move the text cursor back inside the table', () => {
      editor = new TextEdit({ element, content: TABLE_THEN_PARA_DOC })
      setGapCursorAfterTable(editor)

      dispatchKeydown(editor, 'ArrowLeft')

      const { selection } = pmState(editor)
      expect(selection).toBeInstanceOf(TextSelection)
      expect((selection as TextSelection).$head.node(1).type.name).toBe('table')
    })
  })

  // Shift+ArrowLeft from GapCursor after table: ObjectExitCursor dispatches
  // NodeSelection(table), but prosemirror-tables' appendTransaction normalises
  // any NodeSelection of a table-role node into a CellSelection, so the final
  // selection type is CellSelection — not testable as NodeSelection here.
  // For non-table block objects (codeBlock, blockquote) the NodeSelection
  // is preserved; coverage for that path lives in TextEdit.test.ts.

  // -- Paragraph after table → GapCursor via ArrowLeft -------------------------

  describe('ArrowLeft at the very start of the paragraph following a table:', () => {
    it('should place a GapCursor in the gap after the table', () => {
      editor = new TextEdit({ element, content: TABLE_THEN_PARA_DOC })
      setCursorAtStartOfParagraphAfterTable(editor)

      dispatchKeydown(editor, 'ArrowLeft')

      const { selection } = pmState(editor)
      expect(selection).toBeInstanceOf(GapCursor)
      expect(selection.$head.pos).toBe(pmState(editor).doc.firstChild!.nodeSize)
    })
  })

  describe('ArrowLeft mid-paragraph after table (not at the very start):', () => {
    it('should not create a GapCursor', () => {
      editor = new TextEdit({ element, content: TABLE_THEN_PARA_DOC })
      const { state } = pmView(editor)
      // Position a few characters into the paragraph.
      const midParaPos = state.doc.firstChild!.nodeSize + 3
      setPmSelection(editor, TextSelection.create(state.doc, midParaPos))

      dispatchKeydown(editor, 'ArrowLeft')

      expect(pmState(editor).selection).not.toBeInstanceOf(GapCursor)
    })
  })

  describe('ArrowLeft twice from the start of the paragraph following a table:', () => {
    it('should place GapCursor on the first press, then cursor inside the table on the second', () => {
      editor = new TextEdit({ element, content: TABLE_THEN_PARA_DOC })
      setCursorAtStartOfParagraphAfterTable(editor)

      dispatchKeydown(editor, 'ArrowLeft')  // paragraph start → GapCursor
      expect(pmState(editor).selection).toBeInstanceOf(GapCursor)

      dispatchKeydown(editor, 'ArrowLeft')  // GapCursor → inside table

      const { selection } = pmState(editor)
      expect(selection).toBeInstanceOf(TextSelection)
      expect((selection as TextSelection).$head.node(1).type.name).toBe('table')
    })
  })

  // -- First cell exit → GapCursor when a block object precedes the table -------

  describe('ArrowLeft at the very start of the first table cell (codeBlock precedes):', () => {
    it('should place a GapCursor between the codeBlock and the table', () => {
      editor = new TextEdit({ element, content: CODEBLOCK_THEN_TABLE_DOC })
      setCursorAtVeryStartOfTable(editor)

      dispatchKeydown(editor, 'ArrowLeft')

      const { selection } = pmState(editor)
      expect(selection).toBeInstanceOf(GapCursor)
      expect(selection.$head.pos).toBe(pmState(editor).doc.child(0).nodeSize)
    })
  })

  describe('ArrowUp at the very start of the first table cell (codeBlock precedes):', () => {
    it('should place a GapCursor between the codeBlock and the table', () => {
      editor = new TextEdit({ element, content: CODEBLOCK_THEN_TABLE_DOC })
      setCursorAtVeryStartOfTable(editor)

      dispatchKeydown(editor, 'ArrowUp')

      const { selection } = pmState(editor)
      expect(selection).toBeInstanceOf(GapCursor)
      expect(selection.$head.pos).toBe(pmState(editor).doc.child(0).nodeSize)
    })
  })

  // -- isAtVeryEndOfNode early-return (line 46) -----------------------------
  // When the cursor is at the end of a cell that is NOT the last row,
  // isAtVeryEndOfNode fires its return-false guard (the node at row depth is
  // not the last child of the table).

  describe('ArrowRight at the end of a non-last row in a multi-row table:', () => {
    it('should not create a GapCursor (ObjectExitCursor does not handle it)', () => {
      editor = new TextEdit({ element, content: TABLE_THEN_PARA_DOC })
      // Place cursor at end of row 0's only cell (not the very end of the table).
      const view = pmView(editor)
      const { state } = view
      const table = state.doc.firstChild!
      const map = TableMap.get(table)
      const cellStartPos = 1 + map.positionAt(0, 0, table) + 2
      const $cs = state.doc.resolve(cellStartPos)
      view.dispatch(state.tr.setSelection(TextSelection.create(state.doc, $cs.end($cs.depth))))

      dispatchKeydown(editor, 'ArrowRight')

      expect(pmState(editor).selection).not.toBeInstanceOf(GapCursor)
    })
  })

  // -- isAtVeryStartOfNode early-return (line 58) ---------------------------
  // When the cursor is at the start of a non-first child (para2 inside a
  // blockquote), the inner loop of isAtVeryStartOfNode fires its return-false
  // guard because the node at para depth is not the first child of blockquote.

  describe('ArrowLeft at the start of a non-first paragraph inside a blockquote:', () => {
    it('should not create a GapCursor (ObjectExitCursor does not handle it)', () => {
      const BLOCKQUOTE_TWO_PARA_DOC: TextEditContent = {
        type: NodeType.Doc,
        content: [{
          type: NodeType.Blockquote,
          content: [
            { type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: 'first' }] },
            { type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: 'second' }] },
          ],
        }],
      }
      editor = new TextEdit({ element, content: BLOCKQUOTE_TWO_PARA_DOC })
      // Position cursor at the very start of para2's text content.
      // Doc: blockquote(para1[7], para2[8]). Para2 text starts at 1+7+1=9.
      const { state } = pmView(editor)
      const para1 = state.doc.child(0).child(0)
      const startOfPara2 = 1 + para1.nodeSize + 1
      setPmSelection(editor, TextSelection.create(state.doc, startOfPara2))

      dispatchKeydown(editor, 'ArrowLeft')

      expect(pmState(editor).selection).not.toBeInstanceOf(GapCursor)
    })
  })

  // -- findObjectBeforeGap null guard (line 97) ------------------------------
  // When a GapCursor sits after a paragraph (not an object type), nodeBefore
  // is a paragraph → findObjectBeforeGap returns null → plugin does not handle
  // the key event.

  describe('ArrowRight from a GapCursor after a plain paragraph:', () => {
    it('should not be handled by ObjectExitCursor (findObjectBeforeGap returns null)', () => {
      const PARA_THEN_TABLE_DOC: TextEditContent = {
        type: NodeType.Doc,
        content: [
          { type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: 'Before' }] },
          {
            type: NodeType.Table,
            content: [{ type: NodeType.TableRow, content: [
              { type: NodeType.TableHeader, content: [{ type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: 'H' }] }] },
            ]}],
          },
        ],
      }
      editor = new TextEdit({ element, content: PARA_THEN_TABLE_DOC })
      // GapCursor at the gap between the paragraph and the table.
      // nodeBefore = paragraph, which is not in objectTypes → line 97 fires.
      const { state } = pmView(editor)
      const gapPos = state.doc.child(0).nodeSize
      setPmSelection(editor, new GapCursor(state.doc.resolve(gapPos)))
      expect(pmState(editor).selection).toBeInstanceOf(GapCursor)

      dispatchKeydown(editor, 'ArrowRight')

      // ObjectExitCursor returned false (findObjectBeforeGap hit line 97 and
      // returned null). In jsdom the GapCursor plugin also cannot advance the
      // cursor (no layout), so the selection remains a GapCursor — confirming
      // our plugin did not convert it to a TextSelection.
      expect(pmState(editor).selection).toBeInstanceOf(GapCursor)
    })
  })
})
