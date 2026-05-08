/**
 * row-handle.ts
 *
 * Handle that appears to the left of the cursor's row. Click selects the
 * entire row and opens a context menu. Drag reorders the row.
 *
 * $Since: 2026-05-09
 */

import { Extension } from '@tiptap/core'
import { Plugin, PluginKey } from '@tiptap/pm/state'
import type { Node as PmNode } from '@tiptap/pm/model'
import type { EditorView } from '@tiptap/pm/view'
import {
  isInTable,
  selectionCell,
  CellSelection,
  TableMap,
  moveTableRow,
  rowIsHeader,
} from '@tiptap/pm/tables'
import type { Editor } from '@tiptap/core'
import {
  tableNodeAt, mkItem, ColourPickerPopup, setDisabled, bindMenuCloseListeners,
  DRAG_THRESHOLD, mkSep, createDragGhost, updateDragGhost, removeDragGhost, clearCells,
} from './utils'

const KEY = new PluginKey<null>('rowHandle')

const CSS_HANDLE    = 'te-row-handle'
const CSS_MENU      = 'te-row-handle__menu'
const CSS_ITEM      = 'te-row-handle__item'
const CSS_SEP       = 'te-row-handle__sep'
const CSS_OPEN      = 'is-open'
const CSS_DRAGGING  = 'is-dragging'
const CSS_GHOST     = 'te-row-handle--ghost'
const CSS_DROP_LINE = 'te-row-handle__drop-line'

// -- Helpers -------------------------------------------------------------------

function currentRow(
  $cell: ReturnType<typeof selectionCell>,
  map: TableMap,
  tableStart: number,
): number {
  const offset = $cell.pos - tableStart
  const idx = map.map.indexOf(offset)
  return idx >= 0 ? Math.floor(idx / map.width) : 0
}

function clearRow(view: EditorView, editor: Editor, tableNode: PmNode, tableStart: number, row: number): void {
  clearCells(view, editor, tableNode, tableStart,
    map => Array.from({ length: map.width }, (_, c) => map.positionAt(row, c, tableNode))
  )
}

// -- Handle view ---------------------------------------------------------------

class RowHandleView {
  readonly handleDom: HTMLElement
  readonly menuDom: HTMLElement
  readonly dropLineDom: HTMLElement

  private menuOpen = false
  private row = 0
  private map: TableMap | null = null
  private tableStart = 0
  private tableNode: PmNode | null = null

  private dragging = false
  private dragStartY = 0
  private dragTargetRow = -1
  private dragGhost: HTMLElement | null = null

  private readonly colourPicker: ColourPickerPopup

  private readonly onScroll: () => void

  private readonly itemAddAbove:  HTMLButtonElement
  private readonly itemAddBelow:  HTMLButtonElement
  private readonly itemClear:     HTMLButtonElement
  private readonly itemDelete:    HTMLButtonElement
  private readonly itemBg:        HTMLButtonElement
  private readonly itemMoveUp:    HTMLButtonElement
  private readonly itemMoveDown:  HTMLButtonElement

  constructor(
    private readonly editorView: EditorView,
    private readonly editor: Editor,
  ) {
    // noinspection DuplicatedCode: boilerplate DOM initialisation; only CSS constants differ, no logic to centralise
    this.handleDom = document.createElement('div')
    this.handleDom.className = CSS_HANDLE
    this.handleDom.style.display = 'none'

    this.dropLineDom = document.createElement('div')
    this.dropLineDom.className = CSS_DROP_LINE
    this.dropLineDom.hidden = true

    this.menuDom = document.createElement('div')
    this.menuDom.className = CSS_MENU
    this.menuDom.setAttribute('role', 'menu')
    this.menuDom.hidden = true

    this.itemAddAbove = mkItem('Add row above', CSS_ITEM)
    this.itemAddBelow = mkItem('Add row below', CSS_ITEM)
    this.itemClear    = mkItem('Clear cells', CSS_ITEM)
    this.itemDelete   = mkItem('Delete row', CSS_ITEM)
    this.itemBg       = mkItem('Background colour', CSS_ITEM)
    this.itemMoveUp   = mkItem('Move row up', CSS_ITEM)
    this.itemMoveDown = mkItem('Move row down', CSS_ITEM)

    this.menuDom.append(
      this.itemAddAbove, this.itemAddBelow,
      mkSep(CSS_SEP),
      this.itemClear, this.itemDelete,
      mkSep(CSS_SEP),
      this.itemBg,
      mkSep(CSS_SEP),
      this.itemMoveUp, this.itemMoveDown,
    )

    document.body.appendChild(this.handleDom)
    document.body.appendChild(this.dropLineDom)
    document.body.appendChild(this.menuDom)

    this.colourPicker = new ColourPickerPopup('te-colour-input--row')
    document.body.appendChild(this.colourPicker.dom)

    this.onScroll = () => this.update(this.editorView)
    editorView.dom.parentElement?.addEventListener('scroll', this.onScroll, { passive: true })

    this.bindEvents()
    this.update(editorView)
  }

  // noinspection DuplicatedCode: identical to ColumnHandleView.update; extraction would require exposing 5 private members
  update(view: EditorView): void {
    if (!isInTable(view.state)) {
      this.handleDom.style.display = 'none'
      this.closeMenu()
      return
    }
    this.handleDom.style.display = ''
    this.refreshState(view)
    this.updateHandlePosition(view)
    if (this.menuOpen) this.updateMenuPosition()
  }

  destroy(): void {
    this.editorView.dom.parentElement?.removeEventListener('scroll', this.onScroll)
    this.handleDom.remove()
    this.menuDom.remove()
    this.dropLineDom.remove()
    this.colourPicker.dom.remove()
  }

  // -- Private -----------------------------------------------------------------

  private refreshState(view: EditorView): void {
    try {
      const $cell = selectionCell(view.state)
      const { tableNode, tableStart } = tableNodeAt($cell)
      this.tableNode  = tableNode
      this.tableStart = tableStart
      this.map        = TableMap.get(this.tableNode)
      this.row        = currentRow($cell, this.map, this.tableStart)
    } catch { /* keep previous state */ }
  }

  private updateHandlePosition(view: EditorView): void {
    const map = this.map
    const tableNode = this.tableNode
    if (!map || !tableNode) return
    try {
      const leftOffset = map.positionAt(this.row, 0, tableNode)
      const leftCellDom = view.nodeDOM(this.tableStart + leftOffset)
      if (!(leftCellDom instanceof HTMLElement)) return
      const rect = leftCellDom.getBoundingClientRect()
      this.handleDom.style.top    = `${rect.top + rect.height / 2}px`
      this.handleDom.style.left   = `${rect.left}px`
      this.handleDom.style.height = `${rect.height}px`
    } catch { /* leave position unchanged */ }
  }

  private updateMenuPosition(): void {
    const hRect    = this.handleDom.getBoundingClientRect()
    const areaRect = this.editorView.dom.parentElement?.getBoundingClientRect()
    const menuRect = this.menuDom.getBoundingClientRect()

    let top  = hRect.top
    let left = hRect.right + 4

    if (areaRect && menuRect.width > 0) {
      if (left + menuRect.width > areaRect.right) {
        left = hRect.left - menuRect.width - 4
      }
      left = Math.max(left, areaRect.left)
      if (top + menuRect.height > areaRect.bottom) {
        top = Math.max(areaRect.top, areaRect.bottom - menuRect.height)
      }
    }

    this.menuDom.style.top  = `${top}px`
    this.menuDom.style.left = `${left}px`
  }

  private openMenu(): void {
    document.dispatchEvent(new CustomEvent('te:context-menu-open', { detail: this }))
    this.menuOpen = true
    this.menuDom.hidden = false
    this.handleDom.classList.add(CSS_OPEN)
    this.updateMenuPosition()
    this.updateMenuItemStates()
  }

  private closeMenu(): void {
    this.menuOpen = false
    this.menuDom.hidden = true
    this.handleDom.classList.remove(CSS_OPEN)
  }

  private updateMenuItemStates(): void {
    const map = this.map
    const tableNode = this.tableNode
    const isHeader = map && tableNode ? rowIsHeader(map, tableNode, this.row) : false
    setDisabled(this.itemAddAbove, isHeader)
    setDisabled(this.itemMoveUp,  !map || this.row <= 0)
    setDisabled(this.itemMoveDown, !map || this.row >= (map.height - 1))
  }

  private selectRow(): void {
    const map = this.map
    const tableNode = this.tableNode
    if (!map || !tableNode) return
    try {
      const { state } = this.editorView
      const anchor = this.tableStart + map.positionAt(this.row, 0, tableNode)
      const head   = this.tableStart + map.positionAt(this.row, map.width - 1, tableNode)
      const sel = CellSelection.create(state.doc, anchor, head)
      this.editorView.dispatch(state.tr.setSelection(sel))
    } catch { /* no-op */ }
  }

  // -- Drag ------------------------------------------------------------------

  /*
   * Some logic here structurally mirrors RowHandleView.startDragTracking;
   * however, extraction would require many callbacks for
   * negligible line savings and worse readability.
   */
  private startDragTracking(startY: number): void {
    this.dragStartY = startY
    this.dragging = false

    const onMove = (e: MouseEvent) => {
      if (!this.dragging && Math.abs(e.clientY - this.dragStartY) < DRAG_THRESHOLD) return
      if (!this.dragging) {
        this.dragging = true
        this.handleDom.classList.add(CSS_DRAGGING)
        this.dragGhost = createDragGhost(CSS_HANDLE, CSS_GHOST)
      }
      updateDragGhost(this.dragGhost, e.clientX, e.clientY)
      this.showDropIndicator(e.clientY)
    }

    // noinspection DuplicatedCode: see startDragTracking for rationale.
    const cleanup = () => {
      document.removeEventListener('mousemove', onMove)
      document.removeEventListener('mouseup', onUp)
      document.removeEventListener('keydown', onKey)
      this.handleDom.classList.remove(CSS_DRAGGING)
      removeDragGhost(this.dragGhost)
      this.dragGhost = null
      this.dropLineDom.hidden = true
    }

    // noinspection DuplicatedCode: see startDragTracking for rationale.
    const onUp = (e: MouseEvent) => {
      const wasDragging = this.dragging
      const target = this.dragTargetRow
      this.dragging = false
      this.dragTargetRow = -1
      cleanup()
      void e

      if (wasDragging) {
        if (target >= 0) this.dropRow(target)
      } else {
        this.selectRow()
        this.openMenu()
      }
    }

    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        this.dragging = false
        this.dragTargetRow = -1
        cleanup()
      }
    }

    document.addEventListener('mousemove', onMove)
    document.addEventListener('mouseup', onUp)
    document.addEventListener('keydown', onKey)
  }

  private showDropIndicator(mouseY: number): void {
    const map = this.map
    const tableNode = this.tableNode
    if (!map || !tableNode) return
    try {
      const view = this.editorView

      let bestDist = Infinity
      let lineY = 0
      let lineLeft = 0
      let lineWidth = 0
      let targetRow = -1

      for (let r = 0; r <= map.height; r++) {
        const refR = Math.min(r, map.height - 1)
        const offset = map.positionAt(refR, 0, tableNode)
        const cellDom = view.nodeDOM(this.tableStart + offset)
        if (!(cellDom instanceof HTMLElement)) continue
        const rect = cellDom.getBoundingClientRect()
        const edgeY = r < map.height ? rect.top : rect.bottom
        const dist = Math.abs(mouseY - edgeY)
        if (dist < bestDist) {
          bestDist = dist
          lineY = edgeY
          lineLeft = rect.left
          lineWidth = rect.width
          targetRow = r
        }
      }

      if (targetRow === this.row || targetRow === this.row + 1) {
        this.dropLineDom.hidden = true
        this.dragTargetRow = -1
        return
      }

      this.dragTargetRow = targetRow <= this.row ? targetRow : targetRow - 1
      this.dropLineDom.style.top   = `${lineY}px`
      this.dropLineDom.style.left  = `${lineLeft}px`
      this.dropLineDom.style.width = `${lineWidth}px`
      this.dropLineDom.hidden = false
    } catch { /* no-op */ }
  }

  private dropRow(targetRow: number): void {
    const { state } = this.editorView
    moveTableRow({ from: this.row, to: targetRow })(state, tr => this.editorView.dispatch(tr))
    this.editor.commands.focus()
  }

  // -- Events ----------------------------------------------------------------

  private bindEvents(): void {
    this.handleDom.addEventListener('mousedown', ev => {
      ev.preventDefault()
      ev.stopPropagation()
      this.closeMenu()
      this.startDragTracking(ev.clientY)
    })

    bindMenuCloseListeners({
      anchors: [this.handleDom, this.menuDom],
      isOpen: () => this.menuOpen,
      close: () => this.closeMenu(),
      identity: this,
    })

    // noinspection DuplicatedCode: local menu-item handler; closes over this.closeMenu() so cannot be a module-level export
    const run = (fn: () => void) => (ev: MouseEvent) => {
      ev.preventDefault()
      fn()
      this.closeMenu()
    }

    this.itemAddAbove.addEventListener('mousedown', run(() => {
      this.editor.chain().focus().addRowBefore().run()
    }))

    this.itemAddBelow.addEventListener('mousedown', run(() => {
      this.editor.chain().focus().addRowAfter().run()
    }))

    this.itemClear.addEventListener('mousedown', run(() => {
      if (this.tableNode) clearRow(this.editorView, this.editor, this.tableNode, this.tableStart, this.row)
    }))

    this.itemDelete.addEventListener('mousedown', run(() => {
      this.editor.chain().focus().deleteRow().run()
    }))

    this.itemBg.addEventListener('mousedown', run(() => {
      const { tableNode, tableStart, row, map } = this
      if (!tableNode || !map) return
      const offset = map.positionAt(row, 0, tableNode)
      const initialColour = (tableNode.nodeAt(offset)?.attrs['background'] as string | null) ?? '#ffffff'
      const nearRect = this.itemBg.getBoundingClientRect()
      this.colourPicker.open(nearRect, initialColour, colour => {
        const { state } = this.editorView
        const tr = state.tr
        for (let c = 0; c < map.width; c++) {
          const cellOff = map.positionAt(row, c, tableNode)
          const cellNode = tableNode.nodeAt(cellOff)
          if (!cellNode) continue
          tr.setNodeMarkup(tableStart + cellOff, undefined, { ...cellNode.attrs, background: colour })
        }
        this.editorView.dispatch(tr)
        this.editor.commands.focus()
      })
    }))

    this.itemMoveUp.addEventListener('mousedown', run(() => {
      if (!this.map || this.row <= 0) return
      const { state } = this.editorView
      moveTableRow({ from: this.row, to: this.row - 1 })(state, tr => this.editorView.dispatch(tr))
      this.editor.commands.focus()
    }))

    this.itemMoveDown.addEventListener('mousedown', run(() => {
      const map = this.map
      if (!map || this.row >= map.height - 1) return
      const { state } = this.editorView
      moveTableRow({ from: this.row, to: this.row + 1 })(state, tr => this.editorView.dispatch(tr))
      this.editor.commands.focus()
    }))
  }
}

// -- Extension -----------------------------------------------------------------

// noinspection DuplicatedCode: TipTap plugin wiring; each extension wraps a different view class via the same ProseMirror factory pattern
export const RowHandle = Extension.create({
  name: 'rowHandle',

  addProseMirrorPlugins() {
    const editor = this.editor
    return [
      new Plugin({
        key: KEY,
        view(editorView: EditorView) {
          return new RowHandleView(editorView, editor)
        },
      }),
    ]
  },
})
