/**
 * resize-table-handle.ts
 *
 * $Since: 2026-05-09
 */

import type {Editor} from '@tiptap/core'
import {Extension} from '@tiptap/core'
import {Plugin, PluginKey} from '@tiptap/pm/state'
import type {EditorView} from '@tiptap/pm/view'
import {Decoration, DecorationSet} from '@tiptap/pm/view'

const KEY = new PluginKey<null>('resizeTableHandle')

const CSS_HANDLE = 'te-resize-table-handle'
const CSS_RESIZING = 'is-resizing'

const MIN_TABLE_WIDTH = 80
// Distance from the table's right edge (px) at which the handle becomes visible.
// Half the CSS handle width (10 px) so the element appears just before the cursor
// enters the grabbable zone.
const HOVER_THRESHOLD = 6

// -- Handle view ---------------------------------------------------------------

class ResizeTableHandleView {
  readonly dom: HTMLElement

  private resizing = false
  private startX = 0
  private startWidth = 0
  private hoveredTableDom: HTMLTableElement | null = null

  private readonly onDocMouseMove: (ev: MouseEvent) => void
  private readonly onScroll: () => void

  constructor(
    private readonly editorView: EditorView,
    private readonly editor: Editor,
  ) {
    this.dom = document.createElement('div')
    this.dom.className = CSS_HANDLE
    this.dom.setAttribute('title', 'Resize table')
    this.dom.hidden = true
    document.body.appendChild(this.dom)

    this.onDocMouseMove = (ev: MouseEvent) => this.handleHover(ev)
    this.onScroll = () => this.repositionIfHovered()

    document.addEventListener('mousemove', this.onDocMouseMove)
    editorView.dom.parentElement?.addEventListener('scroll', this.onScroll, {passive: true})

    this.bindEvents()
  }

  // Called by ProseMirror after every state change.
  update(_view: EditorView): void {
    this.syncTableWidths()
    this.repositionIfHovered()
  }

  destroy(): void {
    document.removeEventListener('mousemove', this.onDocMouseMove)
    this.editorView.dom.parentElement?.removeEventListener('scroll', this.onScroll)
    this.dom.remove()
  }

  // -- Private -----------------------------------------------------------------

  private handleHover(ev: MouseEvent): void {
    if (this.resizing) return
    // When the mouse moves over the handle itself, preserve the current
    // hoveredTableDom so the handle stays visible and mousedown can fire.
    if (ev.target === this.dom) return

    const target = ev.target as Element | null
    if (!target || !this.editorView.dom.contains(target)) {
      this.hoveredTableDom = null
      this.dom.hidden = true
      return
    }

    const tableDom = target.closest('table') as HTMLTableElement | null
    if (!tableDom) {
      this.hoveredTableDom = null
      this.dom.hidden = true
      return
    }

    const tRect = tableDom.getBoundingClientRect()
    if (ev.clientX >= tRect.right - HOVER_THRESHOLD) {
      this.hoveredTableDom = tableDom
      this.dom.style.top = `${tRect.top}px`
      this.dom.style.left = `${tRect.right}px`
      this.dom.style.height = `${tRect.height}px`
      this.dom.hidden = false
    } else {
      this.hoveredTableDom = null
      this.dom.hidden = true
    }
  }

  // prosemirror-tables' updateColumnsOnResize sets table.style.width directly on
  // the <table> element from per-cell colwidth attrs. This overrides any CSS-based
  // width on the wrapper div that our Decoration.node applies. To keep tableWidth
  // in sync with the DOM, we also write table.style.width here — after node views
  // have updated, so we overwrite whatever updateColumnsOnResize just set.
  private syncTableWidths(): void {
    const {state} = this.editorView
    state.doc.descendants((node, pos) => {
      if (node.type.name !== 'table') return
      const w: number | null = node.attrs['tableWidth'] ?? null
      if (w === null) return
      const wrapperDom = this.editorView.nodeDOM(pos)
      if (!(wrapperDom instanceof HTMLElement)) return
      const tableDom = wrapperDom.querySelector('table') as HTMLTableElement | null
      if (tableDom) tableDom.style.width = `${w}px`
    })
  }

  private repositionIfHovered(): void {
    if (!this.hoveredTableDom) return
    const tRect = this.hoveredTableDom.getBoundingClientRect()
    this.dom.style.top = `${tRect.top}px`
    this.dom.style.left = `${tRect.right}px`
    this.dom.style.height = `${tRect.height}px`
  }

  private bindEvents(): void {
    this.dom.addEventListener('mousedown', ev => {
      ev.preventDefault()
      const tableDom = this.hoveredTableDom
      if (!tableDom) return

      const tablePos = this.findTablePos(tableDom)
      if (tablePos === null) return

      this.resizing = true
      this.startX = ev.clientX
      this.startWidth = tableDom.getBoundingClientRect().width
      this.dom.classList.add(CSS_RESIZING)

      const onMove = (e: MouseEvent) => {
        if (!this.resizing) return
        const newWidth = Math.max(MIN_TABLE_WIDTH, Math.round(this.startWidth + e.clientX - this.startX))
        this.applyWidth(tablePos, newWidth, false)
      }

      const onUp = (e: MouseEvent) => {
        if (!this.resizing) return
        this.resizing = false
        this.dom.classList.remove(CSS_RESIZING)
        document.removeEventListener('mousemove', onMove)
        document.removeEventListener('mouseup', onUp)
        const newWidth = Math.max(MIN_TABLE_WIDTH, Math.round(this.startWidth + e.clientX - this.startX))
        this.applyWidth(tablePos, newWidth, true)
      }

      document.addEventListener('mousemove', onMove)
      document.addEventListener('mouseup', onUp)
    })
  }

  private findTablePos(tableDom: HTMLTableElement): number | null {
    const {state} = this.editorView
    let pos: number | null = null
    state.doc.descendants((node, nodePos) => {
      if (pos !== null) return false
      if (node.type.name !== 'table') return
      if (this.editorView.nodeDOM(nodePos)?.contains(tableDom)) {
        pos = nodePos
        return false
      }
      return
    })
    return pos
  }

  // Commits a new table width via a ProseMirror transaction.
  // Intermediate drag moves use addToHistory:false so undo collapses them into
  // a single step; the final mouseup commit uses addToHistory:true.
  private applyWidth(tablePos: number, width: number, addToHistory: boolean): void {
    const {state} = this.editorView
    const tableNode = state.doc.nodeAt(tablePos)
    if (!tableNode || tableNode.type.name !== 'table') return
    try {
      const tr = state.tr.setNodeMarkup(tablePos, undefined, {
        ...tableNode.attrs,
        tableWidth: width,
      })
      if (!addToHistory) tr.setMeta('addToHistory', false)
      this.editorView.dispatch(tr)
      if (addToHistory) this.editor.commands.focus()
    } catch { /* state may have changed; no-op */
    }
  }
}

// -- Extension -----------------------------------------------------------------

// noinspection DuplicatedCode: TipTap plugin wiring; each extension wraps a different view class via the same ProseMirror factory pattern
export const ResizeTableHandle = Extension.create({
  name: 'resizeTableHandle',

  addProseMirrorPlugins() {
    const editor = this.editor
    return [
      new Plugin({
        key: KEY,

        // Apply tableWidth as a node decoration on the table's outer DOM element
        // (the prosemirror-tables wrapper div). The column-resize node view's
        // update() does not know about tableWidth, so renderHTML is never called
        // for attr changes — decorations are the only reliable way to keep the
        // DOM in sync with the state.
        props: {
          decorations(state) {
            const decos: Decoration[] = []
            state.doc.descendants((node, pos) => {
              if (node.type.name !== 'table') return
              const w: number | null = node.attrs['tableWidth'] ?? null
              if (w) {
                decos.push(Decoration.node(pos, pos + node.nodeSize, {
                  style: `width: ${w}px`,
                }))
              }
            })
            return DecorationSet.create(state.doc, decos)
          },
        },

        view(editorView: EditorView) {
          return new ResizeTableHandleView(editorView, editor)
        },
      }),
    ]
  },
})
