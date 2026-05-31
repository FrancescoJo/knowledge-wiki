/**
 * resize-table-handle.test.ts
 *
 * $Since: 2026-05-09
 */

import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'
import {TextEdit} from '@src/TextEdit'
import {NodeType, type TextEditContent} from '@src/types'
import {getDoc, mountElement, PARA_DOC, pmView, setFixedColumnWidths, TABLE_DOC, TWO_TABLE_DOC} from '../test-utils'

// Table whose cells already carry explicit colwidth — simulates the state after
// prosemirror-tables' column-drag or after the Distribute button has been used.
// updateColumnsOnResize will compute table.style.width = 300px (150+150) from
// these attrs, which our fix must override when tableWidth is set.
const TABLE_DOC_WITH_COLWIDTH: TextEditContent = {
  type: NodeType.Doc,
  content: [{
    type: NodeType.Table,
    content: [
      {
        type: NodeType.TableRow,
        content: [
          {
            type: NodeType.TableHeader,
            attrs: {colwidth: [150]},
            content: [{type: NodeType.Paragraph, content: [{type: NodeType.Text, text: 'A'}]}]
          },
          {
            type: NodeType.TableHeader,
            attrs: {colwidth: [150]},
            content: [{type: NodeType.Paragraph, content: [{type: NodeType.Text, text: 'B'}]}]
          },
        ],
      },
      {
        type: NodeType.TableRow,
        content: [
          {
            type: NodeType.TableCell,
            attrs: {colwidth: [150]},
            content: [{type: NodeType.Paragraph, content: [{type: NodeType.Text, text: '1'}]}]
          },
          {
            type: NodeType.TableCell,
            attrs: {colwidth: [150]},
            content: [{type: NodeType.Paragraph, content: [{type: NodeType.Text, text: '2'}]}]
          },
        ],
      },
    ],
  }],
}

// -- Helpers -------------------------------------------------------------------

// The handle is appended to document.body (position: fixed) — query body directly.
function handle(): HTMLElement | null {
  return document.body.querySelector<HTMLElement>('.te-resize-table-handle')
}

// Returns the actual <table> element inside the first table's wrapper div.
function tableElement(editor: TextEdit): HTMLTableElement | null {
  const wrapperDom = pmView(editor).nodeDOM(0)
  return wrapperDom instanceof HTMLElement ? wrapperDom.querySelector('table') : null
}

// Dispatch a mousemove on the first table cell so it bubbles to the document and
// triggers the handle's hover logic.  In jsdom, getBoundingClientRect().right === 0;
// clientX 0 satisfies 0 >= right(0) – HOVER_THRESHOLD(6) = –6, so hover fires.
function hoverOverTableRightEdge(editor: TextEdit): void {
  const view = pmView(editor)
  const cellDom = view.dom.querySelector('td, th')!
  cellDom.dispatchEvent(new MouseEvent('mousemove', {bubbles: true, cancelable: true, clientX: 0}))
}

function simulateDrag(handleEl: HTMLElement, deltaX: number): void {
  handleEl.dispatchEvent(new MouseEvent('mousedown', {bubbles: true, cancelable: true, clientX: 0}))
  document.dispatchEvent(new MouseEvent('mousemove', {bubbles: true, clientX: deltaX}))
  document.dispatchEvent(new MouseEvent('mouseup', {bubbles: true, clientX: deltaX}))
}

function expectSecondTableIndex1(editor: TextEdit) {
  const view = pmView(editor)
  const tables = view.dom.querySelectorAll('table')
  const secondCell = tables[1].querySelector('td, th')!
  // Hover near right edge of the second table (clientX=0 satisfies right(0)−6=−6).
  secondCell.dispatchEvent(new MouseEvent('mousemove', {bubbles: true, cancelable: true, clientX: 0}))
  simulateDrag(handle()!, 200)
  // Second table is at content index 1.
  expect(getDoc(editor).content[1].attrs?.tableWidth).toBe(200)
}

// -- Tests ---------------------------------------------------------------------

describe('ResizeTableHandle:', () => {
  let element: HTMLElement
  let editor: TextEdit | undefined

  beforeEach(() => {
    element = mountElement()
    // jsdom does not implement elementFromPoint; stub it so the columnResizing
    // plugin does not crash when a non-fixed-width, non-right-edge mousemove fires.
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    ;(document as any).elementFromPoint = () => null
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
  })

  // -- mount -----------------------------------------------------------------

  describe('when TextEdit is created:', () => {
    it('should add the handle element to the DOM', () => {
      editor = new TextEdit({element})
      expect(handle()).not.toBeNull()
    })
  })

  // -- visibility ------------------------------------------------------------

  describe('visibility:', () => {
    describe('when the document contains no table:', () => {
      it('should be hidden', () => {
        editor = new TextEdit({element, content: PARA_DOC})
        expect(handle()!.hidden).toBe(true)
      })
    })

    describe('when the mouse hovers near the right edge of a table:', () => {
      it('should become visible', () => {
        editor = new TextEdit({element, content: TABLE_DOC})
        hoverOverTableRightEdge(editor)
        expect(handle()!.hidden).toBe(false)
      })
    })

    describe('when the mouse is not near the right edge of a table:', () => {
      it('should remain hidden', () => {
        editor = new TextEdit({element, content: TABLE_DOC})
        const view = pmView(editor)
        const cellDom = view.dom.querySelector('td, th')!
        // clientX –100: in jsdom right=0, so –100 < 0–6=–6 → not near right edge
        cellDom.dispatchEvent(new MouseEvent('mousemove', {bubbles: true, cancelable: true, clientX: -100}))
        expect(handle()!.hidden).toBe(true)
      })
    })
  })

  // -- resize drag -----------------------------------------------------------

  describe('resize drag:', () => {
    describe('when dragged right by 200px:', () => {
      it('should set tableWidth on the table node', () => {
        editor = new TextEdit({element, content: TABLE_DOC})
        hoverOverTableRightEdge(editor)
        simulateDrag(handle()!, 200)
        // startWidth=0(jsdom), delta=200 → max(80,200)=200
        expect(getDoc(editor).content[0].attrs?.tableWidth).toBe(200)
      })
    })

    describe('when dragged left past the minimum width:', () => {
      it('should clamp tableWidth to the minimum', () => {
        editor = new TextEdit({element, content: TABLE_DOC})
        hoverOverTableRightEdge(editor)
        simulateDrag(handle()!, -500)
        // startWidth=0, delta=–500 → max(80,–500)=80
        expect(getDoc(editor).content[0].attrs?.tableWidth).toBe(80)
      })
    })

    describe('when the text cursor is not inside the table:', () => {
      it('should still commit tableWidth after drag', () => {
        // The handle now uses mouse-hover position to identify the table, so the
        // text cursor does not need to be inside the table for drag to work.
        editor = new TextEdit({element, content: TABLE_DOC})
        hoverOverTableRightEdge(editor)
        simulateDrag(handle()!, 200)
        expect(getDoc(editor).content[0].attrs?.tableWidth).toBe(200)
      })
    })

    describe('when there are multiple intermediate moves before mouseup:', () => {
      it('should commit the width at the mouseup position', () => {
        editor = new TextEdit({element, content: TABLE_DOC})
        hoverOverTableRightEdge(editor)
        const h = handle()!
        h.dispatchEvent(new MouseEvent('mousedown', {bubbles: true, cancelable: true, clientX: 0}))
        document.dispatchEvent(new MouseEvent('mousemove', {bubbles: true, clientX: 100}))
        document.dispatchEvent(new MouseEvent('mousemove', {bubbles: true, clientX: 130}))
        document.dispatchEvent(new MouseEvent('mouseup', {bubbles: true, clientX: 150}))
        // startWidth=0(jsdom), final delta=150 → max(80,150)=150
        expect(getDoc(editor).content[0].attrs?.tableWidth).toBe(150)
      })
    })

    describe('when fixedColumnWidths is toggled on then off before drag:', () => {
      it('should still set tableWidth after drag', () => {
        editor = new TextEdit({element, content: TABLE_DOC})
        setFixedColumnWidths(editor, true)
        setFixedColumnWidths(editor, false)
        hoverOverTableRightEdge(editor)
        simulateDrag(handle()!, 200)
        expect(getDoc(editor).content[0].attrs?.tableWidth).toBe(200)
      })
    })

    describe('when cells have explicit colwidth (e.g. after Distribute or column drag):', () => {
      it('should set table.style.width to tableWidth, overriding prosemirror-tables column width', () => {
        // prosemirror-tables computes table.style.width = 300px (150+150) from
        // colwidth attrs and sets it directly on the <table> element. Without a
        // fix, our decoration on the wrapper div has no effect because the table's
        // own inline style takes precedence.
        editor = new TextEdit({element, content: TABLE_DOC_WITH_COLWIDTH})
        hoverOverTableRightEdge(editor)
        simulateDrag(handle()!, 200)
        // startWidth=0(jsdom), delta=200 → max(80,200)=200
        expect(getDoc(editor).content[0].attrs?.tableWidth).toBe(200)
        expect(tableElement(editor)?.style.width).toBe('200px')
      })
    })
  })

  // -- two-table document ----------------------------------------------------
  // Covers findTablePos line 174: the descendants scan visits the FIRST table,
  // finds it doesn't contain the second table's DOM, returns (line 174), then
  // continues to the SECOND table which matches.

  describe('when a document has two tables and the second is resized:', () => {
    it('should set tableWidth on the second table', () => {
      editor = new TextEdit({element, content: TWO_TABLE_DOC})

      expectSecondTableIndex1(editor)
    })
  })

  // -- three-node document (descendants early-exit guard) -------------------
  // The paragraph that follows table2 causes findTablePos to visit it AFTER
  // pos is already set. At that point `pos !== null` → the early-exit guard
  // (line 168) fires, returning false to skip the paragraph's children.

  describe('when a document has two tables followed by a paragraph and the second is resized:', () => {
    it('should set tableWidth on the second table', () => {
      const THREE_NODE_DOC: TextEditContent = {
        type: NodeType.Doc,
        content: [
          {
            type: NodeType.Table,
            content: [{
              type: NodeType.TableRow, content: [
                {
                  type: NodeType.TableHeader,
                  content: [{type: NodeType.Paragraph, content: [{type: NodeType.Text, text: 'A'}]}]
                },
              ]
            }],
          },
          {
            type: NodeType.Table,
            content: [{
              type: NodeType.TableRow, content: [
                {
                  type: NodeType.TableCell,
                  content: [{type: NodeType.Paragraph, content: [{type: NodeType.Text, text: '1'}]}]
                },
              ]
            }],
          },
          {type: NodeType.Paragraph, content: [{type: NodeType.Text, text: 'after'}]},
        ],
      }

      editor = new TextEdit({element, content: THREE_NODE_DOC})

      expectSecondTableIndex1(editor)
    })
  })

  // -- handleHover: self-target guard (line 74) -----------------------------
  // When the document mousemove target IS the handle element itself, the
  // handler returns early so hoveredTableDom is preserved and the handle
  // stays visible during a drag grab.

  describe('when mousemove fires on the handle element itself:', () => {
    it('should preserve the current hoveredTableDom and not hide the handle', () => {
      editor = new TextEdit({element, content: TABLE_DOC})
      hoverOverTableRightEdge(editor)
      // Handle is now visible.
      expect(handle()!.hidden).toBe(false)
      // Mousemove dispatched directly on the handle bubbles to document;
      // target === this.dom → guard returns early, state unchanged.
      handle()!.dispatchEvent(new MouseEvent('mousemove', {bubbles: true, cancelable: true, clientX: 0}))
      expect(handle()!.hidden).toBe(false)
    })
  })

  // -- findTablePos null guard (line 136) -----------------------------------
  // findTablePos returns null when the hovered table is no longer in the
  // ProseMirror document. Replacing the content after a hover leaves
  // hoveredTableDom set to a stale DOM reference; mousedown must not crash.

  describe('when the hovered table is replaced by new content before mousedown:', () => {
    it('should not start a drag when findTablePos returns null', () => {
      editor = new TextEdit({element, content: TABLE_DOC})
      hoverOverTableRightEdge(editor)
      // Replace document content — table is removed from ProseMirror state.
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      ;(editor as any).editor.commands.setContent({
        type: NodeType.Doc, content: [{type: NodeType.Paragraph, content: [{type: NodeType.Text, text: 'Hi'}]}],
      })
      // hoveredTableDom still points to the old table DOM; findTablePos returns null.
      handle()!.dispatchEvent(new MouseEvent('mousedown', {bubbles: true, cancelable: true, clientX: 0}))
      document.dispatchEvent(new MouseEvent('mousemove', {bubbles: true, clientX: 200}))
      document.dispatchEvent(new MouseEvent('mouseup', {bubbles: true, clientX: 200}))
      // No drag → no tableWidth on the (now) paragraph document.
      expect(getDoc(editor).content[0].type).toBe(NodeType.Paragraph)
    })
  })

  // -- no-hover mousedown guard (line 133) ----------------------------------
  // If mousedown fires on the handle before any hover has been established,
  // hoveredTableDom is null and the handler returns early without starting a
  // drag.

  describe('when mousedown fires on the handle before any hover:', () => {
    it('should not start a drag (hoveredTableDom is null)', () => {
      editor = new TextEdit({element, content: TABLE_DOC})
      // No mousemove to set hoveredTableDom.
      handle()!.dispatchEvent(new MouseEvent('mousedown', {bubbles: true, cancelable: true, clientX: 0}))
      document.dispatchEvent(new MouseEvent('mousemove', {bubbles: true, clientX: 200}))
      document.dispatchEvent(new MouseEvent('mouseup', {bubbles: true, clientX: 200}))
      // Handler returned early at line 133 → no tableWidth applied.
      expect(getDoc(editor).content[0].attrs?.tableWidth).toBeNull()
    })
  })

  // -- stale drag guard (lines 144, 150) -------------------------------------
  // A second mousedown before a mouseup registers a second (onMove2, onUp2)
  // pair. When the first mouseup fires it sets this.resizing = false and
  // removes onMove1/onUp1. The stale onMove2 and onUp2 then see
  // !this.resizing === true and return early (lines 144, 150).

  describe('when a second mousedown fires before the first mouseup:', () => {
    it('should ignore the stale mousemove and mouseup from the second drag', () => {
      editor = new TextEdit({element, content: TABLE_DOC})
      hoverOverTableRightEdge(editor)
      const h = handle()!
      // First drag starts.
      h.dispatchEvent(new MouseEvent('mousedown', {bubbles: true, cancelable: true, clientX: 0}))
      // Second drag starts before first ends (registers stale onMove/onUp).
      h.dispatchEvent(new MouseEvent('mousedown', {bubbles: true, cancelable: true, clientX: 0}))
      // First drag ends at delta=200 → tableWidth=200 committed.
      document.dispatchEvent(new MouseEvent('mouseup', {bubbles: true, clientX: 200}))
      // Stale onMove2: !this.resizing → early return (line 144).
      document.dispatchEvent(new MouseEvent('mousemove', {bubbles: true, clientX: 250}))
      // Stale onUp2: !this.resizing → early return (line 150); no second commit.
      document.dispatchEvent(new MouseEvent('mouseup', {bubbles: true, clientX: 250}))
      // Only the first drag's width (200) was committed; stale events changed nothing.
      expect(getDoc(editor).content[0].attrs?.tableWidth).toBe(200)
    })
  })

  // -- syncTableWidths: nodeDOM non-HTMLElement guard (line 115) ------------
  // When a table has a tableWidth attribute but nodeDOM returns a non-HTMLElement,
  // the guard on line 115 fires and width sync is skipped for that table.

  describe('when syncTableWidths encounters a nodeDOM that is not an HTMLElement:', () => {
    it('should skip width sync for that table without crashing', () => {
      editor = new TextEdit({element, content: TABLE_DOC})
      // Set tableWidth so syncTableWidths will call nodeDOM for the table node.
      // Use commands (not chain + focus) so only one transaction fires and the mock
      // is not consumed by a preceding focus() transaction.
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      ;(editor as any).editor.commands.updateAttributes('table', {tableWidth: 400})

      // Mock nodeDOM to return null (permanent mock so the one transaction below
      // reliably hits line 115 regardless of any intermediate dispatch order).
      const view = pmView(editor)
      const spy = vi.spyOn(view, 'nodeDOM').mockReturnValue(null)

        // A single updateAttributes transaction triggers update() → syncTableWidths
        // → nodeDOM(tablePos) → null → line 115 guard fires.
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
      ;(editor as any).editor.commands.updateAttributes('table', {tableWidth: 401})

      spy.mockRestore()
      expect(pmView(editor).state.doc.firstChild!.attrs['tableWidth']).toBe(401)
    })
  })

  // -- applyWidth: stale tablePos guard (line 185) ---------------------------
  // When the table is replaced between drag start (mousedown) and drag end
  // (mouseup), the closure-captured tablePos no longer points to a table node.
  // applyWidth's guard on line 185 fires and no update is committed.

  describe('when the table is replaced between drag start and drag end:', () => {
    it('should return early in applyWidth and leave the document unchanged', () => {
      editor = new TextEdit({element, content: TABLE_DOC})
      hoverOverTableRightEdge(editor)

      // Start the drag — findTablePos captures the current tablePos in the closure.
      handle()!.dispatchEvent(new MouseEvent('mousedown', {bubbles: true, cancelable: true, clientX: 0}))

      // Replace the table with a paragraph.  tablePos now points to the paragraph.
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      ;(editor as any).editor.commands.setContent({
        type: NodeType.Doc, content: [{type: NodeType.Paragraph, content: [{type: NodeType.Text, text: 'Hi'}]}],
      })

      // Mouseup fires onUp → applyWidth(tablePos, …) → nodeAt(tablePos) returns
      // the paragraph node → type.name !== 'table' → line 185 fires → no-op.
      document.dispatchEvent(new MouseEvent('mouseup', {bubbles: true, clientX: 200}))

      expect(getDoc(editor).content[0].type).toBe(NodeType.Paragraph)
    })
  })

  // -- applyWidth catch block (line 194) ------------------------------------
  // When dispatch throws inside applyWidth, the catch block absorbs the error
  // and the document remains unchanged.

  describe('when dispatch throws inside applyWidth:', () => {
    it('should silently absorb the error', () => {
      editor = new TextEdit({element, content: TABLE_DOC})
      hoverOverTableRightEdge(editor)
      handle()!.dispatchEvent(new MouseEvent('mousedown', {bubbles: true, cancelable: true, clientX: 0}))
      // Mock dispatch to throw once — fires when applyWidth dispatches on mouseup.
      vi.spyOn(pmView(editor), 'dispatch').mockImplementationOnce(() => {
        throw new Error('mock')
      })
      document.dispatchEvent(new MouseEvent('mouseup', {bubbles: true, clientX: 200}))
      vi.restoreAllMocks()
      // Catch absorbed the error; table is unchanged.
      expect(getDoc(editor).content[0].type).toBe(NodeType.Table)
    })
  })

  // -- destroy ---------------------------------------------------------------

  describe('when the editor is destroyed:', () => {
    it('should remove the handle from the DOM', () => {
      editor = new TextEdit({element})
      editor.destroy()
      editor = undefined
      expect(handle()).toBeNull()
    })
  })
})
