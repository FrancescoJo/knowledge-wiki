/**
 * table-extensions.ts
 *
 * Custom TipTap table node extensions that add attributes for cell
 * background colour, fixed column widths, and numbered rows.
 *
 * $Since: 2026-05-09
 */

import Table from '@tiptap/extension-table'
import TableCell from '@tiptap/extension-table-cell'
import TableHeader from '@tiptap/extension-table-header'
import {Plugin, PluginKey} from '@tiptap/pm/state'

const fixedWidthsResizeBlockerKey = new PluginKey('fixedWidthsResizeBlocker')

// Must be >= prosemirror-tables' handleWidth (5 px) so this plugin's mousemove
// handler fires before the columnResizing plugin activates its drag state.
const RIGHT_EDGE_THRESHOLD = 6

/** Table extended with fixedColumnWidths, numberedRows, and tableWidth attributes. */
export const CustomTable = Table.extend({
  addProseMirrorPlugins() {
    return [
      new Plugin({
        key: fixedWidthsResizeBlockerKey,
        props: {
          handleDOMEvents: {
            mousemove(view, event) {
              const tableDom = (event.target as Element | null)?.closest?.('table')
              if (!tableDom) return false

              // Always block TipTap's column resize at the table's right edge so
              // that the ResizeTableHandle overlay can own that interaction.
              const tRect = tableDom.getBoundingClientRect()
              if (event.clientX >= tRect.right - RIGHT_EDGE_THRESHOLD) {
                event.preventDefault()
                return true
              }

              let blocked = false
              view.state.doc.descendants((node, pos) => {
                if (node.type.name !== 'table') return
                const dom = view.nodeDOM(pos)
                if (dom && (dom as Element).contains(tableDom)) {
                  blocked = node.attrs['fixedColumnWidths'] === true
                  return false
                }
                return
              })
              if (blocked) {
                event.preventDefault()
                return true
              }
              return false
            },
          },
        },
      }),
      ...(this.parent?.() ?? []),
    ]
  },

  addAttributes() {
    return {
      ...this.parent?.(),
      fixedColumnWidths: {
        default: false,
        parseHTML: el => el.getAttribute('data-fixed-column-widths') === 'true',
        renderHTML: attrs => attrs.fixedColumnWidths ? {'data-fixed-column-widths': 'true'} : {},
      },
      tableWidth: {
        default: null,
        parseHTML: el => {
          const w = (el as HTMLElement).style.width
          return w ? parseInt(w, 10) : null
        },
        renderHTML: attrs => {
          if (!attrs.tableWidth) return {}
          return {style: `width: ${attrs.tableWidth}px`}
        },
      },
    }
  },
})

/** TableCell extended with a background attribute. */
export const CustomTableCell = TableCell.extend({
  addAttributes() {
    return {
      ...this.parent?.(),
      background: {
        default: null,
        parseHTML: el => el.style.backgroundColor || null,
        renderHTML: attrs => {
          if (!attrs.background) return {}
          return {style: `background-color: ${attrs.background}`}
        },
      },
    }
  },
})

/** TableHeader extended with a background attribute. */
export const CustomTableHeader = TableHeader.extend({
  addAttributes() {
    return {
      ...this.parent?.(),
      background: {
        default: null,
        parseHTML: el => el.style.backgroundColor || null,
        renderHTML: attrs => {
          if (!attrs.background) return {}
          return {style: `background-color: ${attrs.background}`}
        },
      },
    }
  },
})
