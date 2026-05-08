/**
 * block-default-shortcuts.test.ts
 *
 * Verifies that TipTap's built-in formatting shortcuts are suppressed
 * so they do not interfere with browser defaults or the host toolbar.
 *
 * $Since: 2026-05-09
 */

import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { TextEdit } from '@src/TextEdit'
import { mountElement, pmView } from '../test-utils'

// -- Helpers -------------------------------------------------------------------

function dispatchKey(editor: TextEdit, key: string, modifiers: {
  ctrlKey?: boolean; shiftKey?: boolean; altKey?: boolean; metaKey?: boolean
} = {}): void {
  const view = pmView(editor)
  const event = new KeyboardEvent('keydown', {
    key,
    code: key,
    ctrlKey:  modifiers.ctrlKey  ?? false,
    shiftKey: modifiers.shiftKey ?? false,
    altKey:   modifiers.altKey   ?? false,
    metaKey:  modifiers.metaKey  ?? false,
    bubbles: true,
    cancelable: true,
  })
  view.dom.dispatchEvent(event)
}

// -- Tests ---------------------------------------------------------------------

describe('BlockDefaultShortcuts:', () => {
  let element: HTMLElement
  let editor: TextEdit | undefined

  beforeEach(() => { element = mountElement() })

  // noinspection DuplicatedCode: teardown is local to each suite to capture its own editor and element bindings
  afterEach(() => {
    try { editor?.destroy() } catch { /* already destroyed */ }
    editor = undefined
    element.remove()
  })

  describe('Ctrl+Shift+R (TipTap TextAlign right):', () => {
    it('should not apply right text alignment', () => {
      editor = new TextEdit({ element })
      // key='r' + shiftKey → ProseMirror resolves to Mod-Shift-r (TextAlign right)
      dispatchKey(editor, 'r', { ctrlKey: true, shiftKey: true })
      expect(editor.isActive({ textAlign: 'right' })).toBe(false)
    })
  })

  describe('Ctrl+Shift+L (TipTap TextAlign left):', () => {
    it('should not apply left text alignment', () => {
      editor = new TextEdit({ element })
      // key='l' + shiftKey → ProseMirror resolves to Mod-Shift-l (TextAlign left)
      dispatchKey(editor, 'l', { ctrlKey: true, shiftKey: true })
      expect(editor.isActive({ textAlign: 'center' })).toBe(false)
      expect(editor.isActive({ textAlign: 'right' })).toBe(false)
    })
  })

  describe('Ctrl+B (TipTap Bold):', () => {
    it('should not activate bold', () => {
      editor = new TextEdit({ element })
      // key='b' → ProseMirror resolves to Mod-b (Bold)
      dispatchKey(editor, 'b', { ctrlKey: true })
      expect(editor.isActive('bold')).toBe(false)
    })
  })

  describe('Ctrl+I (TipTap Italic):', () => {
    it('should not activate italic', () => {
      editor = new TextEdit({ element })
      // key='i' → ProseMirror resolves to Mod-i (Italic)
      dispatchKey(editor, 'i', { ctrlKey: true })
      expect(editor.isActive('italic')).toBe(false)
    })
  })

  describe('Ctrl+U (TipTap Underline):', () => {
    it('should not activate underline', () => {
      editor = new TextEdit({ element })
      // key='u' → ProseMirror resolves to Mod-u (Underline)
      dispatchKey(editor, 'u', { ctrlKey: true })
      expect(editor.isActive('underline')).toBe(false)
    })
  })

  describe('Ctrl+E (TipTap inline code):', () => {
    it('should not activate inline code', () => {
      editor = new TextEdit({ element })
      // key='e' → ProseMirror resolves to Mod-e (Code)
      dispatchKey(editor, 'e', { ctrlKey: true })
      expect(editor.isActive('code')).toBe(false)
    })
  })

  describe('Ctrl+Shift+H (TipTap Highlight):', () => {
    it('should not activate highlight', () => {
      editor = new TextEdit({ element })
      // key='h' + shiftKey → ProseMirror resolves to Mod-Shift-h (Highlight)
      dispatchKey(editor, 'h', { ctrlKey: true, shiftKey: true })
      expect(editor.isActive('highlight')).toBe(false)
    })
  })

  describe('Ctrl+Alt+1 (Heading 1 — toolbar shortcut):', () => {
    it('should still activate heading level 1', () => {
      editor = new TextEdit({ element })
      dispatchKey(editor, '1', { ctrlKey: true, altKey: true })
      expect(editor.isActive('heading', { level: 1 })).toBe(true)
    })
  })
})
