/**
 * TextEdit.ts
 *
 * $Since: 2026-05-07
 */

import { Editor } from '@tiptap/core'
import { buildExtensions, TABLE_DEFAULT_ROWS, TABLE_DEFAULT_COLS } from './extensions'
import type { HeadingLevel, TextEditContent, TextEditHandle, TextEditOptions } from './types'

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
      extensions: buildExtensions(),
      content: options.content ?? null,
      editable: !(options.readOnly ?? false),
      onUpdate: ({ editor }) => {
        options.onChange?.(editor.getJSON() as TextEditContent)
      },
      onTransaction: () => {
        options.onSelectionChange?.(this)
      },
    })
  }

  // ── Content ────────────────────────────────────────────────────────────────

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
    this.editor.commands.setContent(content)
  }

  // ── State ──────────────────────────────────────────────────────────────────

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
  isActive(name: string, attributes?: Record<string, unknown>): boolean {
    return this.editor.isActive(name, attributes)
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

  // ── Focus ──────────────────────────────────────────────────────────────────

  /**
   * Moves keyboard focus into the editor.
   *
   * @since 0.1.0
   * @version 0.1.0
   */
  focus(): void {
    this.editor.commands.focus()
  }

  // ── Text formatting ────────────────────────────────────────────────────────

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
  toggleCode(): void {
    this.editor.chain().focus().toggleCode().run()
  }

  // ── Block formatting ───────────────────────────────────────────────────────

  /**
   * Sets the current block to a heading of the given level.
   *
   * @param level heading level between 1 and 6
   * @since 0.1.0
   * @version 0.1.0
   */
  setHeading(level: HeadingLevel): void {
    this.editor.chain().focus().setHeading({ level }).run()
  }

  /** @since 0.1.0 @version 0.1.0 */
  setParagraph(): void {
    this.editor.chain().focus().setParagraph().run()
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

  // ── Table ──────────────────────────────────────────────────────────────────

  /**
   * Inserts a table at the current cursor position.
   *
   * @since 0.1.0
   * @version 0.1.0
   */
  insertTable(): void {
    this.editor
      .chain()
      .focus()
      .insertTable({ rows: TABLE_DEFAULT_ROWS, cols: TABLE_DEFAULT_COLS, withHeaderRow: true })
      .run()
  }

  // ── Lifecycle ──────────────────────────────────────────────────────────────

  /**
   * Destroys the editor instance and removes it from the DOM.
   * Must be called when the host element is removed from the page.
   *
   * @since 0.1.0
   * @version 0.1.0
   */
  destroy(): void {
    this.editor.destroy()
  }
}
