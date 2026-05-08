/**
 * block-default-shortcuts.ts
 *
 * High-priority extension that consumes TipTap's built-in formatting
 * shortcuts, leaving only the Ctrl+Alt+{key} shortcuts registered by
 * the host application's toolbar (and Mod-Alt-1..6 from the Heading
 * extension, which match the toolbar's Ctrl+Alt+1..6 bindings).
 *
 * Text-align shortcuts (Mod-Shift-l/e/r/j) are disabled at source by extending
 * TextAlign with an empty addKeyboardShortcuts(), so they are NOT listed here.
 * This lets Ctrl+Shift+R reach the browser as a hard-reload without any
 * preventDefault() being called.
 *
 * Retained shortcuts:
 *   - Mod-Alt-1..6   (Heading, matches toolbar)
 *   - Mod-z/y/Shift-Mod-z   (History — essential undo/redo)
 *   - Shift-Enter/Mod-Enter (HardBreak — basic editing)
 *   - Enter/Tab/Shift-Tab   (ListItem / TaskItem — basic editing)
 *
 * $Since: 2026-05-09
 */

import { Extension } from '@tiptap/core'

export const BlockDefaultShortcuts = Extension.create({
  name: 'blockDefaultShortcuts',

  // Run before all other extensions (default priority is 100).
  priority: 2000,

  addKeyboardShortcuts() {
    // Returning `true` marks the event as handled, suppressing lower-priority
    // extension handlers.
    const block = () => true

    return {
      // -- Bold (Mod-b) -------------------------------------------------
      'Mod-b': block,
      'Mod-B': block,

      // -- Italic (Mod-i) -----------------------------------------------
      'Mod-i': block,
      'Mod-I': block,

      // -- Underline (Mod-u) --------------------------------------------
      'Mod-u': block,
      'Mod-U': block,

      // -- Inline code (Mod-e) ------------------------------------------
      'Mod-e': block,

      // -- Strikethrough (Mod-Shift-s) ----------------------------------
      'Mod-Shift-s': block,

      // -- Bullet list (Mod-Shift-8) ------------------------------------
      'Mod-Shift-8': block,

      // -- Ordered list (Mod-Shift-7) -----------------------------------
      'Mod-Shift-7': block,

      // -- Blockquote (Mod-Shift-b) -------------------------------------
      'Mod-Shift-b': block,

      // -- Superscript (Mod-.) ------------------------------------------
      'Mod-.': block,

      // -- Subscript (Mod-,) --------------------------------------------
      'Mod-,': block,

      // -- Highlight (Mod-Shift-h) --------------------------------------
      'Mod-Shift-h': block,

      // -- CodeBlock (Mod-Alt-c) — conflicts with toolbar text-colour ---
      'Mod-Alt-c': block,
    }
  },
})
