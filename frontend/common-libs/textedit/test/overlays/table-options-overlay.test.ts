/**
 * table-options-overlay.test.ts
 *
 * $Since: 2026-05-09
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { TextSelection } from '@tiptap/pm/state'
import { TableMap } from '@tiptap/pm/tables'
import { TextEdit } from '@src/TextEdit'
import { NodeType, type TextEditContent } from '@src/types'
import { mountElement, pmView, setCursorInCell, TABLE_DOC, PARA_DOC, COLSPAN_TABLE_DOC } from '../test-utils'

// -- Fixtures ------------------------------------------------------------------

// Table followed by a paragraph — lets tests move cursor from table to outside.
const TABLE_THEN_PARA_DOC: TextEditContent = {
  type: NodeType.Doc,
  content: [
    {
      type: NodeType.Table,
      content: [{
        type: NodeType.TableRow,
        content: [
          { type: NodeType.TableCell, content: [{ type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: 'A' }] }] },
        ],
      }],
    },
    { type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: 'outside' }] },
  ],
}

// 3-column table for Distribute tests: tableWidth=600 → 200px per column.
const TABLE_3COL_DOC: TextEditContent = {
    type: NodeType.Doc,
  content: [{
    type: NodeType.Table,
    content: [
      {
        type: NodeType.TableRow,
        content: [
          { type: NodeType.TableHeader, content: [{ type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: 'A' }] }] },
          { type: NodeType.TableHeader, content: [{ type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: 'B' }] }] },
          { type: NodeType.TableHeader, content: [{ type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: 'C' }] }] },
        ],
      },
      {
        type: NodeType.TableRow,
        content: [
          { type: NodeType.TableCell, content: [{ type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: '1' }] }] },
          { type: NodeType.TableCell, content: [{ type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: '2' }] }] },
          { type: NodeType.TableCell, content: [{ type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: '3' }] }] },
        ],
      },
    ],
  }],
}

// -- Helpers -------------------------------------------------------------------

// The overlay is appended to document.body (position: fixed), not inside the
// editor element — query document.body directly.
function overlay(): HTMLElement | null {
  return document.body.querySelector<HTMLElement>('.te-table-options')
}

function overlayBtn(title: string): HTMLButtonElement | null {
  return document.body.querySelector<HTMLButtonElement>(`.te-table-options__btn[title="${title}"]`)
}

function clickBtn(btn: HTMLButtonElement): void {
  btn.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true }))
}

function setTableAttr(editor: TextEdit, attrs: Record<string, unknown>): void {
  const view = pmView(editor)
  const { state } = view
  const table = state.doc.firstChild!
  view.dispatch(state.tr.setNodeMarkup(0, null, { ...table.attrs, ...attrs }))
}

function getCellColwidth(editor: TextEdit, row: number, col: number): number[] | null {
  const view = pmView(editor)
  const { state } = view
  const table = state.doc.firstChild!
  const map = TableMap.get(table)
  const offset = map.map[row * map.width + col]
  const cell = table.nodeAt(offset)
  return cell?.attrs['colwidth'] ?? null
}

// -- Tests ---------------------------------------------------------------------

describe('TableOptionsOverlay:', () => {
  let element: HTMLElement
  let editor: TextEdit | undefined

  beforeEach(() => { element = mountElement() })

  // noinspection DuplicatedCode: teardown is local to each suite to capture its own editor and element bindings
  afterEach(() => {
    try { editor?.destroy() } catch { /* already destroyed */ }
    editor = undefined
    element.remove()
  })

  // -- mount -----------------------------------------------------------------

  describe('when TextEdit is created:', () => {
    it('should add the overlay element to the DOM', () => {
      editor = new TextEdit({ element })
      expect(overlay()).not.toBeNull()
    })
  })

  // -- visibility ------------------------------------------------------------

  describe('visibility:', () => {
    describe('when the cursor is outside a table:', () => {
      it('should be hidden', () => {
        editor = new TextEdit({ element, content: PARA_DOC })
        const el = overlay()!
        expect(el.style.display).toBe('none')
      })
    })

    describe('when the cursor is placed inside a table cell:', () => {
      it('should become visible', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 0, 0)
        const el = overlay()!
        expect(el.style.display).toBe('')
      })
    })

    describe('when the cursor moves from a table cell to outside the table:', () => {
      it('should become hidden', () => {
        editor = new TextEdit({ element, content: TABLE_THEN_PARA_DOC })
        const view = pmView(editor)
        setCursorInCell(editor, 0, 0)
        const el = overlay()!
        expect(el.style.display).toBe('')
        const { state } = view
        const tableNode = state.doc.firstChild!
        view.dispatch(state.tr.setSelection(TextSelection.create(state.doc, tableNode.nodeSize + 1)))
        expect(el.style.display).toBe('none')
      })
    })

    describe('when the editor loses focus:', () => {
      it('should become hidden', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 0, 0)
        const el = overlay()!
        expect(el.style.display).toBe('')
        pmView(editor).dom.dispatchEvent(new Event('blur'))
        expect(el.style.display).toBe('none')
      })
    })
  })

  // -- active states ---------------------------------------------------------

  describe('active states:', () => {
    describe('Header row button:', () => {
      it('should be active when the table has a header row', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 0, 0)
        const btn = overlayBtn('Toggle header row')!
        expect(btn.classList.contains('is-active')).toBe(true)
      })
    })

    describe('Fixed widths button:', () => {
      it('should not be active initially', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 0, 0)
        const btn = overlayBtn('Toggle fixed column widths')!
        expect(btn.classList.contains('is-active')).toBe(false)
      })
    })

  })

  // -- position --------------------------------------------------------------

  describe('position:', () => {
    describe('when cursor is inside a table:', () => {
      it('should be positioned below the table (top >= 0)', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 0, 0)
        const el = overlay()!
        const top = parseFloat(el.style.top)
        expect(isNaN(top)).toBe(false)
        expect(top).toBeGreaterThanOrEqual(0)
      })

      it('should be horizontally centred relative to the editor text area', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        // Give the editor's scroll container a known bounding rect.
        const view = pmView(editor)
        const areaDom = view.dom.parentElement ?? view.dom
        areaDom.getBoundingClientRect = () => ({
          left: 100, right: 700, width: 600,
          top: 0, bottom: 400, height: 400,
          x: 100, y: 0,
          toJSON() { return this },
        } as DOMRect)
        setCursorInCell(editor, 0, 0)
        const el = overlay()!
        // centre = left + width/2 = 100 + 300 = 400
        expect(el.style.left).toBe('400px')
        expect(el.style.transform).toBe('translateX(-50%)')
      })
    })
  })

  // -- Fixed widths button ---------------------------------------------------

  describe('Fixed widths button mousedown:', () => {
    describe('when fixed widths is off:', () => {
      it('should enable fixed column widths on the table', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 0, 0)
        clickBtn(overlayBtn('Toggle fixed column widths')!)
        expect(editor.isTableFixedColumnWidths()).toBe(true)
      })
    })

    describe('when fixed widths is on:', () => {
      it('should disable fixed column widths on the table', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 0, 0)
        editor.setTableFixedColumnWidths(true)
        clickBtn(overlayBtn('Toggle fixed column widths')!)
        expect(editor.isTableFixedColumnWidths()).toBe(false)
      })
    })
  })

  // -- Header row button -----------------------------------------------------

  describe('Header row button mousedown:', () => {
    describe('when the table has a header row:', () => {
      it('should remove the header row', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 0, 0)
        clickBtn(overlayBtn('Toggle header row')!)
        expect(editor.isTableHeaderRow()).toBe(false)
      })
    })
  })

  // -- Header column button --------------------------------------------------

  describe('Header column button mousedown:', () => {
    describe('when the table has no header column:', () => {
      it('should add a header column', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 0, 0)
        clickBtn(overlayBtn('Toggle header column')!)
        expect(editor.isTableHeaderColumn()).toBe(true)
      })
    })
  })

  // -- Overlay container mousedown -------------------------------------------
  // When the Distribute button is disabled its CSS sets pointer-events: none,
  // so the click passes through to the overlay container. The container's
  // mousedown handler must call ev.preventDefault() to prevent the editor
  // from losing focus (which would hide the overlay).

  describe('overlay container mousedown:', () => {
    it('should call ev.preventDefault() to keep editor focus', () => {
      editor = new TextEdit({ element, content: TABLE_3COL_DOC })
      setCursorInCell(editor, 0, 0)
      const ev = new MouseEvent('mousedown', { bubbles: true, cancelable: true })
      overlay()!.dispatchEvent(ev)
      expect(ev.defaultPrevented).toBe(true)
    })

    it('should keep the overlay visible when mousedown fires on the container', () => {
      editor = new TextEdit({ element, content: TABLE_3COL_DOC })
      setCursorInCell(editor, 0, 0)
      expect(overlay()!.style.display).toBe('')
      overlay()!.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true }))
      expect(overlay()!.style.display).toBe('')
    })
  })

  // -- Distribute button -----------------------------------------------------

  describe('Distribute button:', () => {
    describe('when fixedColumnWidths is true (set via direct state):', () => {
      it('should be disabled', () => {
        editor = new TextEdit({ element, content: TABLE_3COL_DOC })
        setCursorInCell(editor, 0, 0)
        setTableAttr(editor, { fixedColumnWidths: true })
        expect(overlayBtn('Distribute columns evenly')!.disabled).toBe(true)
      })

      it('should not modify colwidths when clicked', () => {
        editor = new TextEdit({ element, content: TABLE_3COL_DOC })
        setCursorInCell(editor, 0, 0)
        setTableAttr(editor, { fixedColumnWidths: true, tableWidth: 600 })
        const before = getCellColwidth(editor, 0, 0)
        clickBtn(overlayBtn('Distribute columns evenly')!)
        expect(getCellColwidth(editor, 0, 0)).toEqual(before)
      })
    })

    describe('when the Fixed widths overlay button is clicked to enable fixed widths:', () => {
      it('should disable the Distribute button', () => {
        editor = new TextEdit({ element, content: TABLE_3COL_DOC })
        setCursorInCell(editor, 0, 0)
        // Simulate clicking the Fixed widths button in the overlay (goes through the
        // TipTap command chain, not direct ProseMirror state manipulation).
        clickBtn(overlayBtn('Toggle fixed column widths')!)
        expect(overlayBtn('Distribute columns evenly')!.disabled).toBe(true)
      })

      it('should re-enable the Distribute button when Fixed widths is toggled off again', () => {
        editor = new TextEdit({ element, content: TABLE_3COL_DOC })
        setCursorInCell(editor, 0, 0)
        clickBtn(overlayBtn('Toggle fixed column widths')!)   // on
        clickBtn(overlayBtn('Toggle fixed column widths')!)   // off
        expect(overlayBtn('Distribute columns evenly')!.disabled).toBe(false)
      })
    })

    describe('when fixedColumnWidths is false:', () => {
      it('should be enabled', () => {
        editor = new TextEdit({ element, content: TABLE_3COL_DOC })
        setCursorInCell(editor, 0, 0)
        setTableAttr(editor, { fixedColumnWidths: false })
        expect(overlayBtn('Distribute columns evenly')!.disabled).toBe(false)
      })
    })

    describe('when tableWidth is set and Distribute is clicked:', () => {
      it('should set equal colwidth on every header cell', () => {
        editor = new TextEdit({ element, content: TABLE_3COL_DOC })
        setCursorInCell(editor, 0, 0)
        setTableAttr(editor, { tableWidth: 600 })
        // noinspection DuplicatedCode: different scenario
        clickBtn(overlayBtn('Distribute columns evenly')!)
        // 600 / 3 columns = 200px each
        expect(getCellColwidth(editor, 0, 0)).toEqual([200])
        expect(getCellColwidth(editor, 0, 1)).toEqual([200])
        expect(getCellColwidth(editor, 0, 2)).toEqual([200])
      })

      it('should set equal colwidth on every body cell', () => {
        editor = new TextEdit({ element, content: TABLE_3COL_DOC })
        setCursorInCell(editor, 0, 0)
        setTableAttr(editor, { tableWidth: 600 })
        clickBtn(overlayBtn('Distribute columns evenly')!)
        expect(getCellColwidth(editor, 1, 0)).toEqual([200])
        expect(getCellColwidth(editor, 1, 1)).toEqual([200])
        expect(getCellColwidth(editor, 1, 2)).toEqual([200])
      })

      it('should round the computed width when tableWidth is not evenly divisible', () => {
        editor = new TextEdit({ element, content: TABLE_3COL_DOC })
        setCursorInCell(editor, 0, 0)
        // 601 / 3 = 200.33... → Math.round → 200
        setTableAttr(editor, { tableWidth: 601 })
        clickBtn(overlayBtn('Distribute columns evenly')!)
        expect(getCellColwidth(editor, 0, 0)).toEqual([200])
      })
    })

    describe('when tableWidth is null but the rendered table has a measured width:', () => {
      it('should distribute evenly based on the DOM-measured table width', () => {
        editor = new TextEdit({ element, content: TABLE_3COL_DOC })
        setCursorInCell(editor, 0, 0)
        // jsdom returns offsetWidth=0 by default; stub it to simulate a real browser.
        const view = pmView(editor)
        const tableNode = view.state.doc.firstChild!
        const map = TableMap.get(tableNode)
        const tableStart = 1 // table at pos 0, so content starts at pos 1
        const firstCellDom = view.nodeDOM(tableStart + map.positionAt(0, 0, tableNode)) as HTMLElement
        const tableDom = firstCellDom.closest('table')!
        Object.defineProperty(tableDom, 'offsetWidth', { configurable: true, value: 600 })
        // noinspection DuplicatedCode: different scenario
        clickBtn(overlayBtn('Distribute columns evenly')!)
        // 600 / 3 columns = 200px each
        expect(getCellColwidth(editor, 0, 0)).toEqual([200])
        expect(getCellColwidth(editor, 0, 1)).toEqual([200])
        expect(getCellColwidth(editor, 0, 2)).toEqual([200])
      })
    })

    describe('when tableWidth is null and the table has no measurable rendered width:', () => {
      it('should not modify colwidths (jsdom returns offsetWidth=0)', () => {
        editor = new TextEdit({ element, content: TABLE_3COL_DOC })
        setCursorInCell(editor, 0, 0)
        // tableWidth attr is null; jsdom offsetWidth is 0 → distributeColumns returns early.
        const before = getCellColwidth(editor, 0, 0)
        clickBtn(overlayBtn('Distribute columns evenly')!)
        expect(getCellColwidth(editor, 0, 0)).toEqual(before)
      })
    })

    describe('when the table has a colspan cell and tableWidth is set:', () => {
      it('should skip duplicate map offsets via the visited set and distribute width evenly', () => {
        editor = new TextEdit({ element, content: COLSPAN_TABLE_DOC })
        setCursorInCell(editor, 0, 0)
        setTableAttr(editor, { tableWidth: 400 })
        clickBtn(overlayBtn('Distribute columns evenly')!)
        // colspan=2 header: equalColWidth=200 (400÷2) → colwidth=[200,200]
        expect(getCellColwidth(editor, 0, 0)).toEqual([200, 200])
      })
    })

    describe('when nodeDOM returns a non-HTMLElement in the else branch:', () => {
      it('should not modify colwidths when firstCellDom is not an HTMLElement', () => {
        editor = new TextEdit({ element, content: TABLE_3COL_DOC })
        setCursorInCell(editor, 0, 0)
        const before = getCellColwidth(editor, 0, 0)
        // tableWidth is null → else branch; mock nodeDOM to return a TextNode so
        // firstCellDom instanceof HTMLElement is false → tableDom = null → early return.
        vi.spyOn(pmView(editor), 'nodeDOM').mockReturnValueOnce(document.createTextNode('x'))
        clickBtn(overlayBtn('Distribute columns evenly')!)
        vi.restoreAllMocks()
        expect(getCellColwidth(editor, 0, 0)).toEqual(before)
      })
    })

    describe('when the cursor moves outside the table before Distribute is clicked:', () => {
      it('should return early without modifying the table', () => {
        editor = new TextEdit({ element, content: TABLE_THEN_PARA_DOC })
        const view = pmView(editor)
        setCursorInCell(editor, 0, 0)
        const before = getCellColwidth(editor, 0, 0)
        // Move cursor to the trailing paragraph — overlay hides, but button stays in DOM.
        const tableNode = view.state.doc.firstChild!
        view.dispatch(view.state.tr.setSelection(
          TextSelection.create(view.state.doc, tableNode.nodeSize + 1),
        ))
        // distributeColumns fires → !isInTable → early return.
        clickBtn(overlayBtn('Distribute columns evenly')!)
        expect(getCellColwidth(editor, 0, 0)).toEqual(before)
      })
    })
  })

  // -- Align buttons ---------------------------------------------------------

  describe('Align left button mousedown:', () => {
    it('should apply left text alignment to the focused paragraph', () => {
      editor = new TextEdit({ element, content: TABLE_DOC })
      setCursorInCell(editor, 0, 0)
      editor.setTextAlign('right')   // set a non-left alignment first
      clickBtn(overlayBtn('Align left')!)
      expect(editor.isActive({ textAlign: 'left' })).toBe(true)
    })
  })

  describe('Align centre button mousedown:', () => {
    it('should apply centre text alignment to the focused paragraph', () => {
      editor = new TextEdit({ element, content: TABLE_DOC })
      setCursorInCell(editor, 0, 0)
      clickBtn(overlayBtn('Align centre')!)
      expect(editor.isActive({ textAlign: 'center' })).toBe(true)
    })
  })

  describe('Align right button mousedown:', () => {
    it('should apply right text alignment to the focused paragraph', () => {
      editor = new TextEdit({ element, content: TABLE_DOC })
      setCursorInCell(editor, 0, 0)
      clickBtn(overlayBtn('Align right')!)
      expect(editor.isActive({ textAlign: 'right' })).toBe(true)
    })
  })

  // -- destroy ---------------------------------------------------------------

  describe('when the editor is destroyed:', () => {
    it('should remove the overlay from the DOM', () => {
      editor = new TextEdit({ element })
      editor.destroy()
      editor = undefined
      expect(overlay()).toBeNull()
    })
  })
})
