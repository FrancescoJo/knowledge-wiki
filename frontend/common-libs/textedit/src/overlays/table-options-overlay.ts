/**
 * table-options-overlay.ts
 *
 * $Since: 2026-05-09
 */

import { Extension } from '@tiptap/core'
import { Plugin, PluginKey } from '@tiptap/pm/state'
import type { EditorView } from '@tiptap/pm/view'
import {
  isInTable,
  selectionCell,
  TableMap,
  rowIsHeader,
  columnIsHeader,
} from '@tiptap/pm/tables'
import type { Editor } from '@tiptap/core'
import { mkItem, tableNodeAt } from './utils'

const KEY = new PluginKey<null>('tableOptionsOverlay')

const CSS_OVERLAY  = 'te-table-options'
const CSS_BTN      = 'te-table-options__btn'
const CSS_SEP      = 'te-table-options__sep'
const CSS_ACTIVE   = 'is-active'

// -- Helpers -------------------------------------------------------------------

// noinspection DuplicatedCode: creates <span aria-hidden> for a toolbar separator; differs from the shared mkSep(cssClass) which creates <hr> for a menu separator
function mkSep(): HTMLSpanElement {
  const s = document.createElement('span')
  s.className = CSS_SEP
  s.setAttribute('aria-hidden', 'true')
  return s
}

// -- Overlay view --------------------------------------------------------------

class TableOptionsView {
  readonly dom: HTMLElement

  private readonly onEditorBlur: () => void
  private readonly onScroll: () => void
  private readonly btnFixedWidths:  HTMLButtonElement
  private readonly btnHeaderRow:    HTMLButtonElement
  private readonly btnHeaderCol:    HTMLButtonElement
  private readonly btnAlignLeft:    HTMLButtonElement
  private readonly btnAlignCentre:  HTMLButtonElement
  private readonly btnAlignRight:   HTMLButtonElement
  private readonly btnDistribute:   HTMLButtonElement

  constructor(
    private readonly editorView: EditorView,
    private readonly editor: Editor,
  ) {
    this.dom = document.createElement('div')
    this.dom.className = CSS_OVERLAY
    this.dom.setAttribute('role', 'toolbar')
    this.dom.setAttribute('aria-label', 'Table options')
    this.dom.style.display = 'none'

    this.btnFixedWidths  = mkItem('Fixed widths', CSS_BTN, 'Toggle fixed column widths')
    this.btnHeaderRow    = mkItem('Header row',   CSS_BTN, 'Toggle header row')
    this.btnHeaderCol    = mkItem('Header col',   CSS_BTN, 'Toggle header column')
    this.btnAlignLeft    = mkItem('←',            CSS_BTN, 'Align left')
    this.btnAlignCentre  = mkItem('↔',            CSS_BTN, 'Align centre')
    this.btnAlignRight   = mkItem('→',            CSS_BTN, 'Align right')
    this.btnDistribute   = mkItem('Distribute',   CSS_BTN, 'Distribute columns evenly')

    this.dom.append(
      this.btnFixedWidths, this.btnHeaderRow,
      this.btnHeaderCol,
      mkSep(),
      this.btnAlignLeft, this.btnAlignCentre, this.btnAlignRight,
      mkSep(),
      this.btnDistribute,
    )

    // Prevent the editor from losing focus whenever the user interacts with any
    // part of the overlay — including buttons that have the `disabled` attribute,
    // which may not fire mousedown on the button element itself in all browsers.
    this.dom.addEventListener('mousedown', ev => { ev.preventDefault() })

    this.bindEvents()

    document.body.appendChild(this.dom)

    // Hide when the editor loses focus (no ProseMirror transaction fires in that case).
    this.onEditorBlur = () => { this.dom.style.display = 'none' }
    editorView.dom.addEventListener('blur', this.onEditorBlur)

    this.onScroll = () => this.update(this.editorView)
    editorView.dom.parentElement?.addEventListener('scroll', this.onScroll, { passive: true })

    this.update(editorView)
  }

  update(view: EditorView): void {
    if (!isInTable(view.state)) {
      this.dom.style.display = 'none'
      return
    }
    this.dom.style.display = ''
    this.updatePosition(view)
    this.updateActiveStates(view)
  }

  destroy(): void {
    this.editorView.dom.removeEventListener('blur', this.onEditorBlur)
    this.editorView.dom.parentElement?.removeEventListener('scroll', this.onScroll)
    this.dom.remove()
  }

  // -- Private -----------------------------------------------------------------

  private bindEvents(): void {
    const e = this.editor

    this.btnFixedWidths.addEventListener('mousedown', ev => {
      ev.preventDefault()
      const fixed = e.isActive('table', { fixedColumnWidths: true })
      e.chain().focus().updateAttributes('table', { fixedColumnWidths: !fixed }).run()
    })

    this.btnHeaderRow.addEventListener('mousedown', ev => {
      ev.preventDefault()
      e.chain().focus().toggleHeaderRow().run()
    })

    this.btnHeaderCol.addEventListener('mousedown', ev => {
      ev.preventDefault()
      e.chain().focus().toggleHeaderColumn().run()
    })

    this.btnAlignLeft.addEventListener('mousedown', ev => {
      ev.preventDefault()
      e.chain().focus().setTextAlign('left').run()
    })

    this.btnAlignCentre.addEventListener('mousedown', ev => {
      ev.preventDefault()
      e.chain().focus().setTextAlign('center').run()
    })

    this.btnAlignRight.addEventListener('mousedown', ev => {
      ev.preventDefault()
      e.chain().focus().setTextAlign('right').run()
    })

    this.btnDistribute.addEventListener('mousedown', ev => {
      ev.preventDefault()
      if (this.btnDistribute.disabled) return
      this.distributeColumns()
    })
  }

  private updatePosition(view: EditorView): void {
    try {
      const $cell = selectionCell(view.state)
      const { tableNode, tableStart } = tableNodeAt($cell)
      const map = TableMap.get(tableNode)
      const firstCellDom = view.nodeDOM(tableStart + map.positionAt(0, 0, tableNode))
      const tableDom = firstCellDom instanceof HTMLElement ? firstCellDom.closest('table') : null
      if (!(tableDom instanceof HTMLElement)) return
      const tRect = tableDom.getBoundingClientRect()
      const areaDom = view.dom.parentElement ?? view.dom
      const areaRect = areaDom.getBoundingClientRect()
      this.dom.style.top       = `${tRect.bottom + 4}px`
      this.dom.style.left      = `${areaRect.left + areaRect.width / 2}px`
      this.dom.style.transform = 'translateX(-50%)'
    } catch { /* leave position unchanged if calculation fails */ }
  }

  private updateActiveStates(view: EditorView): void {
    const e = this.editor
    const isFixed = e.isActive('table', { fixedColumnWidths: true })

    this.setActive(this.btnFixedWidths,  isFixed)
    this.setActive(this.btnAlignLeft,    e.isActive({ textAlign: 'left' }))
    this.setActive(this.btnAlignCentre,  e.isActive({ textAlign: 'center' }))
    this.setActive(this.btnAlignRight,   e.isActive({ textAlign: 'right' }))
    this.btnDistribute.disabled = isFixed

    try {
      const $cell = selectionCell(view.state)
      const { tableNode } = tableNodeAt($cell)
      const map = TableMap.get(tableNode)
      this.setActive(this.btnHeaderRow, rowIsHeader(map, tableNode, 0))
      this.setActive(this.btnHeaderCol, columnIsHeader(map, tableNode, 0))
    } catch {
      this.setActive(this.btnHeaderRow, false)
      this.setActive(this.btnHeaderCol, false)
    }
  }

  private setActive(btn: HTMLButtonElement, active: boolean): void {
    btn.classList.toggle(CSS_ACTIVE, active)
    btn.setAttribute('aria-pressed', String(active))
  }

  private distributeColumns(): void {
    const { state } = this.editorView
    if (!isInTable(state)) return
    const $cell = selectionCell(state)
    const { tableNode, tableStart } = tableNodeAt($cell)
    const map = TableMap.get(tableNode)

    const explicitWidth: number | null = tableNode.attrs['tableWidth'] ?? null
    let equalColWidth: number | null = null
    if (explicitWidth != null) {
      equalColWidth = Math.round(explicitWidth / map.width)
    } else {
      const firstCellDom = this.editorView.nodeDOM(tableStart + map.positionAt(0, 0, tableNode))
      const tableDom = firstCellDom instanceof HTMLElement ? firstCellDom.closest('table') : null
      const renderedWidth = tableDom ? (tableDom as HTMLElement).offsetWidth : 0
      if (renderedWidth > 0) equalColWidth = Math.round(renderedWidth / map.width)
    }
    if (equalColWidth == null) return

    const tr = state.tr
    const visited = new Set<number>()
    for (const offset of map.map) {
      if (visited.has(offset)) continue
      visited.add(offset)
      const cell = tableNode.nodeAt(offset)
      if (!cell) continue
      const colspan: number = cell.attrs['colspan'] ?? 1
      const colwidth = Array.from({ length: colspan }, () => equalColWidth!)
      tr.setNodeMarkup(tableStart + offset, undefined, { ...cell.attrs, colwidth })
    }
    this.editorView.dispatch(tr)
    this.editor.commands.focus()
  }

}

// -- Extension -----------------------------------------------------------------

// noinspection DuplicatedCode: TipTap plugin wiring; each extension wraps a different view class via the same ProseMirror factory pattern
export const TableOptionsOverlay = Extension.create({
  name: 'tableOptionsOverlay',

  addProseMirrorPlugins() {
    const editor = this.editor
    return [
      new Plugin({
        key: KEY,
        view(editorView: EditorView) {
          return new TableOptionsView(editorView, editor)
        },
      }),
    ]
  },
})
