/**
 * cell-selection-escape.ts
 *
 * Pressing Escape while a CellSelection is active collapses the selection
 * back to the text cursor position that existed immediately before the
 * CellSelection was entered.
 *
 * A ProseMirror plugin captures the TextSelection position on every
 * TextSelection → CellSelection transition. The ESC handler reads that
 * saved position and restores it, so:
 *   - Column-handle click then ESC → cursor returns to the cell that was
 *     active before the handle was clicked (not the first cell of the column).
 *   - Row-handle click then ESC → cursor returns to the cell that was active
 *     before the handle was clicked (not the first cell of the row).
 *   - Mouse-drag selection then ESC → cursor returns to where the drag
 *     started.
 *
 * $Since: 2026-05-14
 */

import {Extension} from '@tiptap/core'
import {Plugin, PluginKey, TextSelection} from '@tiptap/pm/state'
import {CellSelection} from '@tiptap/pm/tables'

const pluginKey = new PluginKey<number | null>('cellSelectionEscape')

export const CellSelectionEscape = Extension.create({
  name: 'cellSelectionEscape',

  addKeyboardShortcuts() {
    return {
      Escape: () => {
        const {state} = this.editor
        if (!(state.selection instanceof CellSelection)) return false

        const savedPos = pluginKey.getState(state)
        const docSize = state.doc.content.size
        const targetPos =
          savedPos != null && savedPos > 0 && savedPos < docSize
            ? savedPos
            : state.selection.$anchorCell.pos + 1

        this.editor.view.dispatch(
          state.tr.setSelection(TextSelection.near(state.doc.resolve(targetPos)))
        )
        return true
      },
    }
  },

  addProseMirrorPlugins() {
    return [
      new Plugin({
        key: pluginKey,
        state: {
          init(): number | null {
            return null
          },
          apply(tr, prev, oldState) {
            // Entering CellSelection from a TextSelection — save the cursor position.
            if (
              oldState.selection instanceof TextSelection &&
              tr.selectionSet &&
              tr.selection instanceof CellSelection
            ) {
              return oldState.selection.from
            }
            // Leaving CellSelection — clear the saved position.
            if (!(tr.selection instanceof CellSelection)) {
              return null
            }
            // Still in CellSelection — preserve the saved position.
            return prev
          },
        },
      }),
    ]
  },
})
