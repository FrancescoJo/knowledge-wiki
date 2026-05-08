/**
 * insert-row-handle.ts
 *
 * Renders a '+' button on each row separator to the left of the table.
 * Clicking inserts a row immediately below that separator.
 *
 * $Since: 2026-05-09
 */

import { Extension } from '@tiptap/core'
import { Plugin, PluginKey } from '@tiptap/pm/state'
import { TextSelection } from '@tiptap/pm/state'
import type { EditorView } from '@tiptap/pm/view'
import { isInTable, selectionCell, TableMap } from '@tiptap/pm/tables'
import type { Editor } from '@tiptap/core'
import { tableDepthOf } from './utils'

const KEY = new PluginKey<null>('insertRowHandle')

const CSS_WRAP = 'te-insert-row'
const CSS_BTN  = 'te-insert-row__btn'
const CSS_LINE = 'te-insert-row__line'

// -- Handle view ---------------------------------------------------------------

class InsertRowHandleView {
  readonly dom: HTMLElement
  private readonly onScroll: () => void

  // noinspection DuplicatedCode: axis logic diverges enough that a shared base would cost more than it saves
  constructor(
    private readonly editorView: EditorView,
    private readonly editor: Editor,
  ) {
    this.dom = document.createElement('div')
    this.dom.className = CSS_WRAP
    this.dom.hidden = true
    document.body.appendChild(this.dom)

    this.onScroll = () => this.update(this.editorView)
    editorView.dom.parentElement?.addEventListener('scroll', this.onScroll, { passive: true })

    this.update(editorView)
  }

  update(view: EditorView): void {
    if (!isInTable(view.state)) {
      this.dom.hidden = true
      return
    }
    this.dom.hidden = false
    this.rebuild(view)
  }

  destroy(): void {
    this.editorView.dom.parentElement?.removeEventListener('scroll', this.onScroll)
    this.dom.remove()
  }

  // -- Private -----------------------------------------------------------------

  private rebuild(view: EditorView): void {
    this.dom.innerHTML = ''

    try {
      const $cell = selectionCell(view.state)
      const depth = tableDepthOf($cell)
      const tableNode = $cell.node(depth)
      const tableStart = $cell.start(depth)
      const map = TableMap.get(tableNode)

      // Only show the button for the current cursor row.
      const cellIdx = map.map.indexOf($cell.pos - tableStart)
      const currentRow = cellIdx >= 0 ? Math.floor(cellIdx / map.width) : 0

      const offset = map.positionAt(currentRow, 0, tableNode)
      const cellDom = view.nodeDOM(tableStart + offset)
      if (!(cellDom instanceof HTMLElement)) return
      const rect = cellDom.getBoundingClientRect()

      const x = rect.left
      const y = rect.bottom

      const line = document.createElement('div')
      line.className = CSS_LINE
      line.style.left  = `${x}px`
      line.style.top   = `${y}px`
      line.style.width = `${rect.width}px`

      const btn = document.createElement('button')
      btn.type = 'button'
      btn.className = CSS_BTN
      btn.setAttribute('title', 'Insert row')
      btn.textContent = '+'
      btn.style.left = `${x}px`
      btn.style.top  = `${y}px`

      btn.addEventListener('mousedown', ev => {
        ev.preventDefault()
        ev.stopPropagation()
        this.insertAfterRow(currentRow)
      })

      this.dom.appendChild(line)
      this.dom.appendChild(btn)
    } catch { /* leave dom empty on error */ }
  }

  private insertAfterRow(rowIndex: number): void {
    const view = this.editorView
    const { state } = view
    if (!isInTable(state)) return
    try {
      const $cell = selectionCell(state)
      const depth = tableDepthOf($cell)
      const tableNode = $cell.node(depth)
      const tableStart = $cell.start(depth)
      const map = TableMap.get(tableNode)
      // Place cursor inside a cell in the target row, then add row after.
      const offset = map.positionAt(rowIndex, 0, tableNode)
      const cursorPos = tableStart + offset + 2
      const sel = TextSelection.create(state.doc, cursorPos)
      view.dispatch(state.tr.setSelection(sel))
      this.editor.chain().focus().addRowAfter().run()
    } catch { /* no-op */ }
  }
}

// -- Extension -----------------------------------------------------------------

// noinspection DuplicatedCode: TipTap plugin wiring; each extension wraps a different view class via the same ProseMirror factory pattern
export const InsertRowHandle = Extension.create({
  name: 'insertRowHandle',

  addProseMirrorPlugins() {
    const editor = this.editor
    return [
      new Plugin({
        key: KEY,
        view(editorView: EditorView) {
          return new InsertRowHandleView(editorView, editor)
        },
      }),
    ]
  },
})
