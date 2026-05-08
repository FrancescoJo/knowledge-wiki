/**
 * code-language-overlay.test.ts
 *
 * $Since: 2026-05-15
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { TextSelection } from '@tiptap/pm/state'
import { TextEdit } from '@src/TextEdit'
import { NodeType, type TextEditContent } from '@src/types'
import { mountElement, pmView } from '../test-utils'

// -- Fixtures ------------------------------------------------------------------

const PARA_DOC: TextEditContent = {
  type: NodeType.Doc,
  content: [{ type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: 'outside' }] }],
}

// codeBlock at pos 0 (nodeSize=3 for single char), paragraph follows.
const CODE_DOC: TextEditContent = {
  type: NodeType.Doc,
  content: [
    {
      type: NodeType.CodeBlock,
      attrs: { language: 'javascript' },
      content: [{ type: NodeType.Text, text: 'x' }],
    },
    { type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: 'outside' }] },
  ],
}

// codeBlock with no language set.
const CODE_PLAIN_DOC: TextEditContent = {
  type: NodeType.Doc,
  content: [
    {
      type: NodeType.CodeBlock,
      attrs: { language: null },
      content: [{ type: NodeType.Text, text: 'x' }],
    },
    { type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: 'outside' }] },
  ],
}

// -- Helpers -------------------------------------------------------------------

/** Place cursor inside the codeBlock (pos 2 = inside the text "x"). */
function setCursorInCode(editor: TextEdit): void {
  const view = pmView(editor)
  view.dispatch(view.state.tr.setSelection(TextSelection.create(view.state.doc, 2)))
}

/** Place cursor in the paragraph that follows the code block. */
function setCursorOutside(editor: TextEdit): void {
  const view = pmView(editor)
  // codeBlock nodeSize = 3 (open + "x" + close), so paragraph starts at pos 3.
  // pos 4 = inside paragraph text.
  view.dispatch(view.state.tr.setSelection(TextSelection.create(view.state.doc, 4)))
}

function overlay(): HTMLElement | null {
  return document.body.querySelector<HTMLElement>('.te-code-lang')
}

function triggerBtn(): HTMLButtonElement | null {
  return document.body.querySelector<HTMLButtonElement>('.te-code-lang__btn')
}

function menu(): HTMLElement | null {
  return document.body.querySelector<HTMLElement>('.te-code-lang__menu')
}

function menuItem(label: string): HTMLButtonElement | null {
  const items = document.body.querySelectorAll<HTMLButtonElement>('.te-code-lang__item')
  for (const item of items) {
    if (item.textContent === label) return item
  }
  return null
}

function clickBtn(btn: HTMLButtonElement): void {
  btn.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true }))
}

function getCodeBlockLanguage(editor: TextEdit): string | null {
  const view = pmView(editor)
  return view.state.doc.firstChild?.attrs['language'] ?? null
}

// -- Tests ---------------------------------------------------------------------

describe('CodeLanguageOverlay:', () => {
  // noinspection DuplicatedCode: similar pattern for overlay menus
  let element: HTMLElement
  let editor: TextEdit | undefined

  beforeEach(() => { element = mountElement() })

  afterEach(() => {
    try { editor?.destroy() } catch { /* already destroyed */ }
    editor = undefined
    element.remove()
    vi.restoreAllMocks()
  })

  // -- mount -----------------------------------------------------------------

  describe('when TextEdit is created:', () => {
    it('should add the overlay element to the DOM', () => {
      editor = new TextEdit({ element })
      expect(overlay()).not.toBeNull()
    })
  })

  // -- visibility ------------------------------------------------------------

  describe('visibility:', () => {
    describe('when the cursor is outside a code block:', () => {
      it('should be hidden', () => {
        editor = new TextEdit({ element, content: PARA_DOC })
        expect(overlay()!.style.display).toBe('none')
      })
    })

    describe('when the cursor is placed inside a code block:', () => {
      it('should become visible', () => {
        editor = new TextEdit({ element, content: CODE_DOC })
        setCursorInCode(editor)
        expect(overlay()!.style.display).toBe('')
      })
    })

    describe('when the cursor moves from inside to outside a code block:', () => {
      it('should become hidden', () => {
        editor = new TextEdit({ element, content: CODE_DOC })
        setCursorInCode(editor)
        expect(overlay()!.style.display).toBe('')
        setCursorOutside(editor)
        expect(overlay()!.style.display).toBe('none')
      })
    })

    describe('when the editor loses focus:', () => {
      it('should become hidden', () => {
        editor = new TextEdit({ element, content: CODE_DOC })
        setCursorInCode(editor)
        expect(overlay()!.style.display).toBe('')
        pmView(editor).dom.dispatchEvent(new Event('blur'))
        expect(overlay()!.style.display).toBe('none')
      })
    })

    describe('when the editor loses focus while the menu is open:', () => {
      it('should keep the overlay visible so the search input can receive focus', () => {
        editor = new TextEdit({ element, content: CODE_DOC })
        setCursorInCode(editor)
        clickBtn(triggerBtn()!)   // open the menu
        pmView(editor).dom.dispatchEvent(new Event('blur'))
        // onEditorBlur: menuOpen===true → return without hiding
        expect(overlay()!.style.display).toBe('')
      })
    })
  })

  // -- trigger button label --------------------------------------------------

  describe('trigger button label:', () => {
    describe('when the code block has language="javascript":', () => {
      it('should show "JavaScript ▾"', () => {
        editor = new TextEdit({ element, content: CODE_DOC })
        setCursorInCode(editor)
        expect(triggerBtn()!.textContent).toBe('JavaScript ▾')
      })
    })

    describe('when the code block has no language (null):', () => {
      it('should show "Plain text ▾"', () => {
        editor = new TextEdit({ element, content: CODE_PLAIN_DOC })
        setCursorInCode(editor)
        expect(triggerBtn()!.textContent).toBe('Plain text ▾')
      })
    })

    describe('when the code block has an unrecognised language (not in the built-in list):', () => {
      it('should fall back to "Plain text ▾"', () => {
        // language: 'cobol' is not in the built-in list, so allItems.find() returns
        // undefined and the ?? PLAIN fallback fires.
        const UNKNOWN_LANG_DOC: TextEditContent = {
          type: NodeType.Doc,
          content: [{
            type: NodeType.CodeBlock,
            attrs: { language: 'cobol' },
            content: [{ type: NodeType.Text, text: 'x' }],
          }],
        }
        editor = new TextEdit({ element, content: UNKNOWN_LANG_DOC })
        setCursorInCode(editor)
        expect(triggerBtn()!.textContent).toBe('Plain text ▾')
      })
    })
  })

  // -- updatePosition catch ---------------------------------------------------

  describe('when getBoundingClientRect throws during position update:', () => {
    it('should silently absorb the error', () => {
      editor = new TextEdit({ element, content: CODE_DOC })
      const spy = vi.spyOn(HTMLElement.prototype, 'getBoundingClientRect').mockImplementation(() => {
        throw new Error('mock getBoundingClientRect error')
      })
      // Triggering the update with the spy active causes updatePosition to throw
      // internally, which the catch block absorbs without propagating.
      setCursorInCode(editor)
      spy.mockRestore()
      // Overlay still displayed (catch didn't crash); no assertion needed beyond
      // "no error thrown" which vitest enforces automatically.
      expect(overlay()!.style.display).toBe('')
    })
  })

  // -- menu ------------------------------------------------------------------

  describe('dropdown menu:', () => {
    describe('when the trigger button is clicked:', () => {
      it('should open the menu', () => {
        editor = new TextEdit({ element, content: CODE_DOC })
        setCursorInCode(editor)
        expect(menu()!.style.display).toBe('none')
        clickBtn(triggerBtn()!)
        expect(menu()!.style.display).toBe('')
      })
    })

    describe('when the trigger button is clicked again (menu is open):', () => {
      it('should close the menu', () => {
        editor = new TextEdit({ element, content: CODE_DOC })
        setCursorInCode(editor)
        clickBtn(triggerBtn()!)
        expect(menu()!.style.display).toBe('')
        clickBtn(triggerBtn()!)
        expect(menu()!.style.display).toBe('none')
      })
    })

    describe('menu contents:', () => {
      it('should include "Plain text" as the first item', () => {
        editor = new TextEdit({ element, content: CODE_DOC })
        setCursorInCode(editor)
        clickBtn(triggerBtn()!)
        const items = document.body.querySelectorAll<HTMLButtonElement>('.te-code-lang__item')
        expect(items[0].textContent).toBe('Plain text')
      })

      it('should include "JavaScript", "TypeScript", and "Markdown" items', () => {
        editor = new TextEdit({ element, content: CODE_DOC })
        setCursorInCode(editor)
        clickBtn(triggerBtn()!)
        expect(menuItem('JavaScript')).not.toBeNull()
        expect(menuItem('TypeScript')).not.toBeNull()
        expect(menuItem('Markdown')).not.toBeNull()
      })
    })

    describe('active item state:', () => {
      it('should mark the current language item as active', () => {
        editor = new TextEdit({ element, content: CODE_DOC })
        setCursorInCode(editor)
        clickBtn(triggerBtn()!)
        expect(menuItem('JavaScript')!.classList.contains('is-active')).toBe(true)
        expect(menuItem('TypeScript')!.classList.contains('is-active')).toBe(false)
      })

      it('should mark "Plain text" as active when no language is set', () => {
        editor = new TextEdit({ element, content: CODE_PLAIN_DOC })
        setCursorInCode(editor)
        clickBtn(triggerBtn()!)
        const items = document.body.querySelectorAll<HTMLButtonElement>('.te-code-lang__item')
        expect(items[0].classList.contains('is-active')).toBe(true)
      })
    })

    describe('when the cursor exits a code block while the menu is open:', () => {
      it('should close the menu', () => {
        editor = new TextEdit({ element, content: CODE_DOC })
        setCursorInCode(editor)
        clickBtn(triggerBtn()!)
        expect(menu()!.style.display).toBe('')
        setCursorOutside(editor)
        expect(menu()!.style.display).toBe('none')
      })
    })
  })

  // -- language selection -----------------------------------------------------

  describe('language selection:', () => {
    describe('clicking "TypeScript" in the menu:', () => {
      it('should set language="typescript" on the code block', () => {
        editor = new TextEdit({ element, content: CODE_DOC })
        setCursorInCode(editor)
        clickBtn(triggerBtn()!)
        clickBtn(menuItem('TypeScript')!)
        expect(getCodeBlockLanguage(editor)).toBe('typescript')
      })

      it('should close the menu after selection', () => {
        editor = new TextEdit({ element, content: CODE_DOC })
        setCursorInCode(editor)
        clickBtn(triggerBtn()!)
        clickBtn(menuItem('TypeScript')!)
        expect(menu()!.style.display).toBe('none')
      })

      it('should update the trigger button label to "TypeScript ▾"', () => {
        editor = new TextEdit({ element, content: CODE_DOC })
        setCursorInCode(editor)
        clickBtn(triggerBtn()!)
        clickBtn(menuItem('TypeScript')!)
        expect(triggerBtn()!.textContent).toBe('TypeScript ▾')
      })
    })

    describe('clicking "Plain text" in the menu:', () => {
      it('should set language to null on the code block', () => {
        editor = new TextEdit({ element, content: CODE_DOC })
        setCursorInCode(editor)
        clickBtn(triggerBtn()!)
        const items = document.body.querySelectorAll<HTMLButtonElement>('.te-code-lang__item')
        clickBtn(items[0]!) // Plain text
        expect(getCodeBlockLanguage(editor)).toBeNull()
      })
    })
  })

  // -- focus preservation ----------------------------------------------------

  describe('mousedown on overlay container:', () => {
    it('should call ev.preventDefault() to keep editor focus', () => {
      editor = new TextEdit({ element, content: CODE_DOC })
      setCursorInCode(editor)
      const ev = new MouseEvent('mousedown', { bubbles: true, cancelable: true })
      overlay()!.dispatchEvent(ev)
      expect(ev.defaultPrevented).toBe(true)
    })
  })

  describe('mousedown on dropdown menu:', () => {
    it('should call ev.preventDefault() to keep editor focus', () => {
      editor = new TextEdit({ element, content: CODE_DOC })
      setCursorInCode(editor)
      clickBtn(triggerBtn()!)
      const ev = new MouseEvent('mousedown', { bubbles: true, cancelable: true })
      menu()!.dispatchEvent(ev)
      expect(ev.defaultPrevented).toBe(true)
    })
  })

  // -- search ----------------------------------------------------------------

  describe('search:', () => {
    function searchEl(): HTMLElement | null {
      return document.body.querySelector<HTMLElement>('.te-code-lang__search')
    }

    function typeKey(ed: TextEdit, key: string): void {
      pmView(ed).dom.dispatchEvent(
        new KeyboardEvent('keydown', { key, bubbles: true, cancelable: true }),
      )
    }

    function expectFabVisible(): void {
      const items = [...document.body.querySelectorAll<HTMLButtonElement>('.te-code-lang__item')]
      const visible = items.filter(b => b.style.display !== 'none')
      expect(visible.some(b => b.textContent === 'JavaScript')).toBe(true)
      expect(visible.some(b => b.textContent === 'TypeScript')).toBe(false)
    }

    describe('when the menu is open:', () => {
      it('should show the search display element', () => {
        editor = new TextEdit({ element, content: CODE_DOC })
        setCursorInCode(editor)
        clickBtn(triggerBtn()!)
        expect(searchEl()).not.toBeNull()
      })

      it('should update the search display text when typing', () => {
        editor = new TextEdit({ element, content: CODE_DOC })
        setCursorInCode(editor)
        clickBtn(triggerBtn()!)
        typeKey(editor, 'p')
        typeKey(editor, 'y')
        expect(searchEl()!.textContent).toBe('py')
      })

      it('should filter items to those whose labels contain the query', () => {
        editor = new TextEdit({ element, content: CODE_DOC })
        setCursorInCode(editor)
        clickBtn(triggerBtn()!)
        typeKey(editor, 'j')
        expectFabVisible()
      })

      it('should remove a character on Backspace', () => {
        editor = new TextEdit({ element, content: CODE_DOC })
        setCursorInCode(editor)
        clickBtn(triggerBtn()!)
        typeKey(editor, 'p')
        typeKey(editor, 'y')
        typeKey(editor, 'Backspace')
        expect(searchEl()!.textContent).toBe('p')
      })

      it('should close the menu on Escape', () => {
        editor = new TextEdit({ element, content: CODE_DOC })
        setCursorInCode(editor)
        clickBtn(triggerBtn()!)
        typeKey(editor, 'Escape')
        expect(menu()!.style.display).toBe('none')
      })

      it('should clear the search query when the menu is reopened', () => {
        editor = new TextEdit({ element, content: CODE_DOC })
        setCursorInCode(editor)
        clickBtn(triggerBtn()!)
        typeKey(editor, 'j')
        clickBtn(triggerBtn()!) // close
        clickBtn(triggerBtn()!) // reopen
        expect(searchEl()!.textContent).toBe('')
      })

      it('should restore all items after the search is cleared on reopen', () => {
        editor = new TextEdit({ element, content: CODE_DOC })
        setCursorInCode(editor)
        clickBtn(triggerBtn()!)
        typeKey(editor, 'j')
        clickBtn(triggerBtn()!) // close
        clickBtn(triggerBtn()!) // reopen
        const items = [...document.body.querySelectorAll<HTMLButtonElement>('.te-code-lang__item')]
        const visible = items.filter(b => b.style.display !== 'none')
        expect(visible.some(b => b.textContent === 'TypeScript')).toBe(true)
      })
    })

    describe('when the search input receives an input event directly:', () => {
      it('should filter items by the typed value', () => {
        editor = new TextEdit({ element, content: CODE_DOC })
        setCursorInCode(editor)
        clickBtn(triggerBtn()!)
        const searchInput = document.body.querySelector<HTMLInputElement>('.te-code-lang__search-input')!
        searchInput.value = 'java'
        searchInput.dispatchEvent(new Event('input'))
        expectFabVisible()
      })
    })

    describe('when Escape is pressed directly on the search input while the menu is open:', () => {
      it('should close the menu via the search-input keydown listener', () => {
        editor = new TextEdit({ element, content: CODE_DOC })
        setCursorInCode(editor)
        clickBtn(triggerBtn()!)
        const searchInput = document.body.querySelector<HTMLInputElement>('.te-code-lang__search-input')!
        searchInput.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true, cancelable: true }))
        expect(menu()!.style.display).toBe('none')
      })
    })

    describe('when the menu is closed:', () => {
      it('should not consume printable keystrokes', () => {
        editor = new TextEdit({ element, content: CODE_DOC })
        setCursorInCode(editor)
        const ev = new KeyboardEvent('keydown', { key: 'j', bubbles: true, cancelable: true })
        pmView(editor).dom.dispatchEvent(ev)
        expect(ev.defaultPrevented).toBe(false)
      })
    })

    describe('when a non-printable, non-navigation key is pressed while the menu is open:', () => {
      it('should not consume the event (handleKey returns false)', () => {
        editor = new TextEdit({ element, content: CODE_DOC })
        setCursorInCode(editor)
        clickBtn(triggerBtn()!)
        // Tab is non-printable, non-Escape, non-Backspace → handleKey line 226: return false
        const ev = new KeyboardEvent('keydown', { key: 'Tab', bubbles: true, cancelable: true })
        pmView(editor).dom.dispatchEvent(ev)
        expect(ev.defaultPrevented).toBe(false)
      })
    })
  })

  // -- extra languages -------------------------------------------------------

  describe('when extra languages are passed via TextEditOptions.codeLanguages:', () => {
    it('should include the extra language in the dropdown menu', async () => {
      const { default: cssGrammar } = await import('highlight.js/lib/languages/css')
      editor = new TextEdit({
        element,
        content: CODE_DOC,
        codeLanguages: [{ label: 'CSS', value: 'css', grammar: cssGrammar }],
      })
      setCursorInCode(editor)
      clickBtn(triggerBtn()!)
      expect(menuItem('CSS')).not.toBeNull()
    })
  })

  // -- menu position (upward) ------------------------------------------------

  describe('when the menu would overflow the viewport bottom:', () => {
    it('should open the menu upward instead of downward', () => {
      editor = new TextEdit({ element, content: CODE_DOC })
      setCursorInCode(editor)
      // Stub getBoundingClientRect so the button appears near the bottom of the
      // viewport (window.innerHeight=768 in jsdom).  menuH=0 in jsdom, so:
      // fitsBelow = 800 + 2 + 0 = 802 > 768 → false → opens upward
      // expected top = btnRect.top − 2 − menuH = 790 − 2 − 0 = 788
      vi.spyOn(triggerBtn()!, 'getBoundingClientRect').mockReturnValue({
        top: 790, bottom: 800, left: 100, right: 200, width: 100, height: 10,
        x: 100, y: 790, toJSON: () => ({}),
      } as DOMRect)
      clickBtn(triggerBtn()!)
      expect(menu()!.style.top).toBe('788px')
    })
  })

  // -- syncMenuPosition catch block (line 268) ----------------------------------
  // When getBoundingClientRect throws inside syncMenuPosition, the catch absorbs
  // the error and the menu still opens.

  describe('when getBoundingClientRect throws inside syncMenuPosition:', () => {
    it('should silently absorb the error and still open the menu', () => {
      editor = new TextEdit({ element, content: CODE_DOC })
      setCursorInCode(editor)
      vi.spyOn(triggerBtn()!, 'getBoundingClientRect').mockImplementationOnce(() => {
        throw new Error('mock layout')
      })
      clickBtn(triggerBtn()!)
      expect(menu()!.style.display).toBe('')
    })
  })

  // -- updatePosition guard: nodeDOM → non-HTMLElement (lines 274-275) ---------
  // When view.nodeDOM(pos) does not return an HTMLElement, updatePosition
  // returns early. The overlay remains visible; no error propagates.

  describe('when nodeDOM returns a non-HTMLElement in updatePosition:', () => {
    it('should leave the overlay position unchanged', () => {
      editor = new TextEdit({ element, content: CODE_DOC })
      setCursorInCode(editor)
      vi.spyOn(pmView(editor), 'nodeDOM').mockReturnValue(null)
      const view = pmView(editor)
      view.dispatch(view.state.tr)
      expect(overlay()!.style.display).toBe('')
    })
  })

  // -- destroy ---------------------------------------------------------------

  describe('when the editor is destroyed:', () => {
    it('should remove the overlay and menu from the DOM', () => {
      editor = new TextEdit({ element })
      editor.destroy()
      editor = undefined
      expect(overlay()).toBeNull()
      expect(menu()).toBeNull()
    })
  })
})
