/**
 * column-handle.test.ts
 *
 * $Since: 2026-05-09
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { TextSelection } from '@tiptap/pm/state'
import { CellSelection, TableMap } from '@tiptap/pm/tables'
import { TextEdit } from '@src/TextEdit'
import { NodeType, type TextEditContent } from '@src/types'
import {mountElement, pmView, getDoc, setCursorInCell, PARA_DOC, rect, setTextSelectedAtCell0_0} from '../test-utils'

// -- Fixtures ------------------------------------------------------------------

const TABLE_CONTENT = {
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
        { type: NodeType.TableCell, content: [{ type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: 'cherry' }] }] },
        { type: NodeType.TableCell, content: [{ type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: '2' }] }] },
        { type: NodeType.TableCell, content: [{ type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: 'x' }] }] },
      ],
    },
    {
      type: NodeType.TableRow,
      content: [
        { type: NodeType.TableCell, content: [{ type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: 'apple' }] }] },
        { type: NodeType.TableCell, content: [{ type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: '1' }] }] },
        { type: NodeType.TableCell, content: [{ type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: 'y' }] }] },
      ],
    },
  ],
}

const TABLE_DOC: TextEditContent = {
  type: NodeType.Doc,
  content: [TABLE_CONTENT],
}

// Table followed by a paragraph — lets the cursor move outside the table.
const TABLE_AND_PARA_DOC: TextEditContent = {
  type: NodeType.Doc,
  content: [
    TABLE_CONTENT,
    { type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: 'Para' }] },
  ],
}

// -- Helpers -------------------------------------------------------------------

// All overlay elements are appended to document.body (position: fixed) —
// query document.body directly rather than the editor mount element.
function handle(): HTMLElement | null {
  return document.body.querySelector<HTMLElement>('.te-col-handle')
}

function menu(): HTMLElement | null {
  return document.body.querySelector<HTMLElement>('.te-col-handle__menu')
}

function menuItem(label: string): HTMLButtonElement | null {
  const items = document.body.querySelectorAll<HTMLButtonElement>('.te-col-handle__item')
  for (const item of items) {
    if (item.textContent?.trim() === label) return item
  }
  return null
}

function clickHandle(): void {
  handle()?.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true, clientX: 0 }))
  // No significant movement → menu opens on mouseup
  document.dispatchEvent(new MouseEvent('mouseup', { bubbles: true, clientX: 0 }))
}

function clickItem(label: string): void {
  menuItem(label)?.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true }))
}

function picker(): HTMLElement | null {
  return document.body.querySelector<HTMLElement>('.te-colour-input--col')
}

// -- Tests ---------------------------------------------------------------------

describe('ColumnHandle:', () => {
  let element: HTMLElement
  let editor: TextEdit | undefined

  beforeEach(() => { element = mountElement() })

  afterEach(() => {
    try { editor?.destroy() } catch { /* already destroyed */ }
    editor = undefined
    element.remove()
  })

  // Creates a TABLE_DOC editor, moves the cursor to column 0 row 0, and opens the column menu.
  function setupCol0(): TextEdit {
    const ed = new TextEdit({ element, content: TABLE_DOC })
    setCursorInCell(ed, 0, 0)
    clickHandle()
    return ed
  }

  // Creates a TABLE_DOC editor, moves the cursor to column 1 row 0, and opens the column menu.
  function setupCol1(): TextEdit {
    const ed = new TextEdit({ element, content: TABLE_DOC })
    setCursorInCell(ed, 0, 1)
    clickHandle()
    return ed
  }

  // Creates a TABLE_DOC editor, moves the cursor to column 0 row 1, and opens the column menu.
  function setupRow1(): TextEdit {
    const ed = new TextEdit({ element, content: TABLE_DOC })
    setCursorInCell(ed, 1, 0)
    clickHandle()
    return ed
  }

  // Creates a TABLE_DOC editor, moves to column 0 row 0, and starts a drag
  // (mousedown at clientX=100 + mousemove at clientX=110).
  function startDragFromCol0(): TextEdit {
    const ed = new TextEdit({ element, content: TABLE_DOC })
    setCursorInCell(ed, 0, 0)
    handle()?.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true, clientX: 100 }))
    document.dispatchEvent(new MouseEvent('mousemove', { bubbles: true, clientX: 110 }))
    return ed
  }

  // -- mount -----------------------------------------------------------------

  // noinspection DuplicatedCode: code looks same but using different HTMLElement
  describe('when TextEdit is created:', () => {
    it('should add the handle element to the DOM', () => {
      editor = new TextEdit({ element })
      expect(handle()).not.toBeNull()
    })
  })

  // -- visibility ------------------------------------------------------------

  describe('visibility:', () => {
    describe('when the cursor is outside a table:', () => {
      it('should be hidden', () => {
        editor = new TextEdit({ element, content: PARA_DOC })
        expect(handle()!.style.display).toBe('none')
      })
    })

    describe('when the cursor is placed inside a table cell:', () => {
      it('should become visible', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 0, 0)
        expect(handle()!.style.display).toBe('')
      })
    })
  })

  // -- menu open / close -----------------------------------------------------

  describe('handle click:', () => {
    it('should open the context menu', () => {
      editor = setupCol0()
      expect(menu()!.hidden).toBe(false)
    })

    it('should select the entire column (CellSelection)', () => {
      editor = setupRow1()
      const { selection } = pmView(editor).state
      expect(selection instanceof CellSelection).toBe(true)
    })
  })

  describe('Escape key:', () => {
    describe('when the menu is open:', () => {
      it('should close the menu', () => {
        editor = setupCol0()
        expect(menu()!.hidden).toBe(false)
        document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }))
        expect(menu()!.hidden).toBe(true)
      })
    })

    // noinspection DuplicatedCode: code looks same but using different HTMLElement
    describe('when the menu is closed:', () => {
      it('should not change menu visibility', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 0, 0)
        expect(menu()!.hidden).toBe(true)
        document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }))
        expect(menu()!.hidden).toBe(true)
      })
    })
  })

  // -- Move column left disabled on first column -----------------------------

  describe('Move column left item:', () => {
    describe('when in column 0:', () => {
      it('should be disabled', () => {
        editor = setupCol0()
        expect(menuItem('Move column left')!.disabled).toBe(true)
      })
    })

    describe('when in column 1:', () => {
      it('should be enabled', () => {
        editor = setupCol1()
        expect(menuItem('Move column left')!.disabled).toBe(false)
      })
    })
  })

  // -- Add column right ------------------------------------------------------

  describe('Add column right mousedown:', () => {
    it('should add a column to the table', () => {
      editor = setupCol0()
      const colsBefore = getDoc(editor).content[0].content[0].content.length
      clickItem('Add column right')
      const colsAfter = getDoc(editor).content[0].content[0].content.length
      expect(colsAfter).toBe(colsBefore + 1)
    })
  })

  // -- Add column left -------------------------------------------------------

  describe('Add column left mousedown:', () => {
    it('should add a column to the table', () => {
      editor = setupCol1()
      const colsBefore = getDoc(editor).content[0].content[0].content.length
      clickItem('Add column left')
      const colsAfter = getDoc(editor).content[0].content[0].content.length
      expect(colsAfter).toBe(colsBefore + 1)
    })
  })

  // -- Clear cells -----------------------------------------------------------

  describe('Clear cells mousedown:', () => {
    it('should empty all cells in the column', () => {
      editor = setupRow1()
      clickItem('Clear cells')
      const row1Cell = getDoc(editor).content[0].content[1].content[0]
      const row2Cell = getDoc(editor).content[0].content[2].content[0]
      expect(row1Cell.content[0].content).toBeUndefined()
      expect(row2Cell.content[0].content).toBeUndefined()
    })
  })

  // -- Delete column ---------------------------------------------------------

  describe('Delete column mousedown:', () => {
    it('should remove the column from the table', () => {
      editor = setupCol0()
      const colsBefore = getDoc(editor).content[0].content[0].content.length
      clickItem('Delete column')
      const colsAfter = getDoc(editor).content[0].content[0].content.length
      expect(colsAfter).toBe(colsBefore - 1)
    })

    it('should leave a TextSelection (not CellSelection) after deletion', () => {
      editor = setupCol0()
      clickItem('Delete column')
      expect(pmView(editor).state.selection).toBeInstanceOf(TextSelection)
    })
  })

  // -- Background colour -----------------------------------------------------

  describe('Background colour mousedown:', () => {
    it('should open the colour picker popup', () => {
      editor = setupCol0()
      expect(picker()!.hidden).toBe(true)
      clickItem('Background colour')
      expect(picker()!.hidden).toBe(false)
    })

    it('should apply the chosen colour to all cells in the column when [Apply] is clicked', () => {
      editor = setupCol0()
      clickItem('Background colour')

      const hexInput = picker()!.querySelector<HTMLInputElement>('.te-colour-picker__hex')!
      hexInput.value = '#00ff00'
      hexInput.dispatchEvent(new Event('input'))
      picker()!.querySelector<HTMLButtonElement>('.te-colour-picker__apply')!
        .dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }))

      const tableNode = pmView(editor).state.doc.firstChild!
      const map = TableMap.get(tableNode)
      for (let r = 0; r < map.height; r++) {
        const cell = tableNode.nodeAt(map.positionAt(r, 0, tableNode))!
        expect(cell.attrs['background']).toBe('#00ff00')
      }
    })

    it('should close the picker without applying when [Cancel] is clicked', () => {
      editor = setupCol0()
      clickItem('Background colour')

      const hexInput = picker()!.querySelector<HTMLInputElement>('.te-colour-picker__hex')!
      hexInput.value = '#00ff00'
      hexInput.dispatchEvent(new Event('input'))
      picker()!.querySelector<HTMLButtonElement>('.te-colour-picker__cancel')!
        .dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }))

      expect(picker()!.hidden).toBe(true)
      const tableNode = pmView(editor).state.doc.firstChild!
      const map = TableMap.get(tableNode)
      for (let r = 0; r < map.height; r++) {
        const cell = tableNode.nodeAt(map.positionAt(r, 0, tableNode))!
        expect(cell.attrs['background']).toBeNull()
      }
    })
  })

  // -- Sort increasing -------------------------------------------------------

  describe('Sort increasing mousedown:', () => {
    it('should sort body rows by the column ascending', () => {
      editor = setupRow1()
      clickItem('Sort increasing')
      // apple < cherry → row 1 should now be 'apple'
      const row1Text = getDoc(editor).content[0].content[1].content[0].content[0].content[0].text
      expect(row1Text).toBe('apple')
    })
  })

  // -- Sort decreasing -------------------------------------------------------

  describe('Sort decreasing mousedown:', () => {
    it('should sort body rows by the column descending', () => {
      editor = setupRow1()
      clickItem('Sort decreasing')
      // cherry > apple → row 1 should stay 'cherry'
      const row1Text = getDoc(editor).content[0].content[1].content[0].content[0].content[0].text
      expect(row1Text).toBe('cherry')
    })
  })

  // -- Move column right -----------------------------------------------------

  describe('Move column right mousedown:', () => {
    it('should move column 0 to column 1', () => {
      editor = setupCol0()
      clickItem('Move column right')
      // Column 0 header was 'A'; after move, column 0 should be 'B'
      const firstHeaderText = getDoc(editor).content[0].content[0].content[0].content[0].content[0].text
      expect(firstHeaderText).toBe('B')
    })
  })

  // -- position --------------------------------------------------------------

  describe('position:', () => {
    describe('when cursor is inside a table column:', () => {
      it('should set top from cell rect only — not subtract the CSS-transform offset a second time', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 0, 0)
        expect(handle()!.style.top).toBe('0px')
      })
    })
  })

  // -- menu position clamping -------------------------------------------------

  describe('menu position:', () => {
    describe('when the menu would overflow the right edge of the textarea:', () => {
      it('should clamp the menu left so it stays within the textarea', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 0, 0)

        const view = pmView(editor)
        const areaDom = view.dom.parentElement ?? view.dom
        areaDom.getBoundingClientRect         = () => rect(0, 0, 300, 600)
        handle()!.getBoundingClientRect       = () => rect(250, 50, 50, 30)   // handle at x=250
        menu()!.getBoundingClientRect         = () => rect(0, 0, 200, 300)    // menu 200px wide

        // Natural left = handle.left = 250; 250+200=450 > 300 → clamp to 300-200 = 100
        clickHandle()
        expect(menu()!.style.left).toBe('100px')
      })
    })

    describe('when the menu would overflow the left edge of the textarea:', () => {
      it('should clamp the menu left to the textarea left edge', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 0, 0)

        const view = pmView(editor)
        const areaDom = view.dom.parentElement ?? view.dom
        areaDom.getBoundingClientRect   = () => rect(50, 0, 400, 600)
        handle()!.getBoundingClientRect = () => rect(10, 50, 50, 30)    // handle at x=10, left of area
        menu()!.getBoundingClientRect   = () => rect(0, 0, 200, 300)

        // Natural left = 10; 10 < area.left(50) → clamp to 50
        clickHandle()
        expect(menu()!.style.left).toBe('50px')
      })
    })

    describe('when the menu would overflow the bottom edge of the textarea:', () => {
      it('should flip the menu above the handle', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 0, 0)

        const view = pmView(editor)
        const areaDom = view.dom.parentElement ?? view.dom
        areaDom.getBoundingClientRect   = () => rect(0, 0, 600, 200)
        handle()!.getBoundingClientRect = () => rect(50, 150, 100, 30)  // bottom=180
        menu()!.getBoundingClientRect   = () => rect(0, 0, 150, 100)    // 100px tall

        // Natural top = 180+4 = 184; 184+100=284 > 200 → flip: handle.top - height - 4 = 150-100-4 = 46
        clickHandle()
        expect(menu()!.style.top).toBe('46px')
      })
    })
  })

  // -- drag Escape cancels ----------------------------------------------------

  describe('when Escape is pressed during a drag:', () => {
    it('should cancel the drag and remove the ghost element', () => {
      editor = startDragFromCol0()
      expect(document.querySelector('.te-col-handle--ghost')).not.toBeNull()
      document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }))
      expect(document.querySelector('.te-col-handle--ghost')).toBeNull()
    })
  })

  // -- drag ghost ------------------------------------------------------------

  describe('drag ghost:', () => {
    describe('when drag movement exceeds the threshold:', () => {
      it('should create a ghost element following the cursor', () => {
        editor = startDragFromCol0()
        expect(document.querySelector('.te-col-handle--ghost')).not.toBeNull()
        document.dispatchEvent(new MouseEvent('mouseup', { bubbles: true }))
      })

      it('should give the ghost 0.5 opacity', () => {
        editor = startDragFromCol0()
        const ghost = document.querySelector<HTMLElement>('.te-col-handle--ghost')
        expect(ghost?.style.opacity).toBe('0.5')
        document.dispatchEvent(new MouseEvent('mouseup', { bubbles: true }))
      })
    })

    describe('when drag movement is below the threshold:', () => {
      it('should not create a ghost element', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 0, 0)
        handle()?.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true, clientX: 100 }))
        document.dispatchEvent(new MouseEvent('mousemove', { bubbles: true, clientX: 103 }))
        expect(document.querySelector('.te-col-handle--ghost')).toBeNull()
        document.dispatchEvent(new MouseEvent('mouseup', { bubbles: true }))
      })
    })

    describe('when drag ends:', () => {
      it('should remove the ghost element', () => {
        editor = startDragFromCol0()
        expect(document.querySelector('.te-col-handle--ghost')).not.toBeNull()
        document.dispatchEvent(new MouseEvent('mouseup', { bubbles: true }))
        expect(document.querySelector('.te-col-handle--ghost')).toBeNull()
      })
    })
  })

  // -- Move column left action ------------------------------------------------

  describe('Move column left mousedown (col > 0):', () => {
    it('should move the column to the left', () => {
      editor = setupCol1()  // column 1 = 'B'
      clickItem('Move column left')
      // 'B' is now at column 0 (was at column 1)
      const firstHeaderText = getDoc(editor).content[0].content[0].content[0].content[0].content[0].text
      expect(firstHeaderText).toBe('B')
    })
  })

  // -- drag drop column ------------------------------------------------------

  describe('when drag ends over a different column:', () => {
    it('should move the column to the drop position', () => {
      editor = new TextEdit({ element, content: TABLE_DOC })
      setCursorInCell(editor, 0, 1)  // column 1 = 'B'
      // Drag from col 1; in jsdom all cell rects are at x=0, so the nearest
      // drop edge is col 0 — which is different from col 1 or col 2 → dropColumn fires.
      handle()?.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true, clientX: 100 }))
      document.dispatchEvent(new MouseEvent('mousemove', { bubbles: true, clientX: 110 }))
      document.dispatchEvent(new MouseEvent('mouseup', { bubbles: true }))
      // 'B' (from col 1) should now be at col 0.
      const firstHeaderText = getDoc(editor).content[0].content[0].content[0].content[0].content[0].text
      expect(firstHeaderText).toBe('B')
    })
  })

  // -- drag: showDropIndicator ternary false branch --------------------------
  // getBoundingClientRect is mocked so rect.right = 100. For c < map.width
  // edgeX = rect.left = 0; for c = map.width edgeX = rect.right = 100.
  // With clientX=150 the last edge (dist=50) wins → targetCol = map.width (3).
  // Since this.col=0 and 3 > 0, the ternary false branch fires:
  // dragTargetCol = 3-1 = 2.

  describe('drag with mocked rects — targetCol > this.col (ternary false branch):', () => {
    it('should set dragTargetCol = targetCol - 1 and commit the move', () => {
      editor = new TextEdit({ element, content: TABLE_DOC })
      setCursorInCell(editor, 0, 0)
      const spy = vi.spyOn(HTMLElement.prototype, 'getBoundingClientRect').mockReturnValue({
        top: 0, bottom: 100, left: 0, right: 100, width: 100, height: 100,
        x: 0, y: 0, toJSON: () => ({}),
      } as DOMRect)
      handle()?.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true, clientX: 0 }))
      document.dispatchEvent(new MouseEvent('mousemove', { bubbles: true, clientX: 150 }))
      document.dispatchEvent(new MouseEvent('mouseup', { bubbles: true }))
      spy.mockRestore()
      // col 0 ('A') moved to position 2 → col 0 now has 'B'
      expect(getDoc(editor).content[0].content[0].content[0].content[0].content[0].text).toBe('B')
    })
  })

  // -- drag: showDropIndicator catch block -----------------------------------

  describe('drag with getBoundingClientRect throwing (catch block):', () => {
    it('should silently absorb the error and leave the table unchanged', () => {
      editor = new TextEdit({ element, content: TABLE_DOC })
      setCursorInCell(editor, 0, 1)
      // noinspection DuplicatedCode: code looks same but using different HTMLElement
      const spy = vi.spyOn(HTMLElement.prototype, 'getBoundingClientRect').mockImplementation(() => {
        throw new Error('mock getBoundingClientRect error')
      })
      handle()?.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true, clientX: 0 }))
      document.dispatchEvent(new MouseEvent('mousemove', { bubbles: true, clientX: 10 }))
      document.dispatchEvent(new MouseEvent('mouseup', { bubbles: true }))
      spy.mockRestore()
      // catch fired → dragTargetCol stays -1 → no move → col 0 still has 'A'
      expect(getDoc(editor).content[0].content[0].content[0].content[0].content[0].text).toBe('A')
    })
  })

  // -- Move column left guard (first column) --------------------------------

  describe('Move column left mousedown (first column):', () => {
    it('should leave the table unchanged when the cursor is in column 0', () => {
      editor = setupCol0()  // column 0 = 'A'
      // this.col === 0 → this.col <= 0 guard fires → no-op
      clickItem('Move column left')
      expect(getDoc(editor).content[0].content[0].content[0].content[0].content[0].text).toBe('A')
    })
  })

  // -- Move column right guard (last column) ---------------------------------

  describe('Move column right mousedown (last column):', () => {
    it('should leave the table unchanged when the cursor is in the last column', () => {
      editor = new TextEdit({ element, content: TABLE_DOC })
      setCursorInCell(editor, 0, 2)  // column 2 = last column in TABLE_DOC
      clickHandle()
      // this.col >= map.width - 1 guard fires → no-op
      clickItem('Move column right')
      expect(getDoc(editor).content[0].content[0].content[2].content[0].content[0].text).toBe('C')
    })
  })

  // -- showDropIndicator: map/tableNode null guard (line 388) --------------
  // When no table is present, map and tableNode are never set.  A drag on the
  // handle (which is always in the DOM) reaches showDropIndicator, which fires
  // the !map || !tableNode guard immediately.

  describe('when showDropIndicator is called before any table is tracked:', () => {
    it('should return immediately without crashing', () => {
      editor = new TextEdit({ element })  // no table — map and tableNode stay null
      handle()?.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true, clientX: 0 }))
      document.dispatchEvent(new MouseEvent('mousemove', { bubbles: true, clientX: 100 }))
      // Ghost is created, but showDropIndicator returned early — no crash.
      expect(document.querySelector('.te-col-handle--ghost')).not.toBeNull()
      document.dispatchEvent(new MouseEvent('mouseup', { bubbles: true }))
    })
  })

  // -- showDropIndicator: nodeDOM non-HTMLElement (line 402) ----------------
  // Mock nodeDOM to return null for the first call in showDropIndicator.
  // The guard on line 402 fires (continue), remaining edges are processed normally.

  describe('when nodeDOM returns null during a column drag:', () => {
    it('should skip that column edge without crashing', () => {
      // noinspection DuplicatedCode: code looks same but using different HTMLElement
      editor = new TextEdit({ element, content: TABLE_DOC })
      setCursorInCell(editor, 0, 0)

      const view = pmView(editor)
      const spy = vi.spyOn(view, 'nodeDOM').mockImplementationOnce(() => null)

      handle()?.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true, clientX: 100 }))
      document.dispatchEvent(new MouseEvent('mousemove', { bubbles: true, clientX: 110 }))

      expect(document.querySelector('.te-col-handle--ghost')).not.toBeNull()

      document.dispatchEvent(new MouseEvent('mouseup', { bubbles: true }))
      spy.mockRestore()
    })
  })

  // -- itemBg: tableNode null guard (line 479) -------------------------------
  // When the background colour button is clicked before any column has been
  // tracked (no table in the document), tableNode is null → line 479 fires.

  describe('when the background colour button is clicked before any column is tracked:', () => {
    it('should do nothing and leave the picker closed', () => {
      editor = new TextEdit({ element })  // no table content — tableNode stays null
      menuItem('Background colour')?.dispatchEvent(
        new MouseEvent('mousedown', { bubbles: true, cancelable: true }),
      )
      expect(picker()!.hidden).toBe(true)
    })
  })

  // -- refreshState catch (line 232) + updateHandlePosition null guard (line 238)
  //    + selectColumn null guard (line 298) -----------------------------------
  // Mocking TableMap.get to throw after the first call causes refreshState to
  // absorb the error (line 232) while leaving map=null.  updateHandlePosition
  // then hits the !map guard (line 238) and returns early.  A subsequent
  // clickHandle with map still null causes selectColumn to hit line 298.

  describe('when TableMap.get throws inside refreshState:', () => {
    it('covers: refreshState catch, updateHandlePosition null guard, selectColumn null guard', () => {
      // noinspection DuplicatedCode: code looks same but using different HTMLElement
      editor = new TextEdit({ element, content: TABLE_DOC })
      // Capture the original static method before it is replaced by the spy.
      const origGet = TableMap.get.bind(TableMap)
      // Allow the test-utils setCursorInCell call to succeed (call #1), then
      // throw for all subsequent calls (from plugin view update/refreshState).
      vi.spyOn(TableMap, 'get')
        .mockImplementationOnce(origGet)
        .mockImplementation(() => { throw new Error('mock') })
      // setCursorInCell:
      //   • its own TableMap.get call returns the real map (mockImplementationOnce)
      //   • dispatch triggers update() → refreshState → TableMap.get throws (line 232)
      //   • map stays null → updateHandlePosition → !map → line 238 fires
      setCursorInCell(editor, 0, 0)
      vi.restoreAllMocks()
      // map is still null; click handle → selectColumn → !map → line 298 fires.
      handle()?.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true, clientX: 0 }))
      document.dispatchEvent(new MouseEvent('mouseup', { bubbles: true, clientX: 0 }))
      // openMenu still runs after selectColumn returns early; menu is visible.
      expect(menu()!.hidden).toBe(false)
    })
  })

  // -- selectColumn catch block (line 305) ----------------------------------
  // When dispatch throws inside selectColumn, the catch block absorbs the error
  // and openMenu still runs (selectColumn returns normally after the catch).

  // noinspection DuplicatedCode: code looks same but using different HTMLElement
  describe('when dispatch throws during column selection:', () => {
    it('should silently absorb the error and still open the menu', () => {
      editor = new TextEdit({ element, content: TABLE_DOC })
      setCursorInCell(editor, 0, 0)
      // Start drag tracking via mousedown (no movement yet — not a drag).
      handle()?.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true, clientX: 0 }))
      // Mock dispatch to throw once — fires when selectColumn calls dispatch in onUp.
      vi.spyOn(pmView(editor), 'dispatch').mockImplementationOnce(() => { throw new Error('mock') })
      // Mouseup at same X → !wasDragging → selectColumn() → dispatch throws → catch.
      document.dispatchEvent(new MouseEvent('mouseup', { bubbles: true, clientX: 0 }))
      vi.restoreAllMocks()
      // openMenu still runs after selectColumn's internal catch; menu is visible.
      expect(menu()!.hidden).toBe(false)
    })
  })

  // -- sortColumn !isInTable guard (line 59) -----------------------------------
  // refreshState captures tableNode/map while the cursor is in the table.
  // Moving the cursor to the trailing paragraph causes update() to return early
  // (via !isInTable), leaving tableNode/map set. Clicking Sort increasing then
  // calls sortColumn(view, tableNode, ...) where view.state has cursor outside
  // the table → !isInTable(state) → return (line 59).

  describe('when the cursor moves outside the table before Sort is clicked:', () => {
    it('should return early without reordering rows', () => {
      editor = new TextEdit({ element, content: TABLE_AND_PARA_DOC })
      const rowsBefore = setTextSelectedAtCell0_0(editor)

      menuItem('Sort increasing')?.dispatchEvent(
        new MouseEvent('mousedown', { bubbles: true, cancelable: true }),
      )
      expect(getDoc(editor).content[0].content.length).toBe(rowsBefore)
    })
  })

  // -- sortColumn catch block (line 88) -------------------------------------
  // Mocking dispatch to throw inside sortColumn causes the catch at line 88
  // to absorb the error. The menu is still closed by the run() wrapper.

  describe('when dispatch throws during sortColumn:', () => {
    it('should silently absorb the error', () => {
      editor = setupCol0()
      vi.spyOn(pmView(editor), 'dispatch').mockImplementationOnce(() => { throw new Error('mock') })
      menuItem('Sort increasing')?.dispatchEvent(
        new MouseEvent('mousedown', { bubbles: true, cancelable: true }),
      )
      vi.restoreAllMocks()
      expect(menu()!.hidden).toBe(true)
    })
  })

  // -- clearColumn catch block (line 114) -----------------------------------
  // Mocking dispatch to throw inside clearColumn causes the catch at line 114
  // to absorb the error.

  describe('when dispatch throws during clearColumn:', () => {
    // noinspection DuplicatedCode: code looks same but using different HTMLElement
    it('should silently absorb the error', () => {
      editor = setupCol0()
      vi.spyOn(pmView(editor), 'dispatch').mockImplementationOnce(() => { throw new Error('mock') })
      menuItem('Clear cells')?.dispatchEvent(
        new MouseEvent('mousedown', { bubbles: true, cancelable: true }),
      )
      vi.restoreAllMocks()
      expect(menu()!.hidden).toBe(true)
    })
  })

  // -- destroy ---------------------------------------------------------------

  describe('when the editor is destroyed:', () => {
    it('should remove the handle from the DOM', () => {
      editor = new TextEdit({ element })
      editor.destroy()
      editor = undefined
      expect(handle()).toBeNull()
    })
  })
})
