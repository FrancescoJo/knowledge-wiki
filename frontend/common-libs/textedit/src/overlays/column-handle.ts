/**
 * column-handle.ts
 *
 * Handle that appears above the cursor's column. Click selects the entire
 * column and opens a context menu. Drag reorders the column.
 *
 * $Since: 2026-05-09
 */

import { Extension } from '@tiptap/core'
import { Plugin, PluginKey, TextSelection } from '@tiptap/pm/state'
import { Fragment } from '@tiptap/pm/model'
import type { Node as PmNode } from '@tiptap/pm/model'
import type { EditorView } from '@tiptap/pm/view'
import {
  isInTable,
  selectionCell,
  CellSelection,
  TableMap,
  moveTableColumn,
} from '@tiptap/pm/tables'
import type { Editor } from '@tiptap/core'
import {
  tableNodeAt, mkItem, ColourPickerPopup, setDisabled, bindMenuCloseListeners,
  DRAG_THRESHOLD, mkSep, createDragGhost, updateDragGhost, removeDragGhost, clearCells,
} from './utils'

const KEY = new PluginKey<null>('columnHandle')

const CSS_HANDLE    = 'te-col-handle'
const CSS_MENU      = 'te-col-handle__menu'
const CSS_ITEM      = 'te-col-handle__item'
const CSS_SEP       = 'te-col-handle__sep'
const CSS_OPEN      = 'is-open'
const CSS_DRAGGING  = 'is-dragging'
const CSS_GHOST     = 'te-col-handle--ghost'
const CSS_DROP_LINE = 'te-col-handle__drop-line'

// -- Helpers -------------------------------------------------------------------

function currentCol(
  $cell: ReturnType<typeof selectionCell>,
  map: TableMap,
  tableStart: number,
): number {
  const offset = $cell.pos - tableStart
  const idx = map.map.indexOf(offset)
  return idx >= 0 ? idx % map.width : 0
}

function sortColumn(view: EditorView, tableNode: PmNode, tableStart: number, col: number, ascending: boolean): void {
  const { state } = view
  if (!isInTable(state)) return
  try {
    const map = TableMap.get(tableNode)
    const tablePos = tableStart - 1

    const allRows: PmNode[] = []
    for (let r = 0; r < tableNode.childCount; r++) allRows.push(tableNode.child(r))
    const isHeader = (row: PmNode) => row.firstChild?.type.spec['tableRole'] === 'header_cell'
    const headerRows = allRows.filter(isHeader)
    const bodyRows   = allRows.filter(r => !isHeader(r))

    const getText = (row: PmNode): string => {
      const rowIdx = allRows.indexOf(row)
      const offset = map.positionAt(rowIdx, col, tableNode)
      return tableNode.nodeAt(offset)?.textContent ?? ''
    }

    bodyRows.sort((a, b) => {
      const cmp = getText(a).localeCompare(getText(b))
      return ascending ? cmp : -cmp
    })

    const newTable = tableNode.type.create(
      tableNode.attrs,
      Fragment.fromArray([...headerRows, ...bodyRows]),
    )
    const tr = state.tr.replaceWith(tablePos, tablePos + tableNode.nodeSize, newTable)
    view.dispatch(tr)
    view.dom.focus()
  } catch { /* no-op */ }
}

function clearColumn(view: EditorView, editor: Editor, tableNode: PmNode, tableStart: number, col: number): void {
  clearCells(view, editor, tableNode, tableStart,
    map => Array.from({ length: map.height }, (_, r) => map.positionAt(r, col, tableNode))
  )
}

// -- Handle view ---------------------------------------------------------------

class ColumnHandleView {
  readonly handleDom: HTMLElement
  readonly menuDom: HTMLElement
  readonly dropLineDom: HTMLElement

  private menuOpen = false
  private col = 0
  private map: TableMap | null = null
  private tableStart = 0
  private tableNode: PmNode | null = null

  private dragging = false
  private dragStartX = 0
  private dragTargetCol = -1
  private dragGhost: HTMLElement | null = null

  private readonly colourPicker: ColourPickerPopup

  private readonly onScroll: () => void

  private readonly itemSortAsc:   HTMLButtonElement
  private readonly itemSortDesc:  HTMLButtonElement
  private readonly itemBg:        HTMLButtonElement
  private readonly itemAddLeft:   HTMLButtonElement
  private readonly itemAddRight:  HTMLButtonElement
  private readonly itemClear:     HTMLButtonElement
  private readonly itemDelete:    HTMLButtonElement
  private readonly itemMoveLeft:  HTMLButtonElement
  private readonly itemMoveRight: HTMLButtonElement

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

    this.itemSortAsc   = mkItem('Sort increasing', CSS_ITEM)
    this.itemSortDesc  = mkItem('Sort decreasing', CSS_ITEM)
    this.itemBg        = mkItem('Background colour', CSS_ITEM)
    this.itemAddLeft   = mkItem('Add column left', CSS_ITEM)
    this.itemAddRight  = mkItem('Add column right', CSS_ITEM)
    this.itemClear     = mkItem('Clear cells', CSS_ITEM)
    this.itemDelete    = mkItem('Delete column', CSS_ITEM)
    this.itemMoveLeft  = mkItem('Move column left', CSS_ITEM)
    this.itemMoveRight = mkItem('Move column right', CSS_ITEM)

    this.menuDom.append(
      this.itemSortAsc, this.itemSortDesc,
      mkSep(CSS_SEP),
      this.itemBg,
      mkSep(CSS_SEP),
      this.itemAddLeft, this.itemAddRight,
      mkSep(CSS_SEP),
      this.itemClear, this.itemDelete,
      mkSep(CSS_SEP),
      this.itemMoveLeft, this.itemMoveRight,
    )

    document.body.appendChild(this.handleDom)
    document.body.appendChild(this.dropLineDom)
    document.body.appendChild(this.menuDom)

    this.colourPicker = new ColourPickerPopup('te-colour-input--col')
    document.body.appendChild(this.colourPicker.dom)

    this.onScroll = () => this.update(this.editorView)
    editorView.dom.parentElement?.addEventListener('scroll', this.onScroll, { passive: true })

    this.bindEvents()
    this.update(editorView)
  }

  // noinspection DuplicatedCode: identical to RowHandleView.update; extraction would require exposing 5 private members
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
      this.col        = currentCol($cell, this.map, this.tableStart)
    } catch { /* keep previous state */ }
  }

  private updateHandlePosition(view: EditorView): void {
    const map = this.map
    const tableNode = this.tableNode
    if (!map || !tableNode) return
    try {
      const topOffset = map.positionAt(0, this.col, tableNode)
      const topCellDom = view.nodeDOM(this.tableStart + topOffset)
      if (!(topCellDom instanceof HTMLElement)) return
      const tRect = topCellDom.getBoundingClientRect()
      this.handleDom.style.top   = `${tRect.top}px`
      this.handleDom.style.left  = `${tRect.left}px`
      this.handleDom.style.width = `${tRect.width}px`
    } catch { /* leave position unchanged */ }
  }

  private updateMenuPosition(): void {
    const hRect    = this.handleDom.getBoundingClientRect()
    const areaRect = this.editorView.dom.parentElement?.getBoundingClientRect()
    const menuRect = this.menuDom.getBoundingClientRect()

    let top  = hRect.bottom + 4
    let left = hRect.left

    if (areaRect && menuRect.width > 0) {
      left = Math.max(areaRect.left, Math.min(left, areaRect.right - menuRect.width))
      if (top + menuRect.height > areaRect.bottom) {
        top = Math.max(areaRect.top, hRect.top - menuRect.height - 4)
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
    setDisabled(this.itemMoveLeft,  !map || this.col <= 0)
    setDisabled(this.itemMoveRight, !map || this.col >= (map.width - 1))
  }

  private selectColumn(): void {
    const map = this.map
    const tableNode = this.tableNode
    if (!map || !tableNode) return
    try {
      const { state } = this.editorView
      const anchor = this.tableStart + map.positionAt(0, this.col, tableNode)
      const head   = this.tableStart + map.positionAt(map.height - 1, this.col, tableNode)
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
  private startDragTracking(startX: number): void {
    this.dragStartX = startX
    this.dragging = false

    const onMove = (e: MouseEvent) => {
      if (!this.dragging && Math.abs(e.clientX - this.dragStartX) < DRAG_THRESHOLD) return
      if (!this.dragging) {
        this.dragging = true
        this.handleDom.classList.add(CSS_DRAGGING)
        this.dragGhost = createDragGhost(CSS_HANDLE, CSS_GHOST)
      }
      updateDragGhost(this.dragGhost, e.clientX, e.clientY)
      this.showDropIndicator(e.clientX)
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
      const target = this.dragTargetCol
      this.dragging = false
      this.dragTargetCol = -1
      cleanup()

      if (wasDragging) {
        if (target >= 0) this.dropColumn(target)
      } else {
        // Pure click — select column and open menu
        this.selectColumn()
        this.openMenu()
        void e
      }
    }

    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        this.dragging = false
        this.dragTargetCol = -1
        cleanup()
      }
    }

    document.addEventListener('mousemove', onMove)
    document.addEventListener('mouseup', onUp)
    document.addEventListener('keydown', onKey)
  }

  private showDropIndicator(mouseX: number): void {
    const map = this.map
    const tableNode = this.tableNode
    if (!map || !tableNode) return
    try {
      const view = this.editorView

      let bestDist = Infinity
      let lineX = 0
      let lineTop = 0
      let lineHeight = 0
      let targetCol = -1

      for (let c = 0; c <= map.width; c++) {
        const refC = Math.min(c, map.width - 1)
        const offset = map.positionAt(0, refC, tableNode)
        const cellDom = view.nodeDOM(this.tableStart + offset)
        if (!(cellDom instanceof HTMLElement)) continue
        const rect = cellDom.getBoundingClientRect()
        const edgeX = c < map.width ? rect.left : rect.right
        const dist = Math.abs(mouseX - edgeX)
        if (dist < bestDist) {
          bestDist = dist
          lineX = edgeX
          lineTop = rect.top
          lineHeight = rect.height
          targetCol = c
        }
      }

      // Dropping on own position has no effect
      if (targetCol === this.col || targetCol === this.col + 1) {
        this.dropLineDom.hidden = true
        this.dragTargetCol = -1
        return
      }

      this.dragTargetCol = targetCol <= this.col ? targetCol : targetCol - 1
      this.dropLineDom.style.left   = `${lineX}px`
      this.dropLineDom.style.top    = `${lineTop}px`
      this.dropLineDom.style.height = `${lineHeight}px`
      this.dropLineDom.hidden = false
    } catch { /* no-op */ }
  }

  private dropColumn(targetCol: number): void {
    const { state } = this.editorView
    moveTableColumn({ from: this.col, to: targetCol })(state, tr => this.editorView.dispatch(tr))
    this.editor.commands.focus()
  }

  // -- Events ----------------------------------------------------------------

  private bindEvents(): void {
    this.handleDom.addEventListener('mousedown', ev => {
      ev.preventDefault()
      ev.stopPropagation()
      this.closeMenu()
      this.startDragTracking(ev.clientX)
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

    this.itemSortAsc.addEventListener('mousedown', run(() => {
      if (this.tableNode && this.map) sortColumn(this.editorView, this.tableNode, this.tableStart, this.col, true)
    }))

    this.itemSortDesc.addEventListener('mousedown', run(() => {
      if (this.tableNode && this.map) sortColumn(this.editorView, this.tableNode, this.tableStart, this.col, false)
    }))

    this.itemBg.addEventListener('mousedown', run(() => {
      const { tableNode, tableStart, col, map } = this
      if (!tableNode || !map) return
      const offset = map.positionAt(0, col, tableNode)
      const initialColour = (tableNode.nodeAt(offset)?.attrs['background'] as string | null) ?? '#ffffff'
      const nearRect = this.itemBg.getBoundingClientRect()
      this.colourPicker.open(nearRect, initialColour, colour => {
        const { state } = this.editorView
        const tr = state.tr
        for (let r = 0; r < map.height; r++) {
          const cellOff = map.positionAt(r, col, tableNode)
          const cellNode = tableNode.nodeAt(cellOff)
          if (!cellNode) continue
          tr.setNodeMarkup(tableStart + cellOff, undefined, { ...cellNode.attrs, background: colour })
        }
        this.editorView.dispatch(tr)
        this.editor.commands.focus()
      })
    }))

    this.itemAddLeft.addEventListener('mousedown', run(() => {
      this.editor.chain().focus().addColumnBefore().run()
    }))

    this.itemAddRight.addEventListener('mousedown', run(() => {
      this.editor.chain().focus().addColumnAfter().run()
    }))

    this.itemClear.addEventListener('mousedown', run(() => {
      if (this.tableNode) clearColumn(this.editorView, this.editor, this.tableNode, this.tableStart, this.col)
    }))

    this.itemDelete.addEventListener('mousedown', run(() => {
      this.editor.chain().focus().deleteColumn().run()
      const newState = this.editorView.state
      if (isInTable(newState) && newState.selection instanceof CellSelection) {
        try {
          const $cell = selectionCell(newState)
          this.editorView.dispatch(
            newState.tr.setSelection(TextSelection.create(newState.doc, $cell.pos + 2))
          )
        } catch { /* no-op */ }
      }
    }))

    this.itemMoveLeft.addEventListener('mousedown', run(() => {
      if (!this.map || this.col <= 0) return
      const { state } = this.editorView
      moveTableColumn({ from: this.col, to: this.col - 1 })(state, tr => this.editorView.dispatch(tr))
      this.editor.commands.focus()
    }))

    this.itemMoveRight.addEventListener('mousedown', run(() => {
      const map = this.map
      if (!map || this.col >= map.width - 1) return
      const { state } = this.editorView
      moveTableColumn({ from: this.col, to: this.col + 1 })(state, tr => this.editorView.dispatch(tr))
      this.editor.commands.focus()
    }))
  }
}

// -- Extension -----------------------------------------------------------------

// noinspection DuplicatedCode: TipTap plugin wiring; each extension wraps a different view class via the same ProseMirror factory pattern
export const ColumnHandle = Extension.create({
  name: 'columnHandle',

  addProseMirrorPlugins() {
    const editor = this.editor
    return [
      new Plugin({
        key: KEY,
        view(editorView: EditorView) {
          return new ColumnHandleView(editorView, editor)
        },
      }),
    ]
  },
})
