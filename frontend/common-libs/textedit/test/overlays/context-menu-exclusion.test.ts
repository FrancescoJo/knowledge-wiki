/**
 * context-menu-exclusion.test.ts
 *
 * Verifies that at most one context menu is visible at any time. Opening
 * any of the three context menus (column handle, row handle, cell options)
 * must close whichever other menu is currently open.
 *
 * Background: handle mousedown calls stopPropagation(), so the
 * document-level outside-click listeners on other overlays do not fire.
 * The menus instead coordinate via a 'te:context-menu-open' CustomEvent
 * dispatched on document when any menu opens.
 *
 * Scenarios (6 cross-menu pairs):
 *   column open → row opens   → column closes
 *   column open → cell opens  → column closes
 *   row open    → column opens → row closes
 *   row open    → cell opens  → row closes
 *   cell open   → column opens → cell closes
 *   cell open   → row opens   → cell closes
 *
 * $Since: 2026-05-14
 */

import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { TextEdit } from '@src/TextEdit'
import { mountElement, setCursorInCell, TABLE_DOC } from '../test-utils'

// Each menu type ---------------------------------------------------------------

function colMenu(): HTMLElement | null {
  return document.body.querySelector<HTMLElement>('.te-col-handle__menu')
}

function rowMenu(): HTMLElement | null {
  return document.body.querySelector<HTMLElement>('.te-row-handle__menu')
}

function cellMenu(): HTMLElement | null {
  return document.body.querySelector<HTMLElement>('.te-cell-options__menu')
}

// Simulate a pure click on the column handle (no drag movement).
function openColMenu(): void {
  document.body.querySelector<HTMLElement>('.te-col-handle')
    ?.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true, clientX: 0 }))
  document.dispatchEvent(new MouseEvent('mouseup', { bubbles: true, clientX: 0 }))
}

// Simulate a pure click on the row handle (no drag movement).
function openRowMenu(): void {
  document.body.querySelector<HTMLElement>('.te-row-handle')
    ?.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true, clientY: 0 }))
  document.dispatchEvent(new MouseEvent('mouseup', { bubbles: true, clientY: 0 }))
}

// Simulate clicking the cell options trigger button.
function openCellMenu(): void {
  document.body.querySelector<HTMLButtonElement>('.te-cell-options__btn')
    ?.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true }))
}

// -- Tests ---------------------------------------------------------------------

describe('Context menu mutual exclusion:', () => {
  let element: HTMLElement
  let editor: TextEdit | undefined

  beforeEach(() => {
    element = mountElement()
    editor = new TextEdit({ element, content: TABLE_DOC })
    setCursorInCell(editor, 1, 1)
  })

  // noinspection DuplicatedCode: teardown is local to each suite to capture its own editor and element bindings
  afterEach(() => {
    try { editor?.destroy() } catch { /* already destroyed */ }
    editor = undefined
    element.remove()
  })

  // -- Column menu as the open menu ------------------------------------------

  describe('column handle menu is open:', () => {
    beforeEach(() => { openColMenu() })

    it('should close the column menu when the row handle menu opens', () => {
      expect(colMenu()!.hidden).toBe(false)
      openRowMenu()
      expect(rowMenu()!.hidden).toBe(false)
      expect(colMenu()!.hidden).toBe(true)
    })

    it('should close the column menu when the cell options menu opens', () => {
      expect(colMenu()!.hidden).toBe(false)
      openCellMenu()
      expect(cellMenu()!.hidden).toBe(false)
      expect(colMenu()!.hidden).toBe(true)
    })
  })

  // -- Row menu as the open menu ---------------------------------------------

  describe('row handle menu is open:', () => {
    beforeEach(() => { openRowMenu() })

    it('should close the row menu when the column handle menu opens', () => {
      expect(rowMenu()!.hidden).toBe(false)
      openColMenu()
      expect(colMenu()!.hidden).toBe(false)
      expect(rowMenu()!.hidden).toBe(true)
    })

    it('should close the row menu when the cell options menu opens', () => {
      expect(rowMenu()!.hidden).toBe(false)
      openCellMenu()
      expect(cellMenu()!.hidden).toBe(false)
      expect(rowMenu()!.hidden).toBe(true)
    })
  })

  // -- Cell options menu as the open menu ------------------------------------

  describe('cell options menu is open:', () => {
    beforeEach(() => { openCellMenu() })

    it('should close the cell options menu when the column handle menu opens', () => {
      expect(cellMenu()!.hidden).toBe(false)
      openColMenu()
      expect(colMenu()!.hidden).toBe(false)
      expect(cellMenu()!.hidden).toBe(true)
    })

    it('should close the cell options menu when the row handle menu opens', () => {
      expect(cellMenu()!.hidden).toBe(false)
      openRowMenu()
      expect(rowMenu()!.hidden).toBe(false)
      expect(cellMenu()!.hidden).toBe(true)
    })
  })
})
