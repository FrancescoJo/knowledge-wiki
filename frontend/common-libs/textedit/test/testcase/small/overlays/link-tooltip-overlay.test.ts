/**
 * link-tooltip-overlay.test.ts
 *
 * Tests for the LinkTooltipOverlay extension.
 *
 * $Since: 2026-05-19
 */

import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'
import {TextEdit} from '@src/TextEdit'
import {MarkType, NodeType, type TextEditContent} from '@src/types'
import {mountElement, pmView} from '../test-utils'

// -- Fixtures ------------------------------------------------------------------

const LINK_DOC: TextEditContent = {
  type: NodeType.Doc,
  content: [{
    type: NodeType.Paragraph,
    content: [{
      type: NodeType.Text,
      marks: [{type: MarkType.Link, attrs: {href: 'https://example.com'}}],
      text: 'click here',
    }],
  }],
}

// href longer than HREF_MAX_CHARS (60)
const LONG_URL = 'https://example.com/' + 'a'.repeat(60)
const LONG_URL_DOC: TextEditContent = {
  type: NodeType.Doc,
  content: [{
    type: NodeType.Paragraph,
    content: [{
      type: NodeType.Text,
      marks: [{type: MarkType.Link, attrs: {href: LONG_URL}}],
      text: 'long link',
    }],
  }],
}

const PARA_DOC: TextEditContent = {
  type: NodeType.Doc,
  content: [{type: NodeType.Paragraph, content: [{type: NodeType.Text, text: 'plain text'}]}],
}

// -- Helpers -------------------------------------------------------------------

function tooltip(): HTMLElement | null {
  return document.body.querySelector<HTMLElement>('.te-link-tooltip')
}

function editorDom(editor: TextEdit): HTMLElement {
  return pmView(editor).dom as HTMLElement
}

function linkEl(editor: TextEdit): HTMLAnchorElement | null {
  return editorDom(editor).querySelector<HTMLAnchorElement>('a[href]')
}

function dispatchMousemove(target: Element, clientX = 0, clientY = 0, ctrlKey = false): void {
  target.dispatchEvent(
    new MouseEvent('mousemove', {bubbles: true, cancelable: true, clientX, clientY, ctrlKey}),
  )
}

function dispatchMouseleave(target: Element): void {
  target.dispatchEvent(new MouseEvent('mouseleave', {bubbles: true, cancelable: true}))
}

function dispatchClick(target: Element, ctrlKey = false): void {
  target.dispatchEvent(
    new MouseEvent('click', {bubbles: true, cancelable: true, ctrlKey}),
  )
}

function dispatchKeydown(key: string): void {
  document.dispatchEvent(new KeyboardEvent('keydown', {key, bubbles: true}))
}

function dispatchKeyup(key: string): void {
  document.dispatchEvent(new KeyboardEvent('keyup', {key, bubbles: true}))
}

// -- Tests ---------------------------------------------------------------------

describe('LinkTooltipOverlay:', () => {
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

  // -- mount -----------------------------------------------------------------

  describe('on editor creation:', () => {
    it('should add the tooltip element to the document body', () => {
      editor = new TextEdit({element})
      expect(tooltip()).not.toBeNull()
    })

    it('should start hidden', () => {
      editor = new TextEdit({element})
      expect(tooltip()!.hidden).toBe(true)
    })
  })

  // -- show on link hover ---------------------------------------------------

  describe('when the mouse moves over a link:', () => {
    it('should show the tooltip', () => {
      editor = new TextEdit({element, content: LINK_DOC})
      dispatchMousemove(linkEl(editor)!)
      expect(tooltip()!.hidden).toBe(false)
    })

    it('should display the href as the tooltip text', () => {
      editor = new TextEdit({element, content: LINK_DOC})
      dispatchMousemove(linkEl(editor)!)
      expect(tooltip()!.textContent).toBe('https://example.com')
    })

    it('should truncate hrefs longer than 60 characters with an ellipsis', () => {
      editor = new TextEdit({element, content: LONG_URL_DOC})
      dispatchMousemove(linkEl(editor)!)
      const text = tooltip()!.textContent ?? ''
      expect(text.endsWith('…')).toBe(true)
      expect(text.length).toBeLessThanOrEqual(62) // 60 chars + '…'
    })
  })

  // -- hide on non-link / mouseleave ----------------------------------------

  describe('when the mouse moves off the link to a non-link element:', () => {
    it('should hide the tooltip', () => {
      editor = new TextEdit({element, content: LINK_DOC})
      dispatchMousemove(linkEl(editor)!)
      expect(tooltip()!.hidden).toBe(false)
      dispatchMousemove(editorDom(editor))
      expect(tooltip()!.hidden).toBe(true)
    })
  })

  describe('when the mouse leaves the editor:', () => {
    it('should hide the tooltip', () => {
      editor = new TextEdit({element, content: LINK_DOC})
      dispatchMousemove(linkEl(editor)!)
      expect(tooltip()!.hidden).toBe(false)
      dispatchMouseleave(editorDom(editor))
      expect(tooltip()!.hidden).toBe(true)
    })
  })

  // -- Ctrl+click -----------------------------------------------------------

  describe('when Ctrl+click is fired on a link:', () => {
    it('should call window.open with the href', () => {
      editor = new TextEdit({element, content: LINK_DOC})
      const openSpy = vi.spyOn(window, 'open').mockImplementation(() => null)
      dispatchClick(linkEl(editor)!, true)
      expect(openSpy).toHaveBeenCalledWith('https://example.com', '_blank', 'noopener,noreferrer')
      openSpy.mockRestore()
    })
  })

  describe('when a plain click (no Ctrl) is fired on a link:', () => {
    it('should not call window.open', () => {
      editor = new TextEdit({element, content: LINK_DOC})
      const openSpy = vi.spyOn(window, 'open').mockImplementation(() => null)
      dispatchClick(linkEl(editor)!, false)
      expect(openSpy).not.toHaveBeenCalled()
      openSpy.mockRestore()
    })
  })

  describe('when Ctrl+click is fired on a non-link element:', () => {
    it('should not call window.open', () => {
      editor = new TextEdit({element, content: PARA_DOC})
      const openSpy = vi.spyOn(window, 'open').mockImplementation(() => null)
      dispatchClick(editorDom(editor), true)
      expect(openSpy).not.toHaveBeenCalled()
      openSpy.mockRestore()
    })
  })

  // -- Ctrl cursor class ----------------------------------------------------

  describe('when Ctrl is pressed while hovering over a link:', () => {
    it('should add the te-ctrl-held class to the editor element', () => {
      editor = new TextEdit({element, content: LINK_DOC})
      dispatchMousemove(linkEl(editor)!)
      dispatchKeydown('Control')
      expect(editorDom(editor).classList.contains('te-ctrl-held')).toBe(true)
    })
  })

  describe('when Ctrl is released after being held:', () => {
    it('should remove the te-ctrl-held class', () => {
      editor = new TextEdit({element, content: LINK_DOC})
      dispatchMousemove(linkEl(editor)!)
      dispatchKeydown('Control')
      dispatchKeyup('Control')
      expect(editorDom(editor).classList.contains('te-ctrl-held')).toBe(false)
    })
  })

  describe('when Ctrl is pressed while NOT hovering over a link:', () => {
    it('should not add the te-ctrl-held class', () => {
      editor = new TextEdit({element, content: LINK_DOC})
      // overLink is still false — no prior mousemove over the anchor
      dispatchKeydown('Control')
      expect(editorDom(editor).classList.contains('te-ctrl-held')).toBe(false)
    })
  })

  describe('when Ctrl is held and the mouse moves over the link with ctrlKey=true:', () => {
    it('should add the te-ctrl-held class immediately via the mousemove handler', () => {
      editor = new TextEdit({element, content: LINK_DOC})
      dispatchMousemove(linkEl(editor)!, 0, 0, true)
      expect(editorDom(editor).classList.contains('te-ctrl-held')).toBe(true)
    })
  })

  // -- viewport clamping ----------------------------------------------------

  describe('when the tooltip would overflow the right edge of the viewport:', () => {
    it('should flip the tooltip to the left of the cursor', () => {
      editor = new TextEdit({element, content: LINK_DOC})
      // OFFSET_X=14, SCREEN_MARGIN=8, innerWidth=1024: threshold = 1016
      // width=200, clientX=820: x=834, 834+200=1034 > 1016 → flip
      // after flip: x = 820-200-14 = 606; Math.max(8, 606) = 606
      const spy = vi.spyOn(tooltip()!, 'getBoundingClientRect').mockReturnValue({
        width: 200, height: 20, left: 0, top: 0, right: 200, bottom: 20,
        x: 0, y: 0, toJSON: () => ({}),
      } as DOMRect)
      dispatchMousemove(linkEl(editor)!, 820, 0)
      expect(tooltip()!.style.left).toBe('606px')
      spy.mockRestore()
    })
  })

  describe('when the tooltip would overflow the bottom of the viewport:', () => {
    it('should flip the tooltip above the cursor', () => {
      editor = new TextEdit({element, content: LINK_DOC})
      // OFFSET_Y=20, SCREEN_MARGIN=8, innerHeight=768: threshold = 760
      // height=80, clientY=700: y=720, 720+80=800 > 760 → flip
      // after flip: y = 700-80-4 = 616; Math.max(8, 616) = 616
      const spy = vi.spyOn(tooltip()!, 'getBoundingClientRect').mockReturnValue({
        width: 10, height: 80, left: 0, top: 0, right: 10, bottom: 80,
        x: 0, y: 0, toJSON: () => ({}),
      } as DOMRect)
      dispatchMousemove(linkEl(editor)!, 0, 700)
      expect(tooltip()!.style.top).toBe('616px')
      spy.mockRestore()
    })
  })

  // -- destroy ---------------------------------------------------------------

  describe('when the editor is destroyed:', () => {
    it('should remove the tooltip element from the document body', () => {
      editor = new TextEdit({element})
      expect(tooltip()).not.toBeNull()
      editor.destroy()
      editor = undefined
      expect(tooltip()).toBeNull()
    })

    it('should remove the te-ctrl-held class from the editor element', () => {
      editor = new TextEdit({element, content: LINK_DOC})
      const dom = editorDom(editor)
      dispatchMousemove(linkEl(editor)!)
      dispatchKeydown('Control')
      expect(dom.classList.contains('te-ctrl-held')).toBe(true)
      editor.destroy()
      editor = undefined
      expect(dom.classList.contains('te-ctrl-held')).toBe(false)
    })
  })
})
