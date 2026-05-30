/**
 * table-extensions.test.ts
 *
 * Tests for CustomTable, CustomTableCell, and CustomTableHeader extensions.
 * Currently covers the fixedColumnWidths column-resize blocker.
 *
 * When fixedColumnWidths is true the table's data-fixed-column-widths attribute
 * is set. A ProseMirror plugin intercepts mousemove events over such tables and
 * returns true (consumed), which prevents the columnResizing plugin from
 * detecting column separators and activating its drag handler.
 *
 * jsdom does not implement document.elementFromPoint; we mock it so the
 * columnResizing plugin (which calls posAtCoords → elementFromPoint) does not
 * crash during the non-fixed-width test case.
 *
 * $Since: 2026-05-14
 */

import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'
import {TextEdit} from '@src/TextEdit'
import {mountElement, pmView, setCursorInCell, setFixedColumnWidths, TABLE_DOC, TWO_TABLE_DOC} from '../test-utils'

// -- Helpers -------------------------------------------------------------------

// Dispatch a mousemove event on the first cell inside the editor and return it.
function moveMouseOverCell(editor: TextEdit): MouseEvent {
  const view = pmView(editor)
  const cellDom = view.dom.querySelector('td, th')!
  const event = new MouseEvent('mousemove', {bubbles: true, cancelable: true})
  cellDom.dispatchEvent(event)
  return event
}

// Dispatch a mousemove at a specific clientX on the first cell and return it.
function moveMouseAt(editor: TextEdit, clientX: number): MouseEvent {
  const view = pmView(editor)
  const cellDom = view.dom.querySelector('td, th')!
  const event = new MouseEvent('mousemove', {bubbles: true, cancelable: true, clientX})
  cellDom.dispatchEvent(event)
  return event
}

// -- Tests ---------------------------------------------------------------------

describe('CustomTable:', () => {
  let element: HTMLElement
  let editor: TextEdit | undefined

  // noinspection DuplicatedCode: setup is identical across blocker suites; each suite captures its own editor and element bindings
  beforeEach(() => {
    element = mountElement()
    // jsdom does not implement elementFromPoint; stub it so the columnResizing
    // plugin does not crash when a non-fixed-width table receives mousemove.
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    ;(document as any).elementFromPoint = () => null
    // Give every element a consistent bounding rect so right-edge detection
    // (which compares event.clientX against tableDom.getBoundingClientRect().right)
    // works in jsdom where layout is absent.
    vi.spyOn(HTMLElement.prototype, 'getBoundingClientRect').mockReturnValue({
      left: 0, top: 0, right: 300, bottom: 100, width: 300, height: 100,
      x: 0, y: 0, toJSON: () => ({}),
    } as DOMRect)
  })

  // noinspection DuplicatedCode: teardown is local to each suite to capture its own editor and element bindings
  afterEach(() => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    delete (document as any).elementFromPoint
    try {
      editor?.destroy()
    } catch { /* already destroyed */
    }
    editor = undefined
    element.remove()
    vi.restoreAllMocks()
  })

  // -- Fixed widths active ---------------------------------------------------
  describe('fixed-widths column-resize blocker:', () => {
    describe('when fixedColumnWidths is true:', () => {
      it('should set fixedColumnWidths in the ProseMirror node state', () => {
        editor = new TextEdit({element, content: TABLE_DOC})
        setCursorInCell(editor, 0, 0)
        setFixedColumnWidths(editor, true)

        const view = pmView(editor)
        expect(view.state.doc.firstChild!.attrs['fixedColumnWidths']).toBe(true)
      })

      it('should consume mousemove events so the column-resize plugin cannot activate', () => {
        editor = new TextEdit({element, content: TABLE_DOC})
        setCursorInCell(editor, 0, 0)
        setFixedColumnWidths(editor, true)

        const event = moveMouseOverCell(editor)
        // ProseMirror calls event.preventDefault() when a handleDOMEvents handler
        // returns true, so defaultPrevented signals the event was consumed.
        expect(event.defaultPrevented).toBe(true)
      })
    })
  })

  // -- Fixed widths inactive -------------------------------------------------
  describe('when fixedColumnWidths is false:', () => {
    it('should leave fixedColumnWidths false in the ProseMirror node state', () => {
      editor = new TextEdit({element, content: TABLE_DOC})
      setCursorInCell(editor, 0, 0)

      const view = pmView(editor)
      expect(view.state.doc.firstChild!.attrs['fixedColumnWidths']).toBe(false)
    })

    it('should not consume mousemove events so the column-resize plugin can activate', () => {
      editor = new TextEdit({element, content: TABLE_DOC})
      setCursorInCell(editor, 0, 0)

      // clientX=0 is well inside the mocked table (right=300), so the right-edge
      // blocker does not activate and fixedColumnWidths=false leaves events free.
      const event = moveMouseOverCell(editor)
      expect(event.defaultPrevented).toBe(false)
    })
  })

  // getBoundingClientRect is mocked to return right=300 in beforeEach above.
  // RIGHT_EDGE_THRESHOLD is 6, so the blocker activates for clientX >= 294.
  describe('right-edge column-resize blocker:', () => {
    // -- Near right edge --------------------------------------------------------

    describe('when fixedColumnWidths is false and the cursor is within the right-edge threshold:', () => {
      it('should consume the mousemove event so the column-resize plugin cannot activate', () => {
        editor = new TextEdit({element, content: TABLE_DOC})
        setCursorInCell(editor, 0, 0)
        // clientX=295 >= right(300) - threshold(6) = 294 → blocker fires
        const event = moveMouseAt(editor, 295)
        expect(event.defaultPrevented).toBe(true)
      })
    })

    describe('when fixedColumnWidths is true and the cursor is within the right-edge threshold:', () => {
      it('should consume the mousemove event', () => {
        editor = new TextEdit({element, content: TABLE_DOC})
        setCursorInCell(editor, 0, 0)
        setFixedColumnWidths(editor, true)
        const event = moveMouseAt(editor, 295)
        expect(event.defaultPrevented).toBe(true)
      })
    })

    // -- Away from right edge ---------------------------------------------------

    describe('when fixedColumnWidths is false and the cursor is away from the right edge:', () => {
      it('should not consume the mousemove event', () => {
        editor = new TextEdit({element, content: TABLE_DOC})
        setCursorInCell(editor, 0, 0)
        // clientX=100 < 294 → right-edge blocker does not fire
        const event = moveMouseAt(editor, 100)
        expect(event.defaultPrevented).toBe(false)
      })
    })
  })

// -- CustomTable — multi-table descendants scan --------------------------------
  describe('descendants scan with multiple tables:', () => {

    describe('when the cursor hovers over a cell in the second table:', () => {
      it('should not consume the event when fixedColumnWidths is false on that table', () => {
        editor = new TextEdit({element, content: TWO_TABLE_DOC})
        setCursorInCell(editor, 0, 0)

        const view = pmView(editor)
        // Query the second table's cell so the plugin checks both tables in the
        // descendants scan — the first table is skipped (line 49) and the second
        // is identified as the target.
        const allTables = view.dom.querySelectorAll('table')
        const secondCell = allTables[1].querySelector('td, th')!
        const event = new MouseEvent('mousemove', {bubbles: true, cancelable: true})
        secondCell.dispatchEvent(event)

        expect(event.defaultPrevented).toBe(false)
      })
    })
  })
})

// -- CustomTable — tableWidth attribute ---------------------------------------

describe('CustomTable — tableWidth attribute:', () => {
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

  describe('renderHTML: when tableWidth is set on a table:', () => {
    it('should include the pixel width as an inline style in the rendered HTML', () => {
      editor = new TextEdit({element, content: TABLE_DOC})
      setCursorInCell(editor, 0, 0)
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      ;(editor as any).editor.chain().focus().updateAttributes('table', {tableWidth: 400}).run()
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const html: string = (editor as any).editor.getHTML()
      expect(html).toContain('width: 400px')
    })
  })

  describe('parseHTML: when a table element has style="width: Npx":', () => {
    it('should store the numeric width in the tableWidth attribute', () => {
      editor = new TextEdit({element})
      const html = '<table style="width: 400px"><tbody><tr><th><p>A</p></th></tr></tbody></table>'
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
      ;(editor as any).editor.commands.setContent(html)
      const tableNode = pmView(editor).state.doc.firstChild!
      expect(tableNode.attrs['tableWidth']).toBe(400)
    })
  })

  describe('parseHTML: when a table element has no inline width style:', () => {
    it('should store null in the tableWidth attribute', () => {
      editor = new TextEdit({element})
      const html = '<table><tbody><tr><th><p>A</p></th></tr></tbody></table>'
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
      ;(editor as any).editor.commands.setContent(html)
      const tableNode = pmView(editor).state.doc.firstChild!
      expect(tableNode.attrs['tableWidth']).toBeNull()
    })
  })

  describe('parseHTML: when a table element has data-fixed-column-widths="true":', () => {
    it('should store true in the fixedColumnWidths attribute', () => {
      editor = new TextEdit({element})
      const html = '<table data-fixed-column-widths="true"><tbody><tr><th><p>A</p></th></tr></tbody></table>'
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
      ;(editor as any).editor.commands.setContent(html)
      const tableNode = pmView(editor).state.doc.firstChild!
      expect(tableNode.attrs['fixedColumnWidths']).toBe(true)
    })
  })

  describe('renderHTML: when fixedColumnWidths is true:', () => {
    it('should include data-fixed-column-widths="true" in the rendered HTML', () => {
      editor = new TextEdit({element, content: TABLE_DOC})
      setCursorInCell(editor, 0, 0)
      setFixedColumnWidths(editor, true)
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const html: string = (editor as any).editor.getHTML()
      expect(html).toContain('data-fixed-column-widths="true"')
    })
  })
})
