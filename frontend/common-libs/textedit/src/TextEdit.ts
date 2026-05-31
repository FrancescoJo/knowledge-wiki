/**
 * TextEdit.ts
 *
 * $Since: 2026-05-07
 */

import {Editor} from '@tiptap/core'
import {GapCursor} from '@tiptap/pm/gapcursor'
import {Fragment} from '@tiptap/pm/model'
import {
  CellSelection,
  columnIsHeader,
  isInTable,
  moveTableColumn,
  moveTableRow,
  rowIsHeader,
  selectedRect,
  selectionCell,
  TableMap,
} from '@tiptap/pm/tables'
import type {BlockObjectType} from './extensions'
import {BLOCK_OBJECT_TYPES, buildExtensions, TABLE_DEFAULT_COLS, TABLE_DEFAULT_ROWS} from './extensions'
import type {
  HeadingLevel,
  InsertTableOptions,
  TextAlignment,
  TextEditContent,
  TextEditHandle,
  TextEditOptions
} from './types'
import {hashJson} from './utils'

/**
 * Rich text editor component wrapping TipTap core.
 *
 * Manages the full lifecycle of a TipTap editor instance: mounting,
 * content serialisation and deserialisation, formatting commands,
 * read-only toggling, and cleanup.
 *
 * @author Hwan Jo
 * @since 0.1.0
 * @version 0.1.0
 */
export class TextEdit implements TextEditHandle {
  private readonly editor: Editor
  private readonly initialHash: string
  private currentHash: string
  private readonly beforeUnloadHandler: (event: BeforeUnloadEvent) => void

  /**
   * Creates and mounts a new editor into the given element.
   *
   * @param options initialisation options
   * @since 0.1.0
   * @version 0.1.0
   */
  constructor(options: TextEditOptions) {
    this.editor = new Editor({
      element: options.element,
      extensions: buildExtensions(options.codeLanguages ?? []),
      content: options.content ?? null,
      editable: !(options.readOnly ?? false),
      onUpdate: ({editor}) => {
        this.currentHash = hashJson(editor.getJSON() as TextEditContent)
        options.onChange?.(editor.getJSON() as TextEditContent)
      },
      onTransaction: () => {
        options.onSelectionChange?.(this)
      },
    })

    // Hash the normalised document state (post-TipTap initialisation) so that
    // default attributes added by extensions are included in the baseline.
    this.initialHash = hashJson(this.editor.getJSON() as TextEditContent)
    this.currentHash = this.initialHash

    this.beforeUnloadHandler = (event: BeforeUnloadEvent) => {
      if (this.isDirty()) {
        event.preventDefault()
        event.returnValue = ''  // Required for Chrome(≤118) to show the dialog
      }
    }
    window.addEventListener('beforeunload', this.beforeUnloadHandler)
  }

  // -- Content ----------------------------------------------------------------

  /**
   * Returns the current document content serialised as JSON.
   *
   * @return serialised document content
   * @since 0.1.0
   * @version 0.1.0
   */
  getContent(): TextEditContent {
    return this.editor.getJSON() as TextEditContent
  }

  /**
   * Replaces the entire document with the given content.
   *
   * @param content document content in TipTap JSON format
   * @since 0.1.0
   * @version 0.1.0
   */
  setContent(content: TextEditContent): void {
    this.editor.commands.setContent(content, true)
  }

  // -- State ------------------------------------------------------------------

  /**
   * Toggles the editor between editable and read-only modes.
   *
   * @param readOnly pass true to make the editor non-interactive
   * @since 0.1.0
   * @version 0.1.0
   */
  setReadOnly(readOnly: boolean): void {
    this.editor.setEditable(!readOnly)
  }

  /**
   * Returns true when the named mark or node is active at the current selection.
   * Used by toolbar implementations to reflect the current formatting state.
   *
   * @param name TipTap mark or node name (e.g. 'bold', 'heading')
   * @param attributes optional attribute filter (e.g. { level: 2 } for h2)
   * @return whether the mark or node is active
   * @since 0.1.0
   * @version 0.1.0
   */
  isActive(name: string, attributes?: Record<string, unknown>): boolean
  isActive(attributes: Record<string, unknown>): boolean
  isActive(nameOrAttributes: string | Record<string, unknown>, attributes?: Record<string, unknown>): boolean {
    if (typeof nameOrAttributes === 'string') return this.editor.isActive(nameOrAttributes, attributes)
    return this.editor.isActive(nameOrAttributes)
  }

  /**
   * Returns true when the editor currently has keyboard focus.
   *
   * @return whether the editor is focused
   * @since 0.1.0
   * @version 0.1.0
   */
  isFocused(): boolean {
    return this.editor.isFocused
  }

  /**
   * Returns true when the document has been modified since the editor was created.
   *
   * @return whether the current content differs from the initial content
   * @since 0.1.0
   * @version 0.1.0
   */
  isDirty(): boolean {
    return this.currentHash !== this.initialHash
  }

  /**
   * Returns true when a GapCursor is positioned immediately after a block object
   * (codeBlock, table, or blockquote). Use this to reflect the boundary state in
   * toolbar or status indicators.
   *
   * @return whether the cursor is at an object boundary gap
   * @since 0.1.0
   * @version 0.1.0
   */
  isAtObjectBoundary(): boolean {
    const {selection} = this.editor.state
    if (!(selection instanceof GapCursor)) return false
    const nodeBefore = selection.$head.nodeBefore
    return nodeBefore !== null && BLOCK_OBJECT_TYPES.includes(nodeBefore.type.name as BlockObjectType)
  }

  // -- Focus ------------------------------------------------------------------

  /**
   * Moves keyboard focus into the editor.
   *
   * @since 0.1.0
   * @version 0.1.0
   */
  focus(): void {
    this.editor.view.dom.focus()
  }

  // -- Text formatting --------------------------------------------------------

  /** @since 0.1.0 @version 0.1.0 */
  toggleBold(): void {
    this.editor.chain().focus().toggleBold().run()
  }

  /** @since 0.1.0 @version 0.1.0 */
  toggleItalic(): void {
    this.editor.chain().focus().toggleItalic().run()
  }

  /** @since 0.1.0 @version 0.1.0 */
  toggleStrike(): void {
    this.editor.chain().focus().toggleStrike().run()
  }

  /** @since 0.1.0 @version 0.1.0 */
  toggleUnderline(): void {
    this.editor.chain().focus().toggleUnderline().run()
  }

  /** @since 0.1.0 @version 0.1.0 */
  toggleCode(): void {
    this.editor.chain().focus().toggleCode().run()
  }

  // -- Link -------------------------------------------------------------------

  /**
   * Applies a hyperlink to the current selection.
   *
   * @param href URL to link to
   * @since 0.1.0
   * @version 0.1.0
   */
  setLink(href: string): void {
    this.editor.chain().focus().setLink({href}).run()
  }

  /** @since 0.1.0 @version 0.1.0 */
  unsetLink(): void {
    this.editor.chain().focus().unsetLink().run()
  }

  // -- Colour -----------------------------------------------------------------

  /**
   * Applies a foreground colour to the current selection.
   *
   * @param colour CSS colour value (e.g. '#ff0000')
   * @since 0.1.0
   * @version 0.1.0
   */
  setTextColour(colour: string): void {
    this.editor.chain().focus().setColor(colour).run()
  }

  /** @since 0.1.0 @version 0.1.0 */
  unsetTextColour(): void {
    this.editor.chain().focus().unsetColor().run()
  }

  /**
   * Applies a background highlight colour to the current selection.
   *
   * @param colour CSS colour value (e.g. '#ffff00')
   * @since 0.1.0
   * @version 0.1.0
   */
  setHighlightColour(colour: string): void {
    this.editor.chain().focus().setHighlight({color: colour}).run()
  }

  /** @since 0.1.0 @version 0.1.0 */
  unsetHighlightColour(): void {
    this.editor.chain().focus().unsetHighlight().run()
  }

  // -- Script -----------------------------------------------------------------

  /** @since 0.1.0 @version 0.1.0 */
  toggleSuperscript(): void {
    this.editor.chain().focus().toggleSuperscript().run()
  }

  /** @since 0.1.0 @version 0.1.0 */
  toggleSubscript(): void {
    this.editor.chain().focus().toggleSubscript().run()
  }

  // -- Block formatting -------------------------------------------------------

  /**
   * Toggles the current block to a heading of the given level.
   * If the block is already a heading at that level, it reverts to a paragraph.
   *
   * @param level heading level between 1 and 6
   * @since 0.1.0
   * @version 0.1.0
   */
  toggleHeading(level: HeadingLevel): void {
    this.editor.chain().focus().toggleHeading({level}).run()
  }

  /** @since 0.1.0 @version 0.1.0 */
  toggleBulletList(): void {
    this.editor.chain().focus().toggleBulletList().run()
  }

  /** @since 0.1.0 @version 0.1.0 */
  toggleOrderedList(): void {
    this.editor.chain().focus().toggleOrderedList().run()
  }

  /** @since 0.1.0 @version 0.1.0 */
  toggleTaskList(): void {
    this.editor.chain().focus().toggleTaskList().run()
  }

  /** @since 0.1.0 @version 0.1.0 */
  toggleBlockquote(): void {
    this.editor.chain().focus().toggleBlockquote().run()
  }

  /** @since 0.1.0 @version 0.1.0 */
  toggleCodeBlock(): void {
    this.editor.chain().focus().toggleCodeBlock().run()
  }

  // -- Alignment --------------------------------------------------------------

  /**
   * Sets the text alignment of the current block.
   *
   * @param alignment alignment value
   * @since 0.1.0
   * @version 0.1.0
   */
  setTextAlign(alignment: TextAlignment): void {
    this.editor.chain().focus().setTextAlign(alignment === 'centre' ? 'center' : alignment).run()
  }

  // -- Table ------------------------------------------------------------------

  /**
   * Inserts a table at the current cursor position.
   *
   * @param options optional dimensions and header configuration
   * @since 0.1.0
   * @version 0.1.0
   */
  insertTable(options?: InsertTableOptions): void {
    const rows = options?.rows ?? TABLE_DEFAULT_ROWS
    const cols = options?.cols ?? TABLE_DEFAULT_COLS
    const withHeaderRow = options?.withHeaderRow ?? true
    this.editor
      .chain()
      .focus()
      .insertTable({rows, cols, withHeaderRow})
      .run()
  }

  // -- Table state queries ----------------------------------------------------

  /** @since 0.2.0 @version 0.2.0 */
  isInTable(): boolean {
    return isInTable(this.editor.state)
  }

  /**
   * Returns true when the selection spans multiple cells that can be merged.
   * @since 0.2.0 @version 0.2.0
   */
  canMergeCells(): boolean {
    const {state} = this.editor
    if (!(state.selection instanceof CellSelection)) return false
    const rect = selectedRect(state)
    return rect.right - rect.left > 1 || rect.bottom - rect.top > 1
  }

  /**
   * Returns true when the cursor is inside a merged cell that can be split.
   * @since 0.2.0 @version 0.2.0
   */
  canSplitCell(): boolean {
    if (!isInTable(this.editor.state)) return false
    const $cell = selectionCell(this.editor.state)
    // selectionCell() returns a position immediately before the cell node
    const cell = $cell.nodeAfter
    if (!cell) return false
    const role = cell.type.spec['tableRole']
    if (role !== 'cell' && role !== 'header_cell') return false
    return (cell.attrs['colspan'] ?? 1) > 1 || (cell.attrs['rowspan'] ?? 1) > 1
  }

  /** @since 0.2.0 @version 0.2.0 */
  isTableFixedColumnWidths(): boolean {
    return this.editor.isActive('table', {fixedColumnWidths: true})
  }

  /** @since 0.2.0 @version 0.2.0 */
  isTableHeaderRow(): boolean {
    if (!isInTable(this.editor.state)) return false
    const $cell = selectionCell(this.editor.state)
    for (let d = $cell.depth; d >= 0; d--) {
      if ($cell.node(d).type.spec['tableRole'] === 'table') {
        const table = $cell.node(d)
        return rowIsHeader(TableMap.get(table), table, 0)
      }
    }
    return false
  }

  /** @since 0.2.0 @version 0.2.0 */
  isTableHeaderColumn(): boolean {
    if (!isInTable(this.editor.state)) return false
    const $cell = selectionCell(this.editor.state)
    for (let d = $cell.depth; d >= 0; d--) {
      if ($cell.node(d).type.spec['tableRole'] === 'table') {
        const table = $cell.node(d)
        return columnIsHeader(TableMap.get(table), table, 0)
      }
    }
    return false
  }

  // -- Table attributes -------------------------------------------------------

  /**
   * Sets fixed column widths on the current table.
   * @param fixed true to enable fixed column widths
   * @since 0.2.0 @version 0.2.0
   */
  setTableFixedColumnWidths(fixed: boolean): void {
    this.editor.chain().focus().updateAttributes('table', {fixedColumnWidths: fixed}).run()
  }

  // -- Table structure --------------------------------------------------------

  /** @since 0.2.0 @version 0.2.0 */
  addColumnBefore(): void {
    this.editor.chain().focus().addColumnBefore().run()
  }

  /** @since 0.2.0 @version 0.2.0 */
  addColumnAfter(): void {
    this.editor.chain().focus().addColumnAfter().run()
  }

  /** @since 0.2.0 @version 0.2.0 */
  deleteColumn(): void {
    this.editor.chain().focus().deleteColumn().run()
  }

  /** @since 0.2.0 @version 0.2.0 */
  addRowBefore(): void {
    this.editor.chain().focus().addRowBefore().run()
  }

  /** @since 0.2.0 @version 0.2.0 */
  addRowAfter(): void {
    this.editor.chain().focus().addRowAfter().run()
  }

  /** @since 0.2.0 @version 0.2.0 */
  deleteRow(): void {
    this.editor.chain().focus().deleteRow().run()
  }

  /** @since 0.2.0 @version 0.2.0 */
  mergeCells(): void {
    this.editor.chain().focus().mergeCells().run()
  }

  /** @since 0.2.0 @version 0.2.0 */
  splitCell(): void {
    this.editor.chain().focus().splitCell().run()
  }

  /** @since 0.2.0 @version 0.2.0 */
  toggleHeaderRow(): void {
    this.editor.chain().focus().toggleHeaderRow().run()
  }

  /** @since 0.2.0 @version 0.2.0 */
  toggleHeaderColumn(): void {
    this.editor.chain().focus().toggleHeaderColumn().run()
  }

  // -- Cell background --------------------------------------------------------

  /**
   * Applies a background colour to the current cell or selected cells.
   * @param colour CSS colour value (e.g. '#ffeecc')
   * @since 0.2.0 @version 0.2.0
   */
  setCellBackground(colour: string): void {
    this.editor.chain().focus().setCellAttribute('background', colour).run()
  }

  /** @since 0.2.0 @version 0.2.0 */
  clearCellBackground(): void {
    this.editor.chain().focus().setCellAttribute('background', null).run()
  }

  // -- Table layout ----------------------------------------------------------

  /**
   * Distributes all column widths equally across the table width.
   * Uses the tableWidth attribute when set; otherwise measures the rendered table width from the DOM.
   * @since 0.2.0 @version 0.2.0
   */
  distributeColumns(): void {
    const {state} = this.editor
    if (!isInTable(state)) return
    const $cell = selectionCell(state)
    let tableDepth = $cell.depth
    while (tableDepth > 0 && $cell.node(tableDepth).type.spec['tableRole'] !== 'table') tableDepth--
    if ($cell.node(tableDepth).type.spec['tableRole'] !== 'table') return

    const table = $cell.node(tableDepth)
    const tableStart = $cell.start(tableDepth)
    const map = TableMap.get(table)

    const explicitWidth: number | null = table.attrs['tableWidth'] ?? null
    let equalColWidth: number | null = null
    if (explicitWidth != null) {
      equalColWidth = Math.round(explicitWidth / map.width)
    } else {
      const firstCellDom = this.editor.view.nodeDOM(tableStart + map.positionAt(0, 0, table))
      const tableDom = firstCellDom instanceof HTMLElement ? firstCellDom.closest('table') : null
      const renderedWidth = tableDom ? (tableDom as HTMLElement).offsetWidth : 0
      if (renderedWidth > 0) equalColWidth = Math.round(renderedWidth / map.width)
    }
    if (equalColWidth == null) return

    const tr = state.tr
    const visited = new Set<number>()
    for (const cellOffset of map.map) {
      if (visited.has(cellOffset)) continue
      visited.add(cellOffset)
      const cell = table.nodeAt(cellOffset)
      if (!cell) continue
      const colspan: number = cell.attrs['colspan'] ?? 1
      const colwidth = Array.from({length: colspan}, () => equalColWidth!)
      tr.setNodeMarkup(tableStart + cellOffset, undefined, {...cell.attrs, colwidth})
    }

    this.editor.view.dispatch(tr)
  }

  /**
   * Clears the content of the current cell or all selected cells.
   * @since 0.2.0 @version 0.2.0
   */
  clearCells(): void {
    const {state} = this.editor
    if (!isInTable(state)) return
    const {selection} = state
    const paragraphType = state.schema.nodes['paragraph']
    if (!paragraphType) return

    const tr = state.tr
    let modified = false

    if (selection instanceof CellSelection) {
      selection.forEachCell((cell, pos) => {
        const start = pos + 1
        const end = pos + cell.nodeSize - 1
        if (end > start) {
          tr.replaceWith(start, end, paragraphType.create())
          modified = true
        }
      })
    } else {
      // selectionCell() returns a position immediately before the cell node
      const $cell = selectionCell(state)
      const cellNode = $cell.nodeAfter
      if (cellNode) {
        const start = $cell.pos + 1
        const end = $cell.pos + cellNode.nodeSize - 1
        if (end > start) {
          tr.replaceWith(start, end, paragraphType.create())
          modified = true
        }
      }
    }

    if (modified) this.editor.view.dispatch(tr)
  }

  // -- Column / row movement --------------------------------------------------

  /** @since 0.2.0 @version 0.2.0 */
  moveColumnLeft(): void {
    const {state} = this.editor
    if (!isInTable(state)) return
    const rect = selectedRect(state)
    if (rect.left === 0) return
    moveTableColumn({from: rect.left, to: rect.left - 1})(state, tr => this.editor.view.dispatch(tr))
  }

  /** @since 0.2.0 @version 0.2.0 */
  moveColumnRight(): void {
    const {state} = this.editor
    if (!isInTable(state)) return
    const rect = selectedRect(state)
    if (rect.left >= rect.map.width - 1) return
    moveTableColumn({from: rect.left, to: rect.left + 1})(state, tr => this.editor.view.dispatch(tr))
  }

  /** @since 0.2.0 @version 0.2.0 */
  moveRowUp(): void {
    const {state} = this.editor
    if (!isInTable(state)) return
    const rect = selectedRect(state)
    if (rect.top === 0) return
    moveTableRow({from: rect.top, to: rect.top - 1})(state, tr => this.editor.view.dispatch(tr))
  }

  /** @since 0.2.0 @version 0.2.0 */
  moveRowDown(): void {
    const {state} = this.editor
    if (!isInTable(state)) return
    const rect = selectedRect(state)
    if (rect.top >= rect.map.height - 1) return
    moveTableRow({from: rect.top, to: rect.top + 1})(state, tr => this.editor.view.dispatch(tr))
  }

  // -- Column sort ------------------------------------------------------------

  /** @since 0.2.0 @version 0.2.0 */
  sortColumnAscending(): void {
    this.doSortColumn('asc')
  }

  /** @since 0.2.0 @version 0.2.0 */
  sortColumnDescending(): void {
    this.doSortColumn('desc')
  }

  /**
   * Destroys the editor instance and removes it from the DOM.
   * Must be called when the host element is removed from the page.
   *
   * @since 0.1.0
   * @version 0.1.0
   */
  destroy(): void {
    window.removeEventListener('beforeunload', this.beforeUnloadHandler)
    this.editor.destroy()
  }

  // -- Lifecycle --------------------------------------------------------------

  private doSortColumn(direction: 'asc' | 'desc'): void {
    const {state} = this.editor
    if (!isInTable(state)) return

    const rect = selectedRect(state)
    const {table, map, tableStart} = rect
    const colIndex = rect.left
    const firstDataRow = rowIsHeader(map, table, 0) ? 1 : 0

    if (map.height - firstDataRow <= 1) return

    const rows: Array<{ index: number; text: string }> = []
    for (let r = firstDataRow; r < map.height; r++) {
      const cellOffset = map.positionAt(r, colIndex, table)
      const cell = table.nodeAt(cellOffset)
      rows.push({index: r, text: cell?.textContent ?? ''})
    }

    const collator = new Intl.Collator()
    const sorted = [...rows].sort((a, b) => {
      const cmp = collator.compare(a.text, b.text)
      return direction === 'asc' ? cmp : -cmp
    })

    if (sorted.every((r, i) => r.index === rows[i].index)) return

    const children = []
    for (let r = 0; r < firstDataRow; r++) children.push(table.child(r))
    for (const {index} of sorted) children.push(table.child(index))

    const tableNodePos = tableStart - 1
    const tr = state.tr.replaceWith(tableNodePos, tableNodePos + table.nodeSize, table.copy(Fragment.from(children)))
    this.editor.view.dispatch(tr)
  }
}
