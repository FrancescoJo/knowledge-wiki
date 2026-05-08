/**
 * TextEdit-dirty.test.ts
 *
 * Verifies Feature 1 (isDirty state detection) and Feature 2 (beforeunload guard).
 *
 * $Since: 2026-05-11
 */

import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { TextEdit } from '@src/TextEdit'
import { NodeType } from '@src/types'
import { mountElement, PARA_DOC } from './test-utils'

// -- Helpers -------------------------------------------------------------------

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function pmEditor(editor: TextEdit) { return (editor as any).editor }

function insertText(editor: TextEdit, text: string): void {
  pmEditor(editor).commands.insertContent(text)
}

function undo(editor: TextEdit): void {
  pmEditor(editor).commands.undo()
}

function fireBeforeUnload(): Event {
  const event = new Event('beforeunload', { cancelable: true })
  window.dispatchEvent(event)
  return event
}

// -- isDirty tests -------------------------------------------------------------

describe('TextEdit isDirty:', () => {
  let element: HTMLElement
  let editor: TextEdit | undefined

  beforeEach(() => { element = mountElement() })

  // noinspection DuplicatedCode: teardown is local to each suite to capture its own editor and element bindings
  afterEach(() => {
    try { editor?.destroy() } catch { /* already destroyed */ }
    editor = undefined
    element.remove()
  })

  describe('on creation with no content:', () => {
    it('should return false', () => {
      editor = new TextEdit({ element })
      expect(editor.isDirty()).toBe(false)
    })
  })

  describe('on creation with content:', () => {
    it('should return false', () => {
      editor = new TextEdit({ element, content: PARA_DOC })
      expect(editor.isDirty()).toBe(false)
    })
  })

  describe('after inserting text:', () => {
    it('should return true', () => {
      editor = new TextEdit({ element })
      insertText(editor, 'hello')
      expect(editor.isDirty()).toBe(true)
    })
  })

  describe('after undoing all changes:', () => {
    it('should return false', () => {
      editor = new TextEdit({ element })
      insertText(editor, 'hello')
      expect(editor.isDirty()).toBe(true)
      undo(editor)
      expect(editor.isDirty()).toBe(false)
    })
  })

  describe('after restoring original content via setContent:', () => {
    it('should return false', () => {
      editor = new TextEdit({ element })
      const original = editor.getContent()
      // Simulate editing with different content
      editor.setContent(PARA_DOC)
      expect(editor.isDirty()).toBe(true)
      // Restore to the original (normalised) content
      editor.setContent(original)
      expect(editor.isDirty()).toBe(false)
    })
  })

  describe('after multiple edits that return to the initial state:', () => {
    it('should return false', () => {
      editor = new TextEdit({ element, content: PARA_DOC })
      const original = editor.getContent()
      editor.setContent({ type: NodeType.Doc, content: [{ type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: 'World' }] }] })
      editor.setContent({ type: NodeType.Doc, content: [{ type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: 'Changed again' }] }] })
      expect(editor.isDirty()).toBe(true)
      editor.setContent(original)
      expect(editor.isDirty()).toBe(false)
    })
  })
})

// -- beforeunload guard tests --------------------------------------------------

describe('TextEdit beforeunload guard:', () => {
  let element: HTMLElement
  let editor: TextEdit | undefined

  beforeEach(() => { element = mountElement() })

  // noinspection DuplicatedCode: teardown is local to each suite to capture its own editor and element bindings
  afterEach(() => {
    try { editor?.destroy() } catch { /* already destroyed */ }
    editor = undefined
    element.remove()
  })

  describe('when the editor is clean:', () => {
    it('should not prevent navigation', () => {
      editor = new TextEdit({ element })
      const event = fireBeforeUnload()
      expect(event.defaultPrevented).toBe(false)
    })
  })

  describe('when the editor is dirty:', () => {
    it('should prevent navigation', () => {
      editor = new TextEdit({ element })
      insertText(editor, 'hello')
      const event = fireBeforeUnload()
      expect(event.defaultPrevented).toBe(true)
    })
  })

  describe('when dirty then restored to clean:', () => {
    it('should not prevent navigation', () => {
      editor = new TextEdit({ element })
      insertText(editor, 'hello')
      expect(editor.isDirty()).toBe(true)
      undo(editor)
      const event = fireBeforeUnload()
      expect(event.defaultPrevented).toBe(false)
    })
  })

  describe('after destroy() while dirty:', () => {
    it('should not prevent navigation', () => {
      editor = new TextEdit({ element })
      insertText(editor, 'hello')
      editor.destroy()
      editor = undefined
      const event = fireBeforeUnload()
      expect(event.defaultPrevented).toBe(false)
    })
  })
})
