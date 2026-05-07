/**
 * TextEdit.test.ts
 *
 * $Since: 2026-05-07
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { TextEdit } from '@src/TextEdit'
import type { TextEditContent } from '@src/types'

// ── Test data ─────────────────────────────────────────────────────────────────

const EMPTY_DOC: TextEditContent = {
  type: 'doc',
  content: [{ type: 'paragraph' }],
}

const PARAGRAPH_DOC: TextEditContent = {
  type: 'doc',
  content: [{
    type: 'paragraph',
    content: [{ type: 'text', text: 'Hello, world.' }],
  }],
}

const HEADING_2_DOC: TextEditContent = {
  type: 'doc',
  content: [{
    type: 'heading',
    attrs: { level: 2 },
    content: [{ type: 'text', text: 'Section heading' }],
  }],
}

// ── Helpers ───────────────────────────────────────────────────────────────────

function mountElement(): HTMLElement {
  const el = document.createElement('div')
  document.body.appendChild(el)
  return el
}

function proseMirrorEl(host: HTMLElement): Element | null {
  return host.querySelector('.ProseMirror')
}

function isEditable(host: HTMLElement): boolean {
  return proseMirrorEl(host)?.getAttribute('contenteditable') === 'true'
}

function docNodes(editor: TextEdit): Record<string, unknown>[] {
  return (editor.getContent().content as Record<string, unknown>[]) ?? []
}

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('TextEdit:', () => {
  let element: HTMLElement
  let editor: TextEdit | undefined

  beforeEach(() => {
    element = mountElement()
  })

  afterEach(() => {
    try { editor?.destroy() } catch { /* already destroyed */ }
    editor = undefined
    element.remove()
  })

  // ── constructor ─────────────────────────────────────────────────────────────

  describe('constructor:', () => {
    describe('when called with a DOM element and no content:', () => {
      it('should attach the editor to the host element', () => {
        editor = new TextEdit({ element })
        expect(proseMirrorEl(element)).not.toBeNull()
      })

      it('should be editable by default', () => {
        editor = new TextEdit({ element })
        expect(isEditable(element)).toBe(true)
      })

      it('should produce a document node as initial content', () => {
        editor = new TextEdit({ element })
        expect(editor.getContent().type).toBe('doc')
      })
    })

    describe('when called with initial content:', () => {
      it('should load the provided content into the document', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        expect(editor.getContent()).toMatchObject(PARAGRAPH_DOC)
      })
    })

    describe('when called with readOnly set to true:', () => {
      it('should initialise the editor as non-editable', () => {
        editor = new TextEdit({ element, readOnly: true })
        expect(isEditable(element)).toBe(false)
      })
    })
  })

  // ── getContent ──────────────────────────────────────────────────────────────

  describe('getContent:', () => {
    describe('when content was provided at construction:', () => {
      it('should return a JSON object matching the initial content', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        expect(editor.getContent()).toMatchObject(PARAGRAPH_DOC)
      })
    })

    describe('when no content was provided at construction:', () => {
      it('should return a document containing an empty paragraph node', () => {
        editor = new TextEdit({ element })
        expect(docNodes(editor)[0]).toMatchObject({ type: 'paragraph' })
      })
    })
  })

  // ── setContent ──────────────────────────────────────────────────────────────

  describe('setContent:', () => {
    describe('when called with a new document:', () => {
      it('should replace the entire document with the given content', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.setContent(HEADING_2_DOC)
        expect(editor.getContent()).toMatchObject(HEADING_2_DOC)
      })
    })
  })

  // ── setReadOnly ─────────────────────────────────────────────────────────────

  describe('setReadOnly:', () => {
    describe('when passed true:', () => {
      it('should mark the editor as non-editable', () => {
        editor = new TextEdit({ element })
        editor.setReadOnly(true)
        expect(isEditable(element)).toBe(false)
      })
    })

    describe('when passed false after being made read-only:', () => {
      it('should restore editability', () => {
        editor = new TextEdit({ element, readOnly: true })
        editor.setReadOnly(false)
        expect(isEditable(element)).toBe(true)
      })
    })
  })

  // ── isActive ────────────────────────────────────────────────────────────────

  describe('isActive:', () => {
    describe('when the current block is a heading after setHeading is called:', () => {
      it('should return true for heading with the matching level', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.setHeading(2)
        expect(editor.isActive('heading', { level: 2 })).toBe(true)
      })

      it('should return false for heading with a different level', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.setHeading(2)
        expect(editor.isActive('heading', { level: 1 })).toBe(false)
      })
    })

    describe('when the current block is a plain paragraph:', () => {
      it('should return false for heading', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        expect(editor.isActive('heading')).toBe(false)
      })
    })

    describe('when bold is toggled on without a selection:', () => {
      it('should return true for bold', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.toggleBold()
        expect(editor.isActive('bold')).toBe(true)
      })
    })
  })

  // ── isFocused ───────────────────────────────────────────────────────────────

  describe('isFocused:', () => {
    describe('when the editor has not received focus:', () => {
      it('should return false', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        expect(editor.isFocused()).toBe(false)
      })
    })
  })

  // ── focus ───────────────────────────────────────────────────────────────────

  describe('focus:', () => {
    describe('when called:', () => {
      it('should report the editor as focused', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.focus()
        expect(editor.isFocused()).toBe(true)
      })
    })
  })

  // ── toggleBold ──────────────────────────────────────────────────────────────

  describe('toggleBold:', () => {
    describe('when called once without an active selection:', () => {
      it('should activate the bold stored mark', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.toggleBold()
        expect(editor.isActive('bold')).toBe(true)
      })
    })

    describe('when called twice without an active selection:', () => {
      it('should deactivate the bold stored mark', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.toggleBold()
        editor.toggleBold()
        expect(editor.isActive('bold')).toBe(false)
      })
    })
  })

  // ── toggleItalic ────────────────────────────────────────────────────────────

  describe('toggleItalic:', () => {
    describe('when called once without an active selection:', () => {
      it('should activate the italic stored mark', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.toggleItalic()
        expect(editor.isActive('italic')).toBe(true)
      })
    })

    describe('when called twice without an active selection:', () => {
      it('should deactivate the italic stored mark', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.toggleItalic()
        editor.toggleItalic()
        expect(editor.isActive('italic')).toBe(false)
      })
    })
  })

  // ── toggleStrike ────────────────────────────────────────────────────────────

  describe('toggleStrike:', () => {
    describe('when called once without an active selection:', () => {
      it('should activate the strike stored mark', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.toggleStrike()
        expect(editor.isActive('strike')).toBe(true)
      })
    })

    describe('when called twice without an active selection:', () => {
      it('should deactivate the strike stored mark', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.toggleStrike()
        editor.toggleStrike()
        expect(editor.isActive('strike')).toBe(false)
      })
    })
  })

  // ── toggleCode ──────────────────────────────────────────────────────────────

  describe('toggleCode:', () => {
    describe('when called once without an active selection:', () => {
      it('should activate the inline code stored mark', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.toggleCode()
        expect(editor.isActive('code')).toBe(true)
      })
    })

    describe('when called twice without an active selection:', () => {
      it('should deactivate the inline code stored mark', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.toggleCode()
        editor.toggleCode()
        expect(editor.isActive('code')).toBe(false)
      })
    })
  })

  // ── setHeading ──────────────────────────────────────────────────────────────

  describe('setHeading:', () => {
    describe('when called with level 1:', () => {
      it('should convert the current block to a level-1 heading', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.setHeading(1)
        expect(editor.isActive('heading', { level: 1 })).toBe(true)
      })
    })

    describe('when called with level 2:', () => {
      it('should convert the current block to a level-2 heading', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.setHeading(2)
        expect(editor.isActive('heading', { level: 2 })).toBe(true)
      })
    })

    describe('when called with level 3:', () => {
      it('should convert the current block to a level-3 heading', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.setHeading(3)
        expect(editor.isActive('heading', { level: 3 })).toBe(true)
      })
    })
  })

  // ── setParagraph ────────────────────────────────────────────────────────────

  describe('setParagraph:', () => {
    describe('when the current block is a heading:', () => {
      it('should convert the block to a paragraph node', () => {
        editor = new TextEdit({ element, content: HEADING_2_DOC })
        editor.setParagraph()
        expect(editor.isActive('heading')).toBe(false)
        expect(docNodes(editor)[0]).toMatchObject({ type: 'paragraph' })
      })
    })
  })

  // ── toggleBulletList ────────────────────────────────────────────────────────

  describe('toggleBulletList:', () => {
    describe('when called on a plain paragraph:', () => {
      it('should wrap the block in a bullet list', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.toggleBulletList()
        expect(editor.isActive('bulletList')).toBe(true)
      })
    })

    describe('when called on an existing bullet list item:', () => {
      it('should unwrap the bullet list', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.toggleBulletList()
        editor.toggleBulletList()
        expect(editor.isActive('bulletList')).toBe(false)
      })
    })
  })

  // ── toggleOrderedList ───────────────────────────────────────────────────────

  describe('toggleOrderedList:', () => {
    describe('when called on a plain paragraph:', () => {
      it('should wrap the block in an ordered list', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.toggleOrderedList()
        expect(editor.isActive('orderedList')).toBe(true)
      })
    })

    describe('when called on an existing ordered list item:', () => {
      it('should unwrap the ordered list', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.toggleOrderedList()
        editor.toggleOrderedList()
        expect(editor.isActive('orderedList')).toBe(false)
      })
    })
  })

  // ── toggleTaskList ──────────────────────────────────────────────────────────

  describe('toggleTaskList:', () => {
    describe('when called on a plain paragraph:', () => {
      it('should wrap the block in a task list', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.toggleTaskList()
        expect(editor.isActive('taskList')).toBe(true)
      })
    })

    describe('when called on an existing task list item:', () => {
      it('should unwrap the task list', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.toggleTaskList()
        editor.toggleTaskList()
        expect(editor.isActive('taskList')).toBe(false)
      })
    })
  })

  // ── toggleBlockquote ────────────────────────────────────────────────────────

  describe('toggleBlockquote:', () => {
    describe('when called on a plain paragraph:', () => {
      it('should wrap the block in a blockquote', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.toggleBlockquote()
        expect(editor.isActive('blockquote')).toBe(true)
      })
    })

    describe('when called on an existing blockquote:', () => {
      it('should unwrap the blockquote', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.toggleBlockquote()
        editor.toggleBlockquote()
        expect(editor.isActive('blockquote')).toBe(false)
      })
    })
  })

  // ── toggleCodeBlock ─────────────────────────────────────────────────────────

  describe('toggleCodeBlock:', () => {
    describe('when called on a plain paragraph:', () => {
      it('should convert the block to a code block', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.toggleCodeBlock()
        expect(editor.isActive('codeBlock')).toBe(true)
      })
    })

    describe('when called on an existing code block:', () => {
      it('should convert the block back to a paragraph', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.toggleCodeBlock()
        editor.toggleCodeBlock()
        expect(editor.isActive('codeBlock')).toBe(false)
      })
    })
  })

  // ── insertTable ─────────────────────────────────────────────────────────────

  describe('insertTable:', () => {
    describe('when called on a document with a paragraph:', () => {
      it('should insert a table node into the document', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.insertTable()
        const hasTable = docNodes(editor).some(node => node['type'] === 'table')
        expect(hasTable).toBe(true)
      })
    })
  })

  // ── destroy ─────────────────────────────────────────────────────────────────

  describe('destroy:', () => {
    describe('when called:', () => {
      it('should remove the editor from the host element', () => {
        editor = new TextEdit({ element })
        editor.destroy()
        expect(proseMirrorEl(element)).toBeNull()
        editor = undefined
      })
    })
  })

  // ── onChange callback ───────────────────────────────────────────────────────

  describe('onChange callback:', () => {
    describe('when the document is replaced via setContent:', () => {
      it('should call onChange with an object containing the updated content', () => {
        const onChange = vi.fn()
        editor = new TextEdit({ element, content: EMPTY_DOC, onChange })
        editor.setContent(PARAGRAPH_DOC)
        expect(onChange).toHaveBeenCalledWith(expect.objectContaining({ type: 'doc' }))
      })
    })
  })

  // ── onSelectionChange callback ──────────────────────────────────────────────

  describe('onSelectionChange callback:', () => {
    describe('when a transaction is dispatched:', () => {
      it('should call onSelectionChange with a handle that exposes isActive', () => {
        const onSelectionChange = vi.fn()
        editor = new TextEdit({ element, content: PARAGRAPH_DOC, onSelectionChange })
        editor.setContent(HEADING_2_DOC)
        expect(onSelectionChange).toHaveBeenCalled()
        const handle = onSelectionChange.mock.calls[0][0] as { isActive: unknown }
        expect(typeof handle.isActive).toBe('function')
      })
    })
  })
})
