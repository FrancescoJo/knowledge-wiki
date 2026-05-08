/**
 * cell-options-overlay.test.ts
 *
 * $Since: 2026-05-09
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { TextSelection } from '@tiptap/pm/state'
import { TableMap } from '@tiptap/pm/tables'
import { TextEdit } from '@src/TextEdit'
import {
    mountElement,
    pmView,
    getDoc,
    setCursorInCell,
    TABLE_DOC,
    PARA_DOC,
    TABLE_AND_PARA_DOC,
    setCellSelection,
    rect,
} from '../test-utils'

// -- Helpers -------------------------------------------------------------------

// All overlay elements are appended to document.body (position: fixed) —
// query document.body directly rather than the editor mount element.
function trigger(): HTMLElement | null {
  return document.body.querySelector<HTMLElement>('.te-cell-options')
}

function menu(): HTMLElement | null {
  return document.body.querySelector<HTMLElement>('.te-cell-options__menu')
}

function triggerBtn(): HTMLButtonElement | null {
  return document.body.querySelector<HTMLButtonElement>('.te-cell-options__btn')
}

function menuItem(label: string): HTMLButtonElement | null {
  const items = document.body.querySelectorAll<HTMLButtonElement>('.te-cell-options__item')
  for (const item of items) {
    if (item.textContent?.trim() === label) return item
  }
  return null
}

function clickTrigger(): void {
  triggerBtn()?.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true }))
}

function clickItem(label: string): void {
  menuItem(label)?.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true }))
}

function picker(): HTMLElement | null {
  return document.body.querySelector<HTMLElement>('.te-colour-input--cell')
}

// -- Tests ---------------------------------------------------------------------

describe('CellOptionsOverlay:', () => {
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
    it('should add the trigger element to the DOM', () => {
      editor = new TextEdit({ element })
      expect(trigger()).not.toBeNull()
    })

    it('should add the menu element to the DOM', () => {
      editor = new TextEdit({ element })
      expect(menu()).not.toBeNull()
    })
  })

  // -- visibility ------------------------------------------------------------

  describe('visibility:', () => {
    describe('when the cursor is outside a table:', () => {
      it('should be hidden', () => {
        editor = new TextEdit({ element, content: PARA_DOC })
        expect(trigger()!.hidden).toBe(true)
      })
    })

    describe('when the cursor is placed inside a table cell:', () => {
      it('should become visible', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 0, 0)
        expect(trigger()!.hidden).toBe(false)
      })
    })
  })

  // -- menu open / close -----------------------------------------------------

  describe('menu:', () => {
    describe('when trigger is clicked while menu is closed:', () => {
      it('should open the menu', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 0, 0)
        clickTrigger()
        expect(menu()!.hidden).toBe(false)
      })
    })

    describe('when trigger is clicked while menu is open:', () => {
      it('should close the menu', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 0, 0)
        clickTrigger()
        clickTrigger()
        expect(menu()!.hidden).toBe(true)
      })
    })
  })

  describe('Escape key:', () => {
    describe('when the menu is open:', () => {
      it('should close the menu', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 0, 0)
        clickTrigger()
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

  // -- Merge cells item ------------------------------------------------------

  describe('Merge cells item:', () => {
    describe('when a single cell is selected:', () => {
      it('should be disabled', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 0, 0)
        expect(menuItem('Merge cells')!.disabled).toBe(true)
      })
    })

    describe('when multiple cells are selected:', () => {
      it('should be enabled', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCellSelection(editor, 1, 0, 1, 1)
        expect(menuItem('Merge cells')!.disabled).toBe(false)
      })
    })
  })

  // -- Split cell item -------------------------------------------------------

  describe('Split cell item:', () => {
    describe('when the cell is not merged:', () => {
      it('should be disabled', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 0, 0)
        expect(menuItem('Split cell')!.disabled).toBe(true)
      })
    })

    describe('when the cell is merged:', () => {
      it('should be enabled', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCellSelection(editor, 1, 0, 1, 1)
        clickItem('Merge cells')
        setCursorInCell(editor, 1, 0)
        expect(menuItem('Split cell')!.disabled).toBe(false)
      })
    })
  })

  // -- Split cell -----------------------------------------------------------

  describe('Split cell mousedown:', () => {
    it('should split a merged cell back into individual cells', () => {
      editor = new TextEdit({ element, content: TABLE_DOC })
      setCellSelection(editor, 1, 0, 1, 1)
      clickItem('Merge cells')
      setCursorInCell(editor, 1, 0)
      const colsBefore = getDoc(editor).content[0].content[1].content.length
      clickItem('Split cell')
      const colsAfter = getDoc(editor).content[0].content[1].content.length
      expect(colsAfter).toBeGreaterThan(colsBefore)
    })
  })

  // -- Add column right ------------------------------------------------------

  describe('Add column right mousedown:', () => {
    it('should add a column to the table', () => {
      editor = new TextEdit({ element, content: TABLE_DOC })
      setCursorInCell(editor, 0, 0)
      const colsBefore = getDoc(editor).content[0].content[0].content.length
      clickItem('Add column right')
      const colsAfter = getDoc(editor).content[0].content[0].content.length
      expect(colsAfter).toBe(colsBefore + 1)
    })
  })

  // -- Add row below ---------------------------------------------------------

  describe('Add row below mousedown:', () => {
    it('should add a row to the table', () => {
      editor = new TextEdit({ element, content: TABLE_DOC })
      setCursorInCell(editor, 0, 0)
      const rowsBefore = getDoc(editor).content[0].content.length
      clickItem('Add row below')
      const rowsAfter = getDoc(editor).content[0].content.length
      expect(rowsAfter).toBe(rowsBefore + 1)
    })
  })

  // -- Clear cell ------------------------------------------------------------

  describe('Clear cell mousedown:', () => {
    it('should empty the cell content', () => {
      editor = new TextEdit({ element, content: TABLE_DOC })
      setCursorInCell(editor, 1, 0)
      clickItem('Clear cell')
      const cell = getDoc(editor).content[0].content[1].content[0]
      // Cell should contain a single empty paragraph (no text content)
      expect(cell.content).toHaveLength(1)
      expect(cell.content[0].type).toBe('paragraph')
      expect(cell.content[0].content).toBeUndefined()
    })

    it('should leave the cursor inside the cleared cell', () => {
      editor = new TextEdit({ element, content: TABLE_DOC })
      setCursorInCell(editor, 1, 0)
      clickItem('Clear cell')

      const { selection, doc } = pmView(editor).state
      expect(selection).toBeInstanceOf(TextSelection)

      const tableNode = doc.firstChild!
      const map = TableMap.get(tableNode)
      const cellOffset = map.positionAt(1, 0, tableNode)
      const cellNodeAfterClear = tableNode.nodeAt(cellOffset)!
      const cellStart = 1 + cellOffset           // absolute pos before cell
      const cellEnd   = cellStart + cellNodeAfterClear.nodeSize
      expect((selection as TextSelection).from).toBeGreaterThan(cellStart)
      expect((selection as TextSelection).from).toBeLessThan(cellEnd)
    })
  })

  // -- Clear cell — cursor outside table ------------------------------------
  // When the menu item fires but the cursor has already left the table
  // (e.g. the overlay updated and hid itself between menu open and click),
  // clearCurrentCell should return early without modifying the document.

  describe('Clear cell mousedown when cursor has moved outside the table:', () => {
    it('should leave the table unchanged', () => {
      editor = new TextEdit({ element, content: TABLE_AND_PARA_DOC })
      setCursorInCell(editor, 0, 0)

      // Move cursor to the paragraph that follows the table.
      // This triggers update() → hide() → menu is closed, trigger is hidden.
      const view = pmView(editor)
      view.dispatch(view.state.tr.setSelection(TextSelection.atEnd(view.state.doc)))

      // The menu item DOM is still present; clicking it fires clearCurrentCell(),
      // which returns early at the isInTable guard (line 307 of cell-options-overlay.ts).
      const rowsBefore = getDoc(editor).content[0].content.length
      clickItem('Clear cell')
      expect(getDoc(editor).content[0].content.length).toBe(rowsBefore)
    })
  })

  // -- Background colour -----------------------------------------------------

  describe('Background colour mousedown:', () => {
    function colourPickerAttachedEditor(): TextEdit {
        const editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 0, 0)
        clickTrigger()
        clickItem('Background colour')

        return editor
    }

    it('should open the colour picker popup', () => {
      editor = new TextEdit({ element, content: TABLE_DOC })
      setCursorInCell(editor, 0, 0)
      clickTrigger()
      expect(picker()!.hidden).toBe(true)
      clickItem('Background colour')
      expect(picker()!.hidden).toBe(false)
    })

    it('should apply the chosen colour to the current cell when [Apply] is clicked', () => {
      editor = colourPickerAttachedEditor()

      const hexInput = picker()!.querySelector<HTMLInputElement>('.te-colour-picker__hex')!
      hexInput.value = '#ff0000'
      hexInput.dispatchEvent(new Event('input'))
      picker()!.querySelector<HTMLButtonElement>('.te-colour-picker__apply')!
        .dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }))

      const tableNode = pmView(editor).state.doc.firstChild!
      const map = TableMap.get(tableNode)
      const cell = tableNode.nodeAt(map.positionAt(0, 0, tableNode))!
      expect(cell.attrs['background']).toBe('#ff0000')
    })

    it('should close the picker after [Apply] is clicked', () => {
      editor = new TextEdit({ element, content: TABLE_DOC })
      setCursorInCell(editor, 0, 0)
      clickTrigger()
      clickItem('Background colour')
      picker()!.querySelector<HTMLButtonElement>('.te-colour-picker__apply')!
        .dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }))
      expect(picker()!.hidden).toBe(true)
    })

    it('should close the picker without applying when [Cancel] is clicked', () => {
      editor = colourPickerAttachedEditor()

      const hexInput = picker()!.querySelector<HTMLInputElement>('.te-colour-picker__hex')!
      hexInput.value = '#ff0000'
      hexInput.dispatchEvent(new Event('input'))
      picker()!.querySelector<HTMLButtonElement>('.te-colour-picker__cancel')!
        .dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }))

      expect(picker()!.hidden).toBe(true)
      const tableNode = pmView(editor).state.doc.firstChild!
      const map = TableMap.get(tableNode)
      const cell = tableNode.nodeAt(map.positionAt(0, 0, tableNode))!
      expect(cell.attrs['background']).toBeNull()
    })
  })

  // -- Delete column ---------------------------------------------------------

  describe('Delete column mousedown:', () => {
    it('should remove a column from the table', () => {
      editor = new TextEdit({ element, content: TABLE_DOC })
      setCursorInCell(editor, 0, 0)
      const colsBefore = getDoc(editor).content[0].content[0].content.length
      clickItem('Delete column')
      const colsAfter = getDoc(editor).content[0].content[0].content.length
      expect(colsAfter).toBe(colsBefore - 1)
    })
  })

  // -- Delete row ------------------------------------------------------------

  describe('Delete row mousedown:', () => {
    it('should remove a row from the table', () => {
      editor = new TextEdit({ element, content: TABLE_DOC })
      setCursorInCell(editor, 1, 0)
      const rowsBefore = getDoc(editor).content[0].content.length
      clickItem('Delete row')
      const rowsAfter = getDoc(editor).content[0].content.length
      expect(rowsAfter).toBe(rowsBefore - 1)
    })
  })

  // -- menu position clamping ------------------------------------------------

  describe('menu position:', () => {
    describe('when the menu would overflow the right edge of the textarea:', () => {
      it('should clamp the menu left so it stays within the textarea', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 0, 0)

        const view = pmView(editor)
        const areaDom = view.dom.parentElement ?? view.dom
        areaDom.getBoundingClientRect    = () => rect(0, 0, 300, 600)
        trigger()!.getBoundingClientRect = () => rect(250, 20, 30, 24)   // trigger at x=250
        menu()!.getBoundingClientRect    = () => rect(0, 0, 200, 280)    // menu 200px wide

        // Natural left = trigger.left = 250; 250+200=450 > 300 → clamp to 300-200 = 100
        clickTrigger()
        expect(menu()!.style.left).toBe('100px')
      })
    })

    describe('when the menu would overflow the bottom edge of the textarea:', () => {
      it('should flip the menu above the trigger', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 0, 0)

        const view = pmView(editor)
        const areaDom = view.dom.parentElement ?? view.dom
        areaDom.getBoundingClientRect    = () => rect(0, 0, 600, 200)
        trigger()!.getBoundingClientRect = () => rect(50, 160, 30, 24)   // bottom=184
        menu()!.getBoundingClientRect    = () => rect(0, 0, 150, 100)    // 100px tall

        // Natural top = 184; 184+100=284 > 200 → flip: trigger.top - height = 160-100 = 60
        clickTrigger()
        expect(menu()!.style.top).toBe('60px')
      })
    })
  })

  // -- updatePosition guard: nodeDOM → non-HTMLElement (line 186) ---------------
  // When view.nodeDOM($cell.pos) returns a non-HTMLElement, updatePosition
  // returns early. The trigger is still visible; no error propagates.

  describe('when nodeDOM returns a non-HTMLElement in updatePosition:', () => {
    it('should leave the trigger position unchanged', () => {
      editor = new TextEdit({ element, content: TABLE_DOC })
      setCursorInCell(editor, 0, 0)
      vi.spyOn(pmView(editor), 'nodeDOM').mockReturnValue(null)
      // Move cursor to a different cell to trigger a state-change update().
      setCursorInCell(editor, 1, 0)
      vi.restoreAllMocks()
      expect(trigger()!.hidden).toBe(false)
    })
  })

  // -- updatePosition catch block (line 190) ------------------------------------
  // When getBoundingClientRect throws inside updatePosition, the catch absorbs
  // the error and the trigger remains visible.

  describe('when getBoundingClientRect throws inside updatePosition:', () => {
    it('should silently absorb the error', () => {
      editor = new TextEdit({ element, content: TABLE_DOC })
      setCursorInCell(editor, 0, 0)
      const fakeEl = document.createElement('div')
      fakeEl.getBoundingClientRect = () => { throw new Error('mock layout') }
      vi.spyOn(pmView(editor), 'nodeDOM').mockReturnValue(fakeEl)
      // Move cursor to a different cell to trigger a state-change update().
      setCursorInCell(editor, 1, 0)
      vi.restoreAllMocks()
      expect(trigger()!.hidden).toBe(false)
    })
  })

  // -- destroy ---------------------------------------------------------------

  describe('when the editor is destroyed:', () => {
    it('should remove the trigger from the DOM', () => {
      editor = new TextEdit({ element })
      editor.destroy()
      editor = undefined
      expect(trigger()).toBeNull()
    })

    it('should remove the menu from the DOM', () => {
      editor = new TextEdit({ element })
      editor.destroy()
      editor = undefined
      expect(menu()).toBeNull()
    })
  })
})
