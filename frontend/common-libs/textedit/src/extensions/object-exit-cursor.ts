/**
 * object-exit-cursor.ts
 *
 * When the text cursor is at the very end of a block object (codeBlock, table,
 * blockquote), pressing ArrowRight places a GapCursor in the gap immediately
 * after the object. From that gap:
 *   - ArrowLeft moves the text cursor back to the end of the object.
 *   - Shift+ArrowLeft selects the entire object as a NodeSelection.
 *
 * A Decoration.node class 'is-at-object-boundary' is applied to the object
 * whenever the GapCursor sits directly after it, allowing CSS to render a
 * full-height cursor at the boundary.
 *
 * $Since: 2026-05-08
 */

import {Extension} from '@tiptap/core'
import type {EditorState} from '@tiptap/pm/state'
import {NodeSelection, Plugin, PluginKey, TextSelection} from '@tiptap/pm/state'
import {GapCursor} from '@tiptap/pm/gapcursor'
import type {EditorView} from '@tiptap/pm/view'
import {Decoration, DecorationSet} from '@tiptap/pm/view'
import type {ResolvedPos} from '@tiptap/pm/model'

/** Node types that trigger the object-exit-cursor behaviour. */
export const BLOCK_OBJECT_TYPES = ['codeBlock', 'table', 'blockquote'] as const
export type BlockObjectType = typeof BLOCK_OBJECT_TYPES[number]

export interface ObjectExitCursorOptions {
  objectTypes: readonly string[]
}

const pluginKey = new PluginKey<void>('objectExitCursor')

/**
 * Returns true when $pos is at the very end of the content of the node at
 * targetDepth. The check requires:
 *   1. The cursor is at the end of its immediate parent's content.
 *   2. Every ancestor from the cursor depth down to targetDepth+1 is the last
 *      child of its own parent (i.e. no trailing siblings between the cursor
 *      and the object's closing token).
 */
function isAtVeryEndOfNode($pos: ResolvedPos, targetDepth: number): boolean {
  if ($pos.pos !== $pos.end($pos.depth)) return false
  for (let d = $pos.depth; d > targetDepth; d--) {
    if ($pos.after(d) !== $pos.end(d - 1)) return false
  }
  return true
}

/**
 * Returns true when $pos is at the very start of the content of the node at
 * targetDepth. Mirrors isAtVeryEndOfNode in the opposite direction.
 */
function isAtVeryStartOfNode($pos: ResolvedPos, targetDepth: number): boolean {
  if ($pos.pos !== $pos.start($pos.depth)) return false
  for (let d = $pos.depth; d > targetDepth; d--) {
    if ($pos.before(d) !== $pos.start(d - 1)) return false
  }
  return true
}

/**
 * Returns the depth of the innermost block-object ancestor that the cursor is
 * at the very end of, or null if no such ancestor exists.
 */
function findObjectAtEnd(
  state: EditorState,
  objectTypes: readonly string[],
): { depth: number } | null {
  const {selection} = state
  if (!(selection instanceof TextSelection)) return null
  const $pos = selection.$head

  for (let d = $pos.depth; d >= 1; d--) {
    const node = $pos.node(d)
    if (objectTypes.includes(node.type.name) && isAtVeryEndOfNode($pos, d)) {
      return {depth: d}
    }
  }
  return null
}

/**
 * When the current selection is a GapCursor immediately after a block object,
 * returns the absolute start position and nodeSize of that object. Otherwise
 * returns null.
 */
function findObjectBeforeGap(
  state: EditorState,
  objectTypes: readonly string[],
): { nodeStart: number; nodeSize: number } | null {
  const {selection} = state
  if (!(selection instanceof GapCursor)) return null
  const $pos = selection.$head
  const nodeBefore = $pos.nodeBefore
  if (!nodeBefore || !objectTypes.includes(nodeBefore.type.name)) return null
  return {
    nodeStart: $pos.pos - nodeBefore.nodeSize,
    nodeSize: nodeBefore.nodeSize,
  }
}

/**
 * When the cursor is a TextSelection at the very start of content that sits
 * immediately after a block object, returns the gap position between that
 * object and the current content. Otherwise returns null.
 */
function findObjectBeforeStart(
  state: EditorState,
  objectTypes: readonly string[],
): { gapPos: number } | null {
  const {selection} = state
  if (!(selection instanceof TextSelection)) return null
  const $pos = selection.$head

  for (let d = $pos.depth; d >= 1; d--) {
    if (!isAtVeryStartOfNode($pos, d)) break
    const nodePos = $pos.before(d)
    const nodeBefore = state.doc.resolve(nodePos).nodeBefore
    if (nodeBefore && objectTypes.includes(nodeBefore.type.name)) {
      return {gapPos: nodePos}
    }
  }
  return null
}

export const ObjectExitCursor = Extension.create<ObjectExitCursorOptions>({
  name: 'objectExitCursor',

  // Higher priority than Table (default 100) so this plugin's handleKeyDown runs
  // before prosemirror-tables', giving us first pick on boundary key events.
  priority: 200,

  addOptions() {
    return {objectTypes: BLOCK_OBJECT_TYPES}
  },

  addProseMirrorPlugins() {
    const {objectTypes} = this.options

    return [
      new Plugin({
        key: pluginKey,

        props: {
          handleKeyDown(view: EditorView, event: KeyboardEvent): boolean {
            const {state} = view

            // ArrowRight/Down at end of object → GapCursor after the object
            if ((event.key === 'ArrowRight' || event.key === 'ArrowDown') && !event.shiftKey) {
              const found = findObjectAtEnd(state, objectTypes)
              if (found) {
                const $head = (state.selection as TextSelection).$head
                const nodeStart = $head.before(found.depth)
                const node = $head.node(found.depth)
                const $gap = state.doc.resolve(nodeStart + node.nodeSize)
                view.dispatch(state.tr.setSelection(new GapCursor($gap)))
                return true
              }
            }

            // ArrowRight/Down from gap → move into next content, or insert paragraph at end of doc
            if ((event.key === 'ArrowRight' || event.key === 'ArrowDown') && !event.shiftKey) {
              const found = findObjectBeforeGap(state, objectTypes)
              if (found) {
                const afterPos = found.nodeStart + found.nodeSize
                const $next = TextSelection.findFrom(state.doc.resolve(afterPos), 1)
                if ($next) {
                  view.dispatch(state.tr.setSelection($next))
                } else {
                  const paragraphType = state.schema.nodes['paragraph']
                  if (paragraphType) {
                    const tr = state.tr.insert(afterPos, paragraphType.create())
                    tr.setSelection(TextSelection.create(tr.doc, afterPos + 1))
                    view.dispatch(tr)
                  }
                }
                return true
              }
            }

            // ArrowLeft/Up at very start of content after a block object → GapCursor
            if ((event.key === 'ArrowLeft' || event.key === 'ArrowUp') && !event.shiftKey) {
              const found = findObjectBeforeStart(state, objectTypes)
              if (found) {
                view.dispatch(
                  state.tr.setSelection(new GapCursor(state.doc.resolve(found.gapPos)))
                )
                return true
              }
            }

            // ArrowLeft from gap → text cursor at end of the object
            if (event.key === 'ArrowLeft' && !event.shiftKey) {
              const found = findObjectBeforeGap(state, objectTypes)
              if (found) {
                const $inside = state.doc.resolve(found.nodeStart + found.nodeSize - 1)
                view.dispatch(state.tr.setSelection(TextSelection.near($inside, -1)))
                return true
              }
            }

            // Shift+ArrowLeft from gap → NodeSelection of the entire object
            if (event.key === 'ArrowLeft' && event.shiftKey) {
              const found = findObjectBeforeGap(state, objectTypes)
              if (found) {
                view.dispatch(
                  state.tr.setSelection(NodeSelection.create(state.doc, found.nodeStart))
                )
                return true
              }
            }

            return false
          },

          decorations(state: EditorState): DecorationSet {
            const found = findObjectBeforeGap(state, objectTypes)
            if (!found) return DecorationSet.empty
            return DecorationSet.create(state.doc, [
              Decoration.node(found.nodeStart, found.nodeStart + found.nodeSize, {
                class: 'is-at-object-boundary',
              }),
            ])
          },
        },
      }),
    ]
  },
})
