/**
 * row-handle.test.ts
 *
 * $Since: 2026-05-09
 */

import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'
import {CellSelection, TableMap} from '@tiptap/pm/tables'
import {TextEdit} from '@src/TextEdit'
import {NodeType, type TextEditContent} from '@src/types'
import {getDoc, mountElement, PARA_DOC, pmView, rect, setCursorInCell} from '../test-utils'

// -- Fixtures ------------------------------------------------------------------

const TABLE_DOC: TextEditContent = {
  type: NodeType.Doc,
  content: [{
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
      {
        type: NodeType.TableRow,
        content: [
          {
            type: NodeType.TableCell,
            content: [{type: NodeType.Paragraph, content: [{type: NodeType.Text, text: '3'}]}]
          },
          {
            type: NodeType.TableCell,
            content: [{type: NodeType.Paragraph, content: [{type: NodeType.Text, text: '4'}]}]
          },
        ],
      },
    ],
  }],
}

// -- Helpers -------------------------------------------------------------------

// All overlay elements are appended to document.body (position: fixed) —
// query document.body directly rather than the editor mount element.
function handle(): HTMLElement | null {
  return document.body.querySelector<HTMLElement>('.te-row-handle')
}

function menu(): HTMLElement | null {
  return document.body.querySelector<HTMLElement>('.te-row-handle__menu')
}

function menuItem(label: string): HTMLButtonElement | null {
  const items = document.body.querySelectorAll<HTMLButtonElement>('.te-row-handle__item')
  for (const item of items) {
    if (item.textContent?.trim() === label) return item
  }
  return null
}

function clickHandle(): void {
  handle()?.dispatchEvent(new MouseEvent('mousedown', {bubbles: true, cancelable: true, clientY: 0}))
  document.dispatchEvent(new MouseEvent('mouseup', {bubbles: true, clientY: 0}))
}

function clickItem(label: string): void {
  menuItem(label)?.dispatchEvent(new MouseEvent('mousedown', {bubbles: true, cancelable: true}))
}

function picker(): HTMLElement | null {
  return document.body.querySelector<HTMLElement>('.te-colour-input--row')
}

// -- Tests ---------------------------------------------------------------------

describe('RowHandle:', () => {
  let element: HTMLElement
  let editor: TextEdit | undefined

  beforeEach(() => {
    element = mountElement()
  })

  afterEach(() => {
    try {
      editor?.destroy()
    } catch { /* already destroyed */
    }
    editor = undefined
    element.remove()
  })

  // Creates a TABLE_DOC editor, moves the cursor to row 1, and opens the row menu.
  function setupRow1(): TextEdit {
    const ed = new TextEdit({element, content: TABLE_DOC})
    setCursorInCell(ed, 1, 0)
    clickHandle()
    return ed
  }

  // Creates a TABLE_DOC editor, moves the cursor to row 0, and opens the row menu.
  function setupRow0(): TextEdit {
    const ed = new TextEdit({element, content: TABLE_DOC})
    setCursorInCell(ed, 0, 0)
    clickHandle()
    return ed
  }

  // Creates a TABLE_DOC editor, moves the cursor to row 1, and starts a drag
  // (mousedown at clientY=100 + mousemove at clientY=110).
  function startDragFromRow1(): TextEdit {
    const ed = new TextEdit({element, content: TABLE_DOC})
    setCursorInCell(ed, 1, 0)
    handle()?.dispatchEvent(new MouseEvent('mousedown', {bubbles: true, cancelable: true, clientY: 100}))
    document.dispatchEvent(new MouseEvent('mousemove', {bubbles: true, clientY: 110}))
    return ed
  }

  // Performs a complete drag gesture: mousedown → mousemove → mouseup.
  function dispatchDrag(): void {
    handle()?.dispatchEvent(new MouseEvent('mousedown', {bubbles: true, cancelable: true, clientY: 100}))
    document.dispatchEvent(new MouseEvent('mousemove', {bubbles: true, clientY: 110}))
    document.dispatchEvent(new MouseEvent('mouseup', {bubbles: true}))
  }

  // -- mount -----------------------------------------------------------------

  // noinspection DuplicatedCode: code looks same but using different HTMLElement
  describe('when TextEdit is created:', () => {
    it('should add the handle element to the DOM', () => {
      editor = new TextEdit({element})
      expect(handle()).not.toBeNull()
    })
  })

  // -- visibility ------------------------------------------------------------

  describe('visibility:', () => {
    describe('when the cursor is outside a table:', () => {
      it('should be hidden', () => {
        editor = new TextEdit({element, content: PARA_DOC})
        expect(handle()!.style.display).toBe('none')
      })
    })

    describe('when the cursor is placed inside a table cell:', () => {
      it('should become visible', () => {
        editor = new TextEdit({element, content: TABLE_DOC})
        setCursorInCell(editor, 0, 0)
        expect(handle()!.style.display).toBe('')
      })
    })
  })

  // -- menu open / close -----------------------------------------------------

  describe('handle click:', () => {
    it('should open the context menu', () => {
      editor = setupRow1()
      expect(menu()!.hidden).toBe(false)
    })

    it('should select the entire row (CellSelection)', () => {
      editor = setupRow1()
      const {selection} = pmView(editor).state
      expect(selection instanceof CellSelection).toBe(true)
    })
  })

  describe('Escape key:', () => {
    describe('when the menu is open:', () => {
      it('should close the menu', () => {
        editor = setupRow1()
        expect(menu()!.hidden).toBe(false)
        document.dispatchEvent(new KeyboardEvent('keydown', {key: 'Escape', bubbles: true}))
        expect(menu()!.hidden).toBe(true)
      })
    })

    describe('when the menu is closed:', () => {
      it('should not change menu visibility', () => {
        editor = new TextEdit({element, content: TABLE_DOC})
        setCursorInCell(editor, 1, 0)
        expect(menu()!.hidden).toBe(true)
        document.dispatchEvent(new KeyboardEvent('keydown', {key: 'Escape', bubbles: true}))
        expect(menu()!.hidden).toBe(true)
      })
    })
  })

  // -- Add row above disabled on header row ----------------------------------

  describe('Add row above item:', () => {
    describe('when in the header row:', () => {
      it('should be disabled', () => {
        editor = setupRow0()
        expect(menuItem('Add row above')!.disabled).toBe(true)
      })
    })

    describe('when in a body row:', () => {
      it('should be enabled', () => {
        editor = setupRow1()
        expect(menuItem('Add row above')!.disabled).toBe(false)
      })
    })
  })

  // -- Move row up disabled on first row -------------------------------------

  describe('Move row up item:', () => {
    describe('when in row 0:', () => {
      it('should be disabled', () => {
        editor = setupRow0()
        expect(menuItem('Move row up')!.disabled).toBe(true)
      })
    })

    describe('when in row 1:', () => {
      it('should be enabled', () => {
        editor = setupRow1()
        expect(menuItem('Move row up')!.disabled).toBe(false)
      })
    })
  })

  // -- Add row below ---------------------------------------------------------

  describe('Add row below mousedown:', () => {
    it('should add a row to the table', () => {
      editor = setupRow1()
      const rowsBefore = getDoc(editor).content[0].content.length
      clickItem('Add row below')
      const rowsAfter = getDoc(editor).content[0].content.length
      expect(rowsAfter).toBe(rowsBefore + 1)
    })
  })

  // -- Add row above ---------------------------------------------------------

  describe('Add row above mousedown:', () => {
    it('should add a row above the current row', () => {
      editor = setupRow1()
      const rowsBefore = getDoc(editor).content[0].content.length
      clickItem('Add row above')
      const rowsAfter = getDoc(editor).content[0].content.length
      expect(rowsAfter).toBe(rowsBefore + 1)
    })
  })

  // -- Clear cells -----------------------------------------------------------

  describe('Clear cells mousedown:', () => {
    it('should empty all cells in the row', () => {
      editor = setupRow1()
      clickItem('Clear cells')
      const cell0 = getDoc(editor).content[0].content[1].content[0]
      const cell1 = getDoc(editor).content[0].content[1].content[1]
      expect(cell0.content[0].content).toBeUndefined()
      expect(cell1.content[0].content).toBeUndefined()
    })
  })

  // -- Background colour -----------------------------------------------------

  describe('Background colour mousedown:', () => {
    it('should open the colour picker popup', () => {
      editor = setupRow1()
      expect(picker()!.hidden).toBe(true)
      clickItem('Background colour')
      expect(picker()!.hidden).toBe(false)
    })

    it('should apply the chosen colour to all cells in the row when [Apply] is clicked', () => {
      editor = setupRow1()
      clickItem('Background colour')

      const hexInput = picker()!.querySelector<HTMLInputElement>('.te-colour-picker__hex')!
      hexInput.value = '#0000ff'
      hexInput.dispatchEvent(new Event('input'))
      picker()!.querySelector<HTMLButtonElement>('.te-colour-picker__apply')!
        .dispatchEvent(new MouseEvent('click', {bubbles: true, cancelable: true}))

      const tableNode = pmView(editor).state.doc.firstChild!
      const map = TableMap.get(tableNode)
      for (let c = 0; c < map.width; c++) {
        const cell = tableNode.nodeAt(map.positionAt(1, c, tableNode))!
        expect(cell.attrs['background']).toBe('#0000ff')
      }
    })

    it('should close the picker without applying when [Cancel] is clicked', () => {
      editor = setupRow1()
      clickItem('Background colour')

      const hexInput = picker()!.querySelector<HTMLInputElement>('.te-colour-picker__hex')!
      hexInput.value = '#0000ff'
      hexInput.dispatchEvent(new Event('input'))
      picker()!.querySelector<HTMLButtonElement>('.te-colour-picker__cancel')!
        .dispatchEvent(new MouseEvent('click', {bubbles: true, cancelable: true}))

      expect(picker()!.hidden).toBe(true)
      const tableNode = pmView(editor).state.doc.firstChild!
      const map = TableMap.get(tableNode)
      for (let c = 0; c < map.width; c++) {
        const cell = tableNode.nodeAt(map.positionAt(1, c, tableNode))!
        expect(cell.attrs['background']).toBeNull()
      }
    })
  })

  // -- Delete row ------------------------------------------------------------

  describe('Delete row mousedown:', () => {
    it('should remove the row from the table', () => {
      editor = setupRow1()
      const rowsBefore = getDoc(editor).content[0].content.length
      clickItem('Delete row')
      const rowsAfter = getDoc(editor).content[0].content.length
      expect(rowsAfter).toBe(rowsBefore - 1)
    })
  })

  // -- Move row down ---------------------------------------------------------

  describe('Move row down mousedown:', () => {
    it('should move row 1 to row 2', () => {
      editor = setupRow1()
      clickItem('Move row down')
      // Row 1 had '1','2'; after move down, row 2 should have '1','2'
      const row2Cell0Text = getDoc(editor).content[0].content[2].content[0].content[0].content[0].text
      expect(row2Cell0Text).toBe('1')
    })
  })

  // -- position --------------------------------------------------------------

  describe('position:', () => {
    describe('when cursor is inside a table row:', () => {
      it('should set left from cell rect only — not subtract the CSS-transform offset a second time', () => {
        editor = new TextEdit({element, content: TABLE_DOC})
        setCursorInCell(editor, 1, 0)
        expect(handle()!.style.left).toBe('0px')
      })
    })
  })

  // -- menu position clamping -------------------------------------------------

  describe('menu position:', () => {
    describe('when the menu would overflow the right edge of the textarea:', () => {
      it('should flip the menu to the left of the handle', () => {
        editor = new TextEdit({element, content: TABLE_DOC})
        setCursorInCell(editor, 1, 0)

        const view = pmView(editor)
        const areaDom = view.dom.parentElement ?? view.dom
        areaDom.getBoundingClientRect = () => rect(0, 0, 300, 600)
        handle()!.getBoundingClientRect = () => rect(240, 100, 40, 30)  // right=280
        menu()!.getBoundingClientRect = () => rect(0, 0, 200, 250)    // 200px wide

        // Natural left = 280+4 = 284; 284+200=484 > 300 → flip: 240-200-4 = 36
        clickHandle()
        expect(menu()!.style.left).toBe('36px')
      })
    })

    describe('when the menu would overflow the bottom edge of the textarea:', () => {
      it('should clamp the menu top to keep it within the textarea', () => {
        editor = new TextEdit({element, content: TABLE_DOC})
        setCursorInCell(editor, 1, 0)

        const view = pmView(editor)
        const areaDom = view.dom.parentElement ?? view.dom
        areaDom.getBoundingClientRect = () => rect(0, 0, 600, 250)
        handle()!.getBoundingClientRect = () => rect(0, 180, 40, 30)    // top=180
        menu()!.getBoundingClientRect = () => rect(0, 0, 150, 200)    // 200px tall

        // Natural top = 180; 180+200=380 > 250 → clamp: 250-200 = 50
        clickHandle()
        expect(menu()!.style.top).toBe('50px')
      })
    })
  })

  // -- drag ghost ------------------------------------------------------------

  describe('drag ghost:', () => {
    describe('when drag movement exceeds the threshold:', () => {
      it('should create a ghost element following the cursor', () => {
        editor = startDragFromRow1()
        expect(document.querySelector('.te-row-handle--ghost')).not.toBeNull()
        document.dispatchEvent(new MouseEvent('mouseup', {bubbles: true}))
      })

      it('should give the ghost 0.5 opacity', () => {
        editor = startDragFromRow1()
        const ghost = document.querySelector<HTMLElement>('.te-row-handle--ghost')
        expect(ghost?.style.opacity).toBe('0.5')
        document.dispatchEvent(new MouseEvent('mouseup', {bubbles: true}))
      })
    })

    describe('when drag movement is below the threshold:', () => {
      it('should not create a ghost element', () => {
        editor = new TextEdit({element, content: TABLE_DOC})
        setCursorInCell(editor, 1, 0)
        handle()?.dispatchEvent(new MouseEvent('mousedown', {bubbles: true, cancelable: true, clientY: 100}))
        document.dispatchEvent(new MouseEvent('mousemove', {bubbles: true, clientY: 103}))
        expect(document.querySelector('.te-row-handle--ghost')).toBeNull()
        document.dispatchEvent(new MouseEvent('mouseup', {bubbles: true}))
      })
    })

    describe('when drag ends:', () => {
      it('should remove the ghost element', () => {
        editor = startDragFromRow1()
        expect(document.querySelector('.te-row-handle--ghost')).not.toBeNull()
        document.dispatchEvent(new MouseEvent('mouseup', {bubbles: true}))
        expect(document.querySelector('.te-row-handle--ghost')).toBeNull()
      })
    })

    describe('when Escape is pressed during a drag:', () => {
      it('should cancel the drag and remove the ghost element', () => {
        editor = startDragFromRow1()
        expect(document.querySelector('.te-row-handle--ghost')).not.toBeNull()
        document.dispatchEvent(new KeyboardEvent('keydown', {key: 'Escape', bubbles: true}))
        expect(document.querySelector('.te-row-handle--ghost')).toBeNull()
      })
    })
  })

  // -- Move row up action ----------------------------------------------------

  describe('Move row up mousedown (row > 0):', () => {
    it('should move the row upward', () => {
      editor = new TextEdit({element, content: TABLE_DOC})
      setCursorInCell(editor, 1, 0)  // row 1 = '1', '2'
      clickHandle()
      clickItem('Move row up')
      // Row '1','2' (was row 1) is now at row 0
      const row0Cell0Text = getDoc(editor).content[0].content[0].content[0].content[0].content[0].text
      expect(row0Cell0Text).toBe('1')
    })
  })

  // -- drag drop row ---------------------------------------------------------

  describe('when drag ends over a different row:', () => {
    it('should move the row to the drop position', () => {
      editor = new TextEdit({element, content: TABLE_DOC})
      setCursorInCell(editor, 1, 0)  // row 1 = '1', '2'
      // Drag from row 1; in jsdom all cell rects are at y=0, so the nearest
      // drop edge is row 0 — which is different from row 1 or row 2 → dropRow fires.
      dispatchDrag()
      // Row '1','2' (from row 1) should now be at row 0.
      const row0Cell0Text = getDoc(editor).content[0].content[0].content[0].content[0].content[0].text
      expect(row0Cell0Text).toBe('1')
    })
  })

  // -- Move row up guard (first row) ----------------------------------------

  describe('Move row up mousedown (first row):', () => {
    it('should leave the table unchanged when the cursor is in row 0', () => {
      editor = setupRow0()
      // this.row === 0 → this.row <= 0 guard fires → no-op
      clickItem('Move row up')
      expect(getDoc(editor).content[0].content[0].content[0].content[0].content[0].text).toBe('A')
    })
  })

  // -- Move row down guard (last row) ----------------------------------------

  describe('Move row down mousedown (last row):', () => {
    it('should leave the table unchanged when the cursor is in the last row', () => {
      editor = new TextEdit({element, content: TABLE_DOC})
      setCursorInCell(editor, 2, 0)  // row 2 is the last row in TABLE_DOC
      clickHandle()
      // this.row >= map.height - 1 guard fires → no-op
      clickItem('Move row down')
      expect(getDoc(editor).content[0].content[2].content[0].content[0].content[0].text).toBe('3')
    })
  })

  // -- drag to different row -------------------------------------------------

  describe('when dragging from the last row to a different row:', () => {
    it('should reorder the rows (covers the drop-indicator path)', () => {
      editor = new TextEdit({element, content: TABLE_DOC})
      // row 2 = last body row ('3','4'). jsdom returns all-zero rects, so
      // targetRow is always 0. Since 0 ≠ 2 (this.row) and 0 ≠ 3 (this.row+1),
      // the drop-indicator lines (386-390) execute and dragTargetRow is set to 0.
      setCursorInCell(editor, 2, 0)
      dispatchDrag()
      // Row 2 moved to position 0 → first row now starts with '3'
      expect(getDoc(editor).content[0].content[0].content[0].content[0].content[0].text).toBe('3')
    })
  })

  // -- drag: showDropIndicator ternary false branch -------------------------
  // getBoundingClientRect is mocked so rect.bottom = 100. The loop visits
  // r=0..map.height; for r < map.height edgeY = rect.top = 0, for r = map.height
  // edgeY = rect.bottom = 100. With clientY=150 the last edge (dist=50) wins,
  // giving targetRow = map.height (3). Since this.row=1 and 3 > 1, the ternary
  // false branch fires: dragTargetRow = 3-1 = 2.

  describe('drag with mocked rects — targetRow > this.row (ternary false branch):', () => {
    it('should set dragTargetRow = targetRow - 1 and commit the move', () => {
      editor = new TextEdit({element, content: TABLE_DOC})
      setCursorInCell(editor, 1, 0)
      const spy = vi.spyOn(HTMLElement.prototype, 'getBoundingClientRect').mockReturnValue({
        top: 0, bottom: 100, left: 0, right: 200, width: 200, height: 100,
        x: 0, y: 0, toJSON: () => ({}),
      } as DOMRect)
      handle()?.dispatchEvent(new MouseEvent('mousedown', {bubbles: true, cancelable: true, clientY: 0}))
      document.dispatchEvent(new MouseEvent('mousemove', {bubbles: true, clientY: 150}))
      document.dispatchEvent(new MouseEvent('mouseup', {bubbles: true}))
      spy.mockRestore()
      // row 1 ('1','2') moved to position 2 → row2 now has '1'
      expect(getDoc(editor).content[0].content[2].content[0].content[0].content[0].text).toBe('1')
    })
  })

  // -- drag: showDropIndicator catch block ------------------------------------

  describe('drag with getBoundingClientRect throwing (catch block):', () => {
    it('should silently absorb the error and leave the table unchanged', () => {
      editor = new TextEdit({element, content: TABLE_DOC})
      setCursorInCell(editor, 1, 0)
      // noinspection DuplicatedCode: code looks same but using different HTMLElement
      const spy = vi.spyOn(HTMLElement.prototype, 'getBoundingClientRect').mockImplementation(() => {
        throw new Error('mock getBoundingClientRect error')
      })
      handle()?.dispatchEvent(new MouseEvent('mousedown', {bubbles: true, cancelable: true, clientY: 0}))
      document.dispatchEvent(new MouseEvent('mousemove', {bubbles: true, clientY: 10}))
      document.dispatchEvent(new MouseEvent('mouseup', {bubbles: true}))
      spy.mockRestore()
      // catch fired → dragTargetRow stays -1 → no move → row 0 still has 'A'
      expect(getDoc(editor).content[0].content[0].content[0].content[0].content[0].text).toBe('A')
    })
  })

  // -- drag same-row guard ---------------------------------------------------

  describe('when dragging from row 0 (drop target is the same row):', () => {
    it('should leave the table unchanged', () => {
      editor = new TextEdit({element, content: TABLE_DOC})
      // row 0 = header; jsdom getBoundingClientRect returns all zeros,
      // so showDropIndicator always finds targetRow=0 = this.row=0 → guard fires
      setCursorInCell(editor, 0, 0)
      dispatchDrag()
      const row0Cell0Text = getDoc(editor).content[0].content[0].content[0].content[0].content[0].text
      expect(row0Cell0Text).toBe('A')
    })
  })

  // -- selectRow: map/tableNode null guard (line 264) -----------------------
  // When no table is present, a handle click (mousedown + mouseup without
  // significant Y movement) reaches selectRow, which fires the null guard.

  describe('when the handle is clicked before any table is tracked:', () => {
    it('should do nothing in selectRow (map/tableNode null guard fires)', () => {
      editor = new TextEdit({element})  // no table — map and tableNode stay null
      handle()?.dispatchEvent(new MouseEvent('mousedown', {bubbles: true, cancelable: true, clientY: 0}))
      // Same Y → not a drag → onUp fires → selectRow() → !map || !tableNode guard
      document.dispatchEvent(new MouseEvent('mouseup', {bubbles: true, clientY: 0}))
      // Verify no crash; menu opens (openMenu is still called after selectRow returns).
      expect(menu()!.hidden).toBe(false)
    })
  })

  // -- showDropIndicator: map/tableNode null guard (line 353) --------------
  // When no table is present, map and tableNode are never set.  A drag that
  // starts on the handle element (which is always in the DOM) reaches
  // showDropIndicator, which fires the !map || !tableNode guard immediately.

  describe('when showDropIndicator is called before any table is tracked:', () => {
    it('should return immediately without crashing', () => {
      editor = new TextEdit({element})  // no table — map and tableNode stay null
      handle()?.dispatchEvent(new MouseEvent('mousedown', {bubbles: true, cancelable: true, clientY: 0}))
      document.dispatchEvent(new MouseEvent('mousemove', {bubbles: true, clientY: 100}))
      // Ghost is created, but showDropIndicator returned early — no crash.
      expect(document.querySelector('.te-row-handle--ghost')).not.toBeNull()
      document.dispatchEvent(new MouseEvent('mouseup', {bubbles: true}))
    })
  })

  // -- showDropIndicator: nodeDOM non-HTMLElement (line 367) ----------------
  // Mock nodeDOM to return null for the first call in the drag loop.
  // The continue guard on line 367 fires; remaining edges process normally.

  describe('when nodeDOM returns null during a row drag:', () => {
    it('should skip that row edge without crashing', () => {
      // noinspection DuplicatedCode: code looks same but using different HTMLElement
      editor = new TextEdit({element, content: TABLE_DOC})
      setCursorInCell(editor, 0, 0)

      const view = pmView(editor)
      const spy = vi.spyOn(view, 'nodeDOM').mockImplementationOnce(() => null)

      handle()?.dispatchEvent(new MouseEvent('mousedown', {bubbles: true, cancelable: true, clientY: 100}))
      document.dispatchEvent(new MouseEvent('mousemove', {bubbles: true, clientY: 110}))

      expect(document.querySelector('.te-row-handle--ghost')).not.toBeNull()

      document.dispatchEvent(new MouseEvent('mouseup', {bubbles: true}))
      spy.mockRestore()
    })
  })

  // -- itemBg: tableNode null guard (line 451) -------------------------------
  // When the background colour button is clicked before any row has been
  // tracked (no table in the document), tableNode is null → line 451 fires.

  describe('when the background colour button is clicked before any row is tracked:', () => {
    it('should do nothing and leave the picker closed', () => {
      editor = new TextEdit({element})  // no table content — tableNode stays null
      menuItem('Background colour')?.dispatchEvent(
        new MouseEvent('mousedown', {bubbles: true, cancelable: true}),
      )
      expect(picker()!.hidden).toBe(true)
    })
  })

  // -- refreshState catch (line 192) + updateHandlePosition null guard (line 198)
  // When TableMap.get throws inside refreshState, the catch absorbs the error
  // (line 192) leaving map=null; updateHandlePosition then hits the !map
  // guard (line 198) and returns early.

  describe('when TableMap.get throws inside refreshState:', () => {
    it('covers: refreshState catch (line 192) and updateHandlePosition null guard (line 198)', () => {
      // noinspection DuplicatedCode: code looks same but using different HTMLElement
      editor = new TextEdit({element, content: TABLE_DOC})
      const origGet = TableMap.get.bind(TableMap)
      vi.spyOn(TableMap, 'get')
        .mockImplementationOnce(origGet)  // allow the setCursorInCell call to succeed
        .mockImplementation(() => {
          throw new Error('mock')
        })  // all subsequent calls throw
      // setCursorInCell: its own TableMap.get succeeds; dispatch triggers update() →
      // refreshState → TableMap.get throws → catch (line 192); map stays null →
      // updateHandlePosition → !map → line 198 fires.
      setCursorInCell(editor, 0, 0)
      vi.restoreAllMocks()
      // Handle is visible (display was set before refreshState ran in update()).
      expect(handle()!.style.display).toBe('')
    })
  })

  // -- selectRow catch block (line 271) --------------------------------------
  // When dispatch throws inside selectRow, the catch block absorbs the error
  // and openMenu still runs (selectRow returns normally after the catch).

  // noinspection DuplicatedCode: code looks same but using different HTMLElement
  describe('when dispatch throws during row selection:', () => {
    it('should silently absorb the error and still open the menu', () => {
      editor = new TextEdit({element, content: TABLE_DOC})
      setCursorInCell(editor, 0, 0)
      // Start drag tracking via mousedown (no movement yet — not a drag).
      handle()?.dispatchEvent(new MouseEvent('mousedown', {bubbles: true, cancelable: true, clientY: 0}))
      // Mock dispatch to throw once — fires when selectRow calls dispatch in onUp.
      vi.spyOn(pmView(editor), 'dispatch').mockImplementationOnce(() => {
        throw new Error('mock')
      })
      // Mouseup at same Y → !wasDragging → selectRow() → dispatch throws → catch.
      document.dispatchEvent(new MouseEvent('mouseup', {bubbles: true, clientY: 0}))
      vi.restoreAllMocks()
      // openMenu still runs after selectRow's internal catch; menu is visible.
      expect(menu()!.hidden).toBe(false)
    })
  })

  // -- clearRow catch block (line 80) ------------------------------------------
  // Mocking dispatch to throw inside clearRow causes the catch at line 80 to
  // absorb the error. The menu is still closed by the run() wrapper.

  describe('when dispatch throws during clearRow:', () => {
    // noinspection DuplicatedCode: code looks same but using different HTMLElement
    it('should silently absorb the error', () => {
      editor = setupRow1()
      vi.spyOn(pmView(editor), 'dispatch').mockImplementationOnce(() => {
        throw new Error('mock')
      })
      menuItem('Clear cells')?.dispatchEvent(
        new MouseEvent('mousedown', {bubbles: true, cancelable: true}),
      )
      vi.restoreAllMocks()
      expect(menu()!.hidden).toBe(true)
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
