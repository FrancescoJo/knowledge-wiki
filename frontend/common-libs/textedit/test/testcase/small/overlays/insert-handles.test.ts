/**
 * insert-handles.test.ts
 *
 * Tests for InsertColumnHandle and InsertRowHandle overlays.
 *
 * $Since: 2026-05-09
 */

import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'
import {TextEdit} from '@src/TextEdit'
import {TextSelection} from '@tiptap/pm/state'
import {
  getDoc,
  mountElement,
  PARA_DOC,
  pmView,
  setCursorInCell,
  setTextSelectedAtCell0_0,
  TABLE_AND_PARA_DOC,
  TABLE_DOC
} from '../test-utils'

// -- Helpers -------------------------------------------------------------------

// All overlay elements are appended to document.body (position: fixed) —
// query document.body directly rather than the editor mount element.
function colWrap(): HTMLElement | null {
  return document.body.querySelector<HTMLElement>('.te-insert-col')
}

function rowWrap(): HTMLElement | null {
  return document.body.querySelector<HTMLElement>('.te-insert-row')
}

// Insert handles render one button for the current cursor column/row.
// Index 0 selects the only rendered button.
function colBtn(): HTMLButtonElement | null {
  return document.body.querySelector<HTMLButtonElement>('.te-insert-col__btn')
}

function rowBtn(): HTMLButtonElement | null {
  return document.body.querySelector<HTMLButtonElement>('.te-insert-row__btn')
}

function click(btn: HTMLButtonElement): void {
  btn.dispatchEvent(new MouseEvent('mousedown', {bubbles: true, cancelable: true}))
}

// -- Tests: InsertColumnHandle -------------------------------------------------

describe('InsertColumnHandle:', () => {
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

  // -- mount -----------------------------------------------------------------

  describe('when TextEdit is created:', () => {
    it('should add the wrapper element to the DOM', () => {
      editor = new TextEdit({element})
      expect(colWrap()).not.toBeNull()
    })
  })

  // -- visibility ------------------------------------------------------------

  describe('visibility:', () => {
    describe('when the cursor is outside a table:', () => {
      it('should be hidden', () => {
        editor = new TextEdit({element, content: PARA_DOC})
        expect(colWrap()!.hidden).toBe(true)
      })
    })

    describe('when the cursor is inside a table:', () => {
      it('should become visible', () => {
        editor = new TextEdit({element, content: TABLE_DOC})
        setCursorInCell(editor, 0, 0)
        expect(colWrap()!.hidden).toBe(false)
      })
    })
  })

  // -- buttons ---------------------------------------------------------------

  describe('when inside a table:', () => {
    it('should render a button for the current column', () => {
      editor = new TextEdit({element, content: TABLE_DOC})
      setCursorInCell(editor, 0, 0)
      expect(colBtn()).not.toBeNull()
    })
  })

  // -- insert action ---------------------------------------------------------

  describe('clicking the column button:', () => {
    it('should insert a column after the current column', () => {
      editor = new TextEdit({element, content: TABLE_DOC})
      setCursorInCell(editor, 0, 0)
      const colsBefore = getDoc(editor).content[0].content[0].content.length
      click(colBtn()!)
      const colsAfter = getDoc(editor).content[0].content[0].content.length
      expect(colsAfter).toBe(colsBefore + 1)
    })
  })

  // -- insertAfterColumn catch block (line 124) ------------------------------
  // When dispatch throws inside insertAfterColumn, the catch block absorbs the
  // error and the table remains unchanged.

  describe('when dispatch throws during insertAfterColumn:', () => {
    it('should silently absorb the error and leave the table unchanged', () => {
      editor = new TextEdit({element, content: TABLE_DOC})
      setCursorInCell(editor, 0, 0)
      const colsBefore = getDoc(editor).content[0].content[0].content.length
      // Mock dispatch to throw once — fires when insertAfterColumn calls dispatch.
      vi.spyOn(pmView(editor), 'dispatch').mockImplementationOnce(() => {
        throw new Error('mock')
      })
      click(colBtn()!)
      vi.restoreAllMocks()
      // Catch absorbed the error; no column was inserted.
      expect(getDoc(editor).content[0].content[0].content.length).toBe(colsBefore)
    })
  })

  // -- destroy ---------------------------------------------------------------

  describe('when the editor is destroyed:', () => {
    it('should remove the wrapper from the DOM', () => {
      editor = new TextEdit({element})
      editor.destroy()
      editor = undefined
      expect(colWrap()).toBeNull()
    })
  })
})

// -- Tests: InsertRowHandle ----------------------------------------------------

describe('InsertRowHandle:', () => {
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

  // -- mount -----------------------------------------------------------------

  describe('when TextEdit is created:', () => {
    it('should add the wrapper element to the DOM', () => {
      editor = new TextEdit({element})
      expect(rowWrap()).not.toBeNull()
    })
  })

  // -- visibility ------------------------------------------------------------

  describe('visibility:', () => {
    describe('when the cursor is outside a table:', () => {
      it('should be hidden', () => {
        editor = new TextEdit({element, content: PARA_DOC})
        expect(rowWrap()!.hidden).toBe(true)
      })
    })

    describe('when the cursor is inside a table:', () => {
      it('should become visible', () => {
        editor = new TextEdit({element, content: TABLE_DOC})
        setCursorInCell(editor, 0, 0)
        expect(rowWrap()!.hidden).toBe(false)
      })
    })
  })

  // -- buttons ---------------------------------------------------------------

  describe('when inside a table:', () => {
    it('should render a button for the current row', () => {
      editor = new TextEdit({element, content: TABLE_DOC})
      setCursorInCell(editor, 0, 0)
      expect(rowBtn()).not.toBeNull()
    })
  })

  // -- insert action ---------------------------------------------------------

  describe('clicking the row button:', () => {
    it('should insert a row after the current row', () => {
      editor = new TextEdit({element, content: TABLE_DOC})
      setCursorInCell(editor, 0, 0)
      const rowsBefore = getDoc(editor).content[0].content.length
      click(rowBtn()!)
      const rowsAfter = getDoc(editor).content[0].content.length
      expect(rowsAfter).toBe(rowsBefore + 1)
    })
  })

  // -- insertAfterRow catch block (line 124) --------------------------------
  // When dispatch throws inside insertAfterRow, the catch block absorbs the
  // error and the table remains unchanged.

  describe('when dispatch throws during insertAfterRow:', () => {
    it('should silently absorb the error and leave the table unchanged', () => {
      editor = new TextEdit({element, content: TABLE_DOC})
      setCursorInCell(editor, 0, 0)
      const rowsBefore = getDoc(editor).content[0].content.length
      // Mock dispatch to throw once — fires when insertAfterRow calls dispatch.
      vi.spyOn(pmView(editor), 'dispatch').mockImplementationOnce(() => {
        throw new Error('mock')
      })
      click(rowBtn()!)
      vi.restoreAllMocks()
      // Catch absorbed the error; no row was inserted.
      expect(getDoc(editor).content[0].content.length).toBe(rowsBefore)
    })
  })

  // -- destroy ---------------------------------------------------------------

  describe('when the editor is destroyed:', () => {
    it('should remove the wrapper from the DOM', () => {
      editor = new TextEdit({element})
      editor.destroy()
      editor = undefined
      expect(rowWrap()).toBeNull()
    })
  })
})

describe('InsertColumnHandle — rebuild catch block:', () => {
  // noinspection DuplicatedCode: similar pattern for overlay menus
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
    vi.restoreAllMocks()
  })

  // -- rebuild() catch block -----------------------------------------------------
  // If getBoundingClientRect throws inside rebuild(), the catch absorbs the error
  // and leaves the overlay wrapper empty (no button/line rendered).
  describe('rebuild catch block:', () => {
    it('should silently absorb errors thrown by getBoundingClientRect', () => {
      editor = new TextEdit({element, content: TABLE_DOC})
      // Mock AFTER construction so the initial render succeeds.
      vi.spyOn(HTMLElement.prototype, 'getBoundingClientRect').mockImplementation(() => {
        throw new Error('mock layout error')
      })
      // setCursorInCell triggers update() → rebuild(); catch fires, dom stays empty.
      setCursorInCell(editor, 0, 0)
      expect(colWrap()!.hidden).toBe(false)
      expect(colBtn()).toBeNull()
    })
  })

  describe('insertAfterColumn guard (cursor outside table)', () => {
    it('should leave the table unchanged when the cursor is no longer in the table', () => {
      editor = new TextEdit({element, content: TABLE_AND_PARA_DOC})
      setCursorInCell(editor, 0, 0)
      const colsBefore = getDoc(editor).content[0].content[0].content.length

      // Move cursor to the trailing paragraph (atEnd puts it inside 'Hi').
      const view = pmView(editor)
      view.dispatch(view.state.tr.setSelection(TextSelection.atEnd(view.state.doc)))

      // Button is still in DOM but wrapper is hidden; click it.
      click(colBtn()!)
      expect(getDoc(editor).content[0].content[0].content.length).toBe(colsBefore)
    })
  })

  // -- insertAfterColumn / insertAfterRow guard (line 111) -----------------------
  // The guard fires when the cursor has moved out of the table between the time
  // the button was rendered and the time it is clicked.
  // Steps:
  //   1. Cursor in table → rebuild renders the button.
  //   2. Move cursor to the paragraph before the table (isInTable → false →
  //      wrapper hidden, but button DOM element still exists).
  //   3. Click the (now hidden) button → insertAfterColumn/Row fires →
  //      !isInTable check returns early, table unchanged.
  it('should leave the table unchanged when the cursor is no longer in the table', () => {
    editor = new TextEdit({element, content: TABLE_AND_PARA_DOC})
    setCursorInCell(editor, 0, 0)
    const rowsBefore = setTextSelectedAtCell0_0(editor)

    click(rowBtn()!)
    expect(getDoc(editor).content[0].content.length).toBe(rowsBefore)
  })

  // -- nodeDOM non-HTMLElement guard (line 77) ------------------------------------
  // When view.nodeDOM() returns a non-HTMLElement (e.g. null) inside rebuild(),
  // the guard on line 77 fires (return) and the button is not rendered.
  it('should not render the button when nodeDOM does not return an HTMLElement', () => {
    editor = new TextEdit({element, content: TABLE_DOC})
    // Mock AFTER construction so the initial render succeeds.
    vi.spyOn(pmView(editor), 'nodeDOM').mockReturnValue(document.createTextNode('x'))
    // setCursorInCell triggers rebuild(); nodeDOM returns a TextNode → line 77 fires.
    setCursorInCell(editor, 0, 0)
    expect(colWrap()!.hidden).toBe(false)
    expect(colBtn()).toBeNull()
  })
})
