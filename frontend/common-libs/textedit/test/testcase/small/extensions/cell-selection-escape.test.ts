/**
 * cell-selection-escape.test.ts
 *
 * Verifies that pressing Escape while a CellSelection is active restores
 * the text cursor to the position it occupied immediately before the
 * CellSelection was entered — not to the anchor cell of the CellSelection.
 *
 * This distinction matters for column/row handle clicks, where the anchor is
 * always the first cell in the column/row, whereas the cursor may have been
 * anywhere in the table.
 *
 * Scenarios:
 *   - Cursor in an arbitrary cell → multi-cell CellSelection → ESC
 *     → cursor returns to that original cell (not the CellSelection anchor).
 *   - Column-handle simulation: cursor at (1, 1) → column CellSelection
 *     (anchor at row 0) → ESC → cursor returns to (1, 1).
 *   - Row-handle simulation: cursor at (1, 1) → row CellSelection
 *     (anchor at col 0) → ESC → cursor returns to (1, 1).
 *   - No CellSelection active → ESC does not change the selection.
 *
 * $Since: 2026-05-14
 */

import {afterEach, beforeEach, describe, expect, it} from 'vitest'
import {NodeSelection, TextSelection} from '@tiptap/pm/state'
import {CellSelection, TableMap} from '@tiptap/pm/tables'
import {TextEdit} from '@src/TextEdit'
import {mountElement, pmState, pmView, setCellSelection, setCursorInCell, TABLE_DOC} from '../test-utils'

// -- Helpers -------------------------------------------------------------------

// True when the text cursor is inside the specified cell.
function cursorIsInCell(editor: TextEdit, row: number, col: number): boolean {
  const state = pmState(editor)
  if (!(state.selection instanceof TextSelection)) return false
  const tableNode = state.doc.firstChild!
  const map = TableMap.get(tableNode)
  const cellOffset = map.positionAt(row, col, tableNode)
  const cellNode = tableNode.nodeAt(cellOffset)
  if (!cellNode) return false
  const cellStart = 1 + cellOffset          // absolute start of cell node
  const cellEnd = cellStart + cellNode.nodeSize
  return state.selection.from > cellStart && state.selection.from < cellEnd
}

function pressEscape(editor: TextEdit): void {
  pmView(editor).dom.dispatchEvent(
    new KeyboardEvent('keydown', {key: 'Escape', bubbles: true, cancelable: true}),
  )
}

// -- Tests ---------------------------------------------------------------------

describe('CellSelectionEscape:', () => {
  let element: HTMLElement
  let editor: TextEdit | undefined

  beforeEach(() => {
    element = mountElement()
  })

  // noinspection DuplicatedCode: teardown is local to each suite to capture its own editor and element bindings
  afterEach(() => {
    try {
      editor?.destroy()
    } catch { /* already destroyed */
    }
    editor = undefined
    element.remove()
  })

  // -- Result type -----------------------------------------------------------

  describe('when a CellSelection is active:', () => {
    it('should produce a TextSelection after Escape', () => {
      editor = new TextEdit({element, content: TABLE_DOC})
      setCursorInCell(editor, 1, 1)
      setCellSelection(editor, 0, 0, 1, 1)
      expect(pmState(editor).selection).toBeInstanceOf(CellSelection)

      pressEscape(editor)

      expect(pmState(editor).selection).toBeInstanceOf(TextSelection)
    })
  })

  // -- Cursor restoration ----------------------------------------------------

  describe('column-handle simulation — cursor at (1,1), column CellSelection anchored at (0,1):', () => {
    it('should restore the cursor to cell (1,1), not to the anchor row (0,1)', () => {
      editor = new TextEdit({element, content: TABLE_DOC})
      setCursorInCell(editor, 1, 1)
      // Column handle selects entire column: anchor = top row, head = bottom row
      setCellSelection(editor, 0, 1, 1, 1)

      pressEscape(editor)

      expect(cursorIsInCell(editor, 1, 1)).toBe(true)
    })
  })

  describe('row-handle simulation — cursor at (1,1), row CellSelection anchored at (1,0):', () => {
    it('should restore the cursor to cell (1,1), not to the anchor column (1,0)', () => {
      editor = new TextEdit({element, content: TABLE_DOC})
      setCursorInCell(editor, 1, 1)
      // Row handle selects entire row: anchor = leftmost col, head = rightmost col
      setCellSelection(editor, 1, 0, 1, 1)

      pressEscape(editor)

      expect(cursorIsInCell(editor, 1, 1)).toBe(true)
    })
  })

  describe('drag-selection simulation — cursor at (0,0) before drag, selection spans to (1,1):', () => {
    it('should restore the cursor to cell (0,0) where the drag started', () => {
      editor = new TextEdit({element, content: TABLE_DOC})
      setCursorInCell(editor, 0, 0)
      setCellSelection(editor, 0, 0, 1, 1)

      pressEscape(editor)

      expect(cursorIsInCell(editor, 0, 0)).toBe(true)
    })
  })

  // -- Fallback when savedPos is null ----------------------------------------
  // If a CellSelection is dispatched while the editor is in a non-TextSelection
  // state (e.g. NodeSelection), the plugin never records a savedPos because the
  // TextSelection → CellSelection transition guard is not satisfied.  In that
  // case the ESC handler falls back to $anchorCell.pos + 1 (line 41).

  describe('when a CellSelection is entered from a NodeSelection (savedPos is null):', () => {
    it('should fall back to $anchorCell.pos + 1 and produce a TextSelection', () => {
      editor = new TextEdit({element, content: TABLE_DOC})
      const view = pmView(editor)

      // NodeSelection on the table: plugin processes TextSel → NodeSel as
      // "leaving CellSelection" (not a CellSel) → returns null → state = null.
      view.dispatch(view.state.tr.setSelection(NodeSelection.create(view.state.doc, 0)))

      // CellSelection from NodeSelection: oldState is NOT a TextSelection, so
      // the plugin's recording branch does not fire → state stays null.
      setCellSelection(editor, 0, 0, 1, 1)
      expect(pmState(editor).selection).toBeInstanceOf(CellSelection)

      // Escape: savedPos is null → fallback to $anchorCell.pos + 1 (line 41).
      pressEscape(editor)

      expect(pmState(editor).selection).toBeInstanceOf(TextSelection)
    })
  })

  // -- No-op when not in CellSelection --------------------------------------

  describe('when no CellSelection is active:', () => {
    it('should not change the selection', () => {
      editor = new TextEdit({element, content: TABLE_DOC})
      setCursorInCell(editor, 0, 0)
      const posBefore = pmState(editor).selection.from

      pressEscape(editor)

      expect(pmState(editor).selection.from).toBe(posBefore)
    })
  })
})
