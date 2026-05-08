/**
 * cell-options-overlay.ts
 *
 * $Since: 2026-05-09
 */

import { Extension } from '@tiptap/core'
import { Plugin, PluginKey, TextSelection } from '@tiptap/pm/state'
import type { EditorView } from '@tiptap/pm/view'
import {
  isInTable,
  selectionCell,
  selectedRect,
  CellSelection,
} from '@tiptap/pm/tables'
import type { Editor } from '@tiptap/core'
import { ColourPickerPopup, mkItem, setDisabled, bindMenuCloseListeners } from './utils'

const KEY = new PluginKey<null>('cellOptionsOverlay')

const CSS_TRIGGER  = 'te-cell-options'
const CSS_MENU     = 'te-cell-options__menu'
const CSS_ITEM     = 'te-cell-options__item'
const CSS_OPEN     = 'is-open'

// -- Overlay view --------------------------------------------------------------

class CellOptionsView {
  readonly triggerDom: HTMLElement
  readonly menuDom: HTMLElement

  private readonly itemBackground:  HTMLButtonElement
  private readonly itemMergeCells:  HTMLButtonElement
  private readonly itemSplitCell:   HTMLButtonElement
  private readonly itemAddColRight: HTMLButtonElement
  private readonly itemAddRowBelow: HTMLButtonElement
  private readonly itemClearCell:   HTMLButtonElement
  private readonly itemDelColumn:   HTMLButtonElement
  private readonly itemDelRow:      HTMLButtonElement

  private menuOpen = false
  private readonly colourPicker: ColourPickerPopup
  private pendingBgPos: number | null = null
  private readonly onScroll: () => void

  constructor(
    private readonly editorView: EditorView,
    private readonly editor: Editor,
  ) {
    // Trigger button — small floating handle at top-right of current cell
    this.triggerDom = document.createElement('div')
    this.triggerDom.className = CSS_TRIGGER
    this.triggerDom.setAttribute('aria-label', 'Cell options')
    this.triggerDom.hidden = true

    const triggerBtn = document.createElement('button')
    triggerBtn.type = 'button'
    triggerBtn.className = `${CSS_TRIGGER}__btn`
    triggerBtn.setAttribute('aria-haspopup', 'true')
    triggerBtn.setAttribute('aria-expanded', 'false')
    triggerBtn.textContent = '⋮'
    this.triggerDom.appendChild(triggerBtn)

    // Dropdown menu
    this.menuDom = document.createElement('div')
    this.menuDom.className = CSS_MENU
    this.menuDom.setAttribute('role', 'menu')
    this.menuDom.hidden = true

    this.itemBackground  = mkItem('Background colour', CSS_ITEM)
    this.itemMergeCells  = mkItem('Merge cells', CSS_ITEM)
    this.itemSplitCell   = mkItem('Split cell', CSS_ITEM)
    this.itemAddColRight = mkItem('Add column right', CSS_ITEM)
    this.itemAddRowBelow = mkItem('Add row below', CSS_ITEM)
    this.itemClearCell   = mkItem('Clear cell', CSS_ITEM)
    this.itemDelColumn   = mkItem('Delete column', CSS_ITEM)
    this.itemDelRow      = mkItem('Delete row', CSS_ITEM)

    this.menuDom.append(
      this.itemBackground,
      this.itemMergeCells,
      this.itemSplitCell,
      this.itemAddColRight,
      this.itemAddRowBelow,
      this.itemClearCell,
      this.itemDelColumn,
      this.itemDelRow,
    )

    bindMenuCloseListeners({
      anchors: [this.triggerDom, this.menuDom],
      isOpen: () => this.menuOpen,
      close: () => this.closeMenu(),
      identity: this,
    })

    this.bindEvents(triggerBtn)

    document.body.appendChild(this.triggerDom)
    document.body.appendChild(this.menuDom)

    this.colourPicker = new ColourPickerPopup('te-colour-input--cell')
    document.body.appendChild(this.colourPicker.dom)

    this.onScroll = () => this.update(this.editorView)
    editorView.dom.parentElement?.addEventListener('scroll', this.onScroll, { passive: true })

    this.update(editorView)
  }

  update(view: EditorView): void {
    if (!isInTable(view.state)) {
      this.hide()
      return
    }
    this.triggerDom.hidden = false
    this.updatePosition(view)
    this.updateItemStates(view)

    if (this.menuOpen) {
      this.updateMenuPosition()
    }
  }

  destroy(): void {
    this.editorView.dom.parentElement?.removeEventListener('scroll', this.onScroll)
    this.triggerDom.remove()
    this.menuDom.remove()
    this.colourPicker.dom.remove()
  }

  // -- Private -----------------------------------------------------------------

  private hide(): void {
    this.triggerDom.hidden = true
    this.closeMenu()
  }

  private openMenu(): void {
    document.dispatchEvent(new CustomEvent('te:context-menu-open', { detail: this }))
    this.menuOpen = true
    this.menuDom.hidden = false
    this.triggerDom.querySelector('button')?.setAttribute('aria-expanded', 'true')
    this.triggerDom.classList.add(CSS_OPEN)
    this.updateMenuPosition()
  }

  private closeMenu(): void {
    this.menuOpen = false
    this.menuDom.hidden = true
    this.triggerDom.querySelector('button')?.setAttribute('aria-expanded', 'false')
    this.triggerDom.classList.remove(CSS_OPEN)
  }

  private updateMenuPosition(): void {
    const tRect    = this.triggerDom.getBoundingClientRect()
    const areaRect = this.editorView.dom.parentElement?.getBoundingClientRect()
    const menuRect = this.menuDom.getBoundingClientRect()

    let top  = tRect.bottom
    let left = tRect.left

    if (areaRect && menuRect.width > 0) {
      left = Math.max(areaRect.left, Math.min(left, areaRect.right - menuRect.width))
      if (top + menuRect.height > areaRect.bottom) {
        top = Math.max(areaRect.top, tRect.top - menuRect.height)
      }
    }

    this.menuDom.style.top  = `${top}px`
    this.menuDom.style.left = `${left}px`
  }

  private updatePosition(view: EditorView): void {
    try {
      const $cell = selectionCell(view.state)
      // $cell points to the position before the cell node.
      // Find the cell's DOM node to position the trigger at its top-right corner.
      const cellNode = $cell.nodeAfter
      if (!cellNode) return
      const cellDom = view.nodeDOM($cell.pos)
      if (!(cellDom instanceof HTMLElement)) return
      const dRect = cellDom.getBoundingClientRect()
      this.triggerDom.style.top   = `${dRect.top}px`
      this.triggerDom.style.right = `${window.innerWidth - dRect.right}px`
    } catch { /* leave position unchanged */ }
  }

  private updateItemStates(view: EditorView): void {
    const { state } = view
    const canMerge = (() => {
      if (!(state.selection instanceof CellSelection)) return false
      try {
        const rect = selectedRect(state)
        return rect.right - rect.left > 1 || rect.bottom - rect.top > 1
      } catch { return false }
    })()

    const canSplit = (() => {
      if (!isInTable(state)) return false
      try {
        const $cell = selectionCell(state)
        const cell = $cell.nodeAfter
        if (!cell) return false
        return (cell.attrs['colspan'] ?? 1) > 1 || (cell.attrs['rowspan'] ?? 1) > 1
      } catch { return false }
    })()

    setDisabled(this.itemMergeCells, !canMerge)
    setDisabled(this.itemSplitCell, !canSplit)
  }

  private bindEvents(triggerBtn: HTMLButtonElement): void {
    triggerBtn.addEventListener('mousedown', ev => {
      ev.preventDefault()
      ev.stopPropagation()
      this.menuOpen ? this.closeMenu() : this.openMenu()
    })

    // noinspection DuplicatedCode: local menu-item handler; closes over this.closeMenu() so cannot be a module-level export
    const run = (fn: () => void) => (ev: MouseEvent) => {
      ev.preventDefault()
      fn()
      this.closeMenu()
    }

    this.itemBackground.addEventListener('mousedown', run(() => {
      const { state } = this.editorView
      try {
        const $cell = selectionCell(state)
        this.pendingBgPos = $cell.pos
        const nearRect = this.itemBackground.getBoundingClientRect()
        const initialColour = ($cell.nodeAfter?.attrs['background'] as string | null) ?? '#ffffff'
        this.colourPicker.open(nearRect, initialColour, colour => this.applyCellBackground(colour))
      } catch { return }
    }))

    this.itemMergeCells.addEventListener('mousedown', run(() => {
      this.editor.chain().focus().mergeCells().run()
    }))

    this.itemSplitCell.addEventListener('mousedown', run(() => {
      this.editor.chain().focus().splitCell().run()
    }))

    this.itemAddColRight.addEventListener('mousedown', run(() => {
      this.editor.chain().focus().addColumnAfter().run()
    }))

    this.itemAddRowBelow.addEventListener('mousedown', run(() => {
      this.editor.chain().focus().addRowAfter().run()
    }))

    this.itemClearCell.addEventListener('mousedown', run(() => {
      this.clearCurrentCell()
    }))

    this.itemDelColumn.addEventListener('mousedown', run(() => {
      this.editor.chain().focus().deleteColumn().run()
    }))

    this.itemDelRow.addEventListener('mousedown', run(() => {
      this.editor.chain().focus().deleteRow().run()
    }))
  }

  private applyCellBackground(colour: string): void {
    if (this.pendingBgPos === null) return
    const { state } = this.editorView
    const $cell = state.doc.resolve(this.pendingBgPos)
    const cellNode = $cell.nodeAfter
    if (!cellNode) { this.pendingBgPos = null; return }
    const tr = state.tr.setNodeMarkup(this.pendingBgPos, undefined, {
      ...cellNode.attrs,
      background: colour,
    })
    this.editorView.dispatch(tr)
    this.pendingBgPos = null
    this.editor.commands.focus()
  }

  private clearCurrentCell(): void {
    const { state } = this.editorView
    if (!isInTable(state)) return
    const paragraphType = state.schema.nodes['paragraph']
    if (!paragraphType) return
    const $cell = selectionCell(state)
    const cellNode = $cell.nodeAfter
    if (!cellNode) return
    const start = $cell.pos + 1
    const end = $cell.pos + cellNode.nodeSize - 1
    if (end > start) {
      const tr = state.tr.replaceWith(start, end, paragraphType.create())
      tr.setSelection(TextSelection.create(tr.doc, start + 1))
      this.editorView.dispatch(tr)
    }
    this.editor.commands.focus()
  }

}


// -- Extension -----------------------------------------------------------------

// noinspection DuplicatedCode: TipTap plugin wiring; each extension wraps a different view class via the same ProseMirror factory pattern
export const CellOptionsOverlay = Extension.create({
  name: 'cellOptionsOverlay',

  addProseMirrorPlugins() {
    const editor = this.editor
    return [
      new Plugin({
        key: KEY,
        view(editorView: EditorView) {
          return new CellOptionsView(editorView, editor)
        },
      }),
    ]
  },
})
