/**
 * code-language-overlay.ts
 *
 * Floating language picker that appears below the active code block.
 * Shows when the cursor is inside a codeBlock node; hides otherwise.
 *
 * $Since: 2026-05-15
 */

import type {Editor} from '@tiptap/core'
import {Extension} from '@tiptap/core'
import type {EditorState} from '@tiptap/pm/state'
import {Plugin, PluginKey} from '@tiptap/pm/state'
import type {EditorView} from '@tiptap/pm/view'
import type {Node as PmNode} from '@tiptap/pm/model'
import type {LanguageItem} from '../extensions/code-block-extension'

const KEY = new PluginKey<null>('codeLanguageOverlay')

const CSS_WRAP = 'te-code-lang'
const CSS_BTN = 'te-code-lang__btn'
const CSS_MENU = 'te-code-lang__menu'
const CSS_SEARCH = 'te-code-lang__search'
const CSS_SEARCH_TEXT = 'te-code-lang__search-text'
const CSS_SEARCH_INPUT = 'te-code-lang__search-input'
const CSS_LIST = 'te-code-lang__list'
const CSS_ITEM = 'te-code-lang__item'
const CSS_ACTIVE = 'is-active'

const PLAIN: LanguageItem = {label: 'Plain text', value: ''}

// -- Helpers -------------------------------------------------------------------

function findCodeBlock(state: EditorState): { node: PmNode; pos: number } | null {
  const {$from} = state.selection
  for (let depth = $from.depth; depth >= 0; depth--) {
    const node = $from.node(depth)
    if (node.type.name === 'codeBlock') {
      return {node, pos: $from.before(depth)}
    }
  }
  return null
}

// -- Overlay view --------------------------------------------------------------

class CodeLanguageView {
  readonly dom: HTMLElement

  private readonly triggerBtn: HTMLButtonElement
  private readonly menuDom: HTMLElement
  private readonly searchDisplay: HTMLDivElement
  private readonly searchTextSpan: HTMLSpanElement
  private readonly searchInput: HTMLInputElement
  private readonly listDom: HTMLDivElement
  private readonly allItems: LanguageItem[]
  private readonly menuBtns: HTMLButtonElement[]

  private menuOpen = false
  private searchQuery = ''

  private readonly onEditorBlur: () => void
  private readonly onDocMousedown: (ev: MouseEvent) => void
  private readonly onScroll: () => void

  constructor(
    private readonly editorView: EditorView,
    private readonly editor: Editor,
    languages: LanguageItem[],
  ) {
    this.allItems = [PLAIN, ...languages]

    // -- FAB (trigger) --------------------------------------------------
    this.dom = document.createElement('div')
    this.dom.className = CSS_WRAP
    this.dom.style.display = 'none'
    // Prevent any mousedown inside the FAB from stealing editor focus.
    this.dom.addEventListener('mousedown', ev => ev.preventDefault())

    this.triggerBtn = document.createElement('button')
    this.triggerBtn.type = 'button'
    this.triggerBtn.className = CSS_BTN
    this.triggerBtn.addEventListener('mousedown', ev => {
      ev.preventDefault()
      this.toggleMenu()
    })
    this.dom.appendChild(this.triggerBtn)

    // -- Dropdown menu --------------------------------------------------
    this.menuDom = document.createElement('div')
    this.menuDom.className = CSS_MENU
    this.menuDom.style.display = 'none'
    // Same focus-preservation rationale as the FAB.
    this.menuDom.addEventListener('mousedown', ev => ev.preventDefault())

    // -- Search area ----------------------------------------------------
    // searchDisplay: container that provides the visual style
    // searchTextSpan: mirrors the typed query (visible)
    // searchInput: transparent overlay that captures keystrokes including IME
    this.searchDisplay = document.createElement('div')
    this.searchDisplay.className = CSS_SEARCH

    this.searchTextSpan = document.createElement('span')
    this.searchTextSpan.className = CSS_SEARCH_TEXT
    this.searchDisplay.appendChild(this.searchTextSpan)

    this.searchInput = document.createElement('input')
    this.searchInput.type = 'text'
    this.searchInput.className = CSS_SEARCH_INPUT
    this.searchInput.setAttribute('autocomplete', 'off')
    this.searchInput.spellcheck = false
    // stopPropagation lets the input receive focus on click without being
    // blocked by menuDom's mousedown.preventDefault().
    this.searchInput.addEventListener('mousedown', ev => ev.stopPropagation())
    // Handles all input including IME composition.
    this.searchInput.addEventListener('input', () => {
      this.searchQuery = this.searchInput.value
      this.syncSearch()
    })
    this.searchInput.addEventListener('keydown', ev => {
      if (ev.key === 'Escape') {
        ev.preventDefault()
        this.closeMenu()
        this.editor.view.focus()
      }
    })
    this.searchDisplay.appendChild(this.searchInput)
    this.menuDom.appendChild(this.searchDisplay)

    // -- Scrollable language list ---------------------------------------
    this.listDom = document.createElement('div')
    this.listDom.className = CSS_LIST
    this.menuDom.appendChild(this.listDom)

    // -- Menu items -----------------------------------------------------
    this.menuBtns = this.allItems.map(item => {
      const btn = document.createElement('button')
      btn.type = 'button'
      btn.className = CSS_ITEM
      btn.textContent = item.label
      btn.dataset['value'] = item.value
      btn.addEventListener('mousedown', ev => {
        ev.preventDefault()
        this.selectLanguage(item.value)
      })
      this.listDom.appendChild(btn)
      return btn
    })

    document.body.appendChild(this.dom)
    document.body.appendChild(this.menuDom)

    // -- Lifecycle listeners --------------------------------------------
    this.onEditorBlur = () => {
      // menuOpen means openMenu() already fired requestAnimationFrame to move
      // focus to searchInput — document.activeElement is not yet updated at
      // this point, so check the flag instead.
      if (this.menuOpen) return
      this.dom.style.display = 'none'
    }
    editorView.dom.addEventListener('blur', this.onEditorBlur)

    this.onDocMousedown = (ev: MouseEvent) => {
      if (
        !this.dom.contains(ev.target as Node) &&
        !this.menuDom.contains(ev.target as Node)
      ) {
        this.closeMenu()
      }
    }
    document.addEventListener('mousedown', this.onDocMousedown)

    this.onScroll = () => this.update(this.editorView)
    editorView.dom.parentElement?.addEventListener('scroll', this.onScroll, {passive: true})

    this.update(editorView)
  }

  update(view: EditorView): void {
    const result = findCodeBlock(view.state)
    if (!result) {
      this.dom.style.display = 'none'
      this.closeMenu()
      return
    }
    this.dom.style.display = ''
    const language: string = result.node.attrs['language'] ?? ''
    this.syncLabel(language)
    this.syncActiveItem(language)
    this.updatePosition(view, result.pos)
    if (this.menuOpen) this.syncMenuPosition()
  }

  destroy(): void {
    this.editorView.dom.removeEventListener('blur', this.onEditorBlur)
    this.editorView.dom.parentElement?.removeEventListener('scroll', this.onScroll)
    document.removeEventListener('mousedown', this.onDocMousedown)
    this.dom.remove()
    this.menuDom.remove()
  }

  /** Called by the plugin's handleKeyDown prop when a key is pressed in the editor. */
  handleKey(ev: KeyboardEvent): boolean {
    if (!this.menuOpen) return false

    if (ev.key === 'Escape') {
      this.closeMenu()
      return true
    }

    if (ev.key === 'Backspace') {
      this.searchQuery = this.searchQuery.slice(0, -1)
      this.searchInput.value = this.searchQuery
      this.syncSearch()
      return true
    }

    // Printable character — append to query.
    if (ev.key.length === 1 && !ev.ctrlKey && !ev.metaKey && !ev.altKey) {
      this.searchQuery += ev.key
      this.searchInput.value = this.searchQuery
      this.syncSearch()
      return true
    }

    return false
  }

  // -- Private -----------------------------------------------------------------

  private toggleMenu(): void {
    if (this.menuOpen) {
      this.closeMenu()
      this.editor.view.focus()
    } else {
      this.openMenu()
    }
  }

  private openMenu(): void {
    this.searchQuery = ''
    this.searchInput.value = ''
    this.syncSearch()
    this.menuDom.style.display = ''
    this.menuOpen = true
    this.syncMenuPosition()
    // Transfer focus to the search input so IME events go there, not the editor.
    requestAnimationFrame(() => {
      this.searchInput.focus()
    })
  }

  private closeMenu(): void {
    this.menuDom.style.display = 'none'
    this.menuOpen = false
  }

  private syncMenuPosition(): void {
    try {
      const btnRect = this.triggerBtn.getBoundingClientRect()
      const menuH = this.menuDom.getBoundingClientRect().height
      const vph = window.innerHeight

      // Open upward when the menu would extend beyond the viewport bottom.
      const fitsBelow = btnRect.bottom + 2 + menuH <= vph
      this.menuDom.style.top = fitsBelow
        ? `${btnRect.bottom + 2}px`
        : `${btnRect.top - 2 - menuH}px`
      this.menuDom.style.left = `${btnRect.left}px`
    } catch { /* leave unchanged */
    }
  }

  private updatePosition(view: EditorView, pos: number): void {
    try {
      const domNode = view.nodeDOM(pos)
      const el = domNode instanceof HTMLElement ? domNode : null
      if (!el) return
      const rect = el.getBoundingClientRect()
      const areaDom = view.dom.parentElement ?? view.dom
      const areaRect = areaDom.getBoundingClientRect()
      this.dom.style.top = `${rect.bottom + 4}px`
      this.dom.style.left = `${areaRect.left + areaRect.width / 2}px`
      this.dom.style.transform = 'translateX(-50%)'
    } catch { /* leave unchanged */
    }
  }

  private syncLabel(language: string): void {
    const item = this.allItems.find(i => i.value === language) ?? PLAIN
    this.triggerBtn.textContent = `${item.label} ▾`
  }

  private syncActiveItem(language: string): void {
    for (const btn of this.menuBtns) {
      btn.classList.toggle(CSS_ACTIVE, btn.dataset['value'] === language)
    }
  }

  private syncSearch(): void {
    this.searchTextSpan.textContent = this.searchQuery
    const q = this.searchQuery.toLowerCase()
    for (const btn of this.menuBtns) {
      const label = btn.textContent?.toLowerCase() ?? ''
      btn.style.display = label.includes(q) ? '' : 'none'
    }
  }

  private selectLanguage(value: string): void {
    this.editor
      .chain()
      .focus()
      .updateAttributes('codeBlock', {language: value || null})
      .run()
    this.closeMenu()
  }
}

// -- Extension -----------------------------------------------------------------

interface CodeLanguageOverlayOptions {
  languages: LanguageItem[]
}

export const CodeLanguageOverlay = Extension.create<CodeLanguageOverlayOptions>({
  name: 'codeLanguageOverlay',

  addOptions() {
    return {languages: []}
  },

  addProseMirrorPlugins() {
    const {languages} = this.options
    const editor = this.editor
    let overlayView: CodeLanguageView | null = null

    return [
      new Plugin({
        key: KEY,
        view(editorView: EditorView) {
          const v = new CodeLanguageView(editorView, editor, languages)
          overlayView = v
          return {
            update(view: EditorView) {
              v.update(view)
            },
            destroy() {
              v.destroy();
              overlayView = null
            },
          }
        },
        props: {
          handleKeyDown(_view, ev) {
            return overlayView?.handleKey(ev) ?? false
          },
        },
      }),
    ]
  },
})
