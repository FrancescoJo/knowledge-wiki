/**
 * code-line-numbers.test.ts
 *
 * $Since: 2026-05-15
 */

import {afterEach, beforeEach, describe, expect, it} from 'vitest'
import {TextEdit} from '@src/TextEdit'
import {NodeType, type TextEditContent} from '@src/types'
import {mountElement} from '../test-utils'

// -- Fixtures ------------------------------------------------------------------

function codeDoc(text: string): TextEditContent {
  return {
    type: NodeType.Doc,
    content: [{
      type: NodeType.CodeBlock,
      attrs: {language: null},
      content: text ? [{type: NodeType.Text, text}] : [],
    }],
  }
}

// -- Helpers -------------------------------------------------------------------

function gutter(): HTMLElement | null {
  return document.body.querySelector<HTMLElement>('.code-line-numbers')
}

function lineCount(): number {
  return gutter()?.querySelectorAll('span').length ?? 0
}

// -- Tests ---------------------------------------------------------------------

describe('Code block line numbers:', () => {
  let element: HTMLElement
  let editor: TextEdit | undefined

  beforeEach(() => {
    element = mountElement()
  })

  // noinspection DuplicatedCode: teardown is local to each suite to capture its own editor and element bindings
  afterEach(() => {
    try {
      editor?.destroy()
    } catch { /* already destroyed */
    }
    editor = undefined
    element.remove()
  })

  describe('DOM structure:', () => {
    it('should wrap the code block in .code-block-with-lines', () => {
      editor = new TextEdit({element, content: codeDoc('x')})
      expect(document.body.querySelector('.code-block-with-lines')).not.toBeNull()
    })

    it('should render .code-line-numbers inside the wrapper', () => {
      editor = new TextEdit({element, content: codeDoc('x')})
      expect(gutter()).not.toBeNull()
    })

    it('should mark the gutter as aria-hidden', () => {
      editor = new TextEdit({element, content: codeDoc('x')})
      expect(gutter()?.getAttribute('aria-hidden')).toBe('true')
    })
  })

  describe('line count:', () => {
    it('should show 1 for a single-line block', () => {
      editor = new TextEdit({element, content: codeDoc('hello')})
      expect(lineCount()).toBe(1)
    })

    it('should show 3 for a three-line block', () => {
      editor = new TextEdit({element, content: codeDoc('a\nb\nc')})
      expect(lineCount()).toBe(3)
    })

    it('should not count a trailing newline as an extra line', () => {
      editor = new TextEdit({element, content: codeDoc('a\nb\n')})
      expect(lineCount()).toBe(2)
    })

    it('should show at least 1 for an empty block', () => {
      editor = new TextEdit({element, content: codeDoc('')})
      expect(lineCount()).toBe(1)
    })
  })
})
