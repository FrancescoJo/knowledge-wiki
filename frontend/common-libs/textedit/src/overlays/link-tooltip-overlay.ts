/**
 * link-tooltip-overlay.ts
 *
 * Displays the href of a hovered hyperlink near the cursor.
 * Ctrl+click on a link opens it in a new tab.
 *
 * $Since: 2026-05-19
 */

import {Extension} from '@tiptap/core'
import {Plugin, PluginKey} from '@tiptap/pm/state'
import type {EditorView} from '@tiptap/pm/view'

const KEY = new PluginKey<null>('linkTooltipOverlay')

const CSS_TOOLTIP = 'te-link-tooltip'
const CSS_CTRL_HELD = 'te-ctrl-held'
const HREF_MAX_CHARS = 60
const OFFSET_X = 14
const OFFSET_Y = 20
const SCREEN_MARGIN = 8

// -- Tooltip view --------------------------------------------------------------

class LinkTooltipView {
  readonly dom: HTMLElement

  private overLink = false

  private readonly onMousemove: (ev: MouseEvent) => void
  private readonly onMouseleave: () => void
  private readonly onClick: (ev: MouseEvent) => void
  private readonly onKeydown: (ev: KeyboardEvent) => void
  private readonly onKeyup: (ev: KeyboardEvent) => void

  constructor(private readonly editorView: EditorView) {
    this.dom = document.createElement('div')
    this.dom.className = CSS_TOOLTIP
    this.dom.hidden = true
    document.body.appendChild(this.dom)

    this.onMousemove = (ev: MouseEvent) => this.handleMousemove(ev)
    this.onMouseleave = () => this.hide()
    this.onClick = (ev: MouseEvent) => this.handleClick(ev)
    this.onKeydown = (ev: KeyboardEvent) => this.handleKeydown(ev)
    this.onKeyup = (ev: KeyboardEvent) => this.handleKeyup(ev)

    editorView.dom.addEventListener('mousemove', this.onMousemove)
    editorView.dom.addEventListener('mouseleave', this.onMouseleave)
    editorView.dom.addEventListener('click', this.onClick)
    document.addEventListener('keydown', this.onKeydown)
    document.addEventListener('keyup', this.onKeyup)
  }

  private handleMousemove(ev: MouseEvent): void {
    const anchor = (ev.target as Element).closest('a[href]')
    if (!anchor) {
      this.hide()
      return
    }

    this.overLink = true

    const href = anchor.getAttribute('href') ?? ''
    this.dom.textContent = href.length > HREF_MAX_CHARS
      ? href.slice(0, HREF_MAX_CHARS) + '…'
      : href
    this.dom.hidden = false
    this.position(ev.clientX, ev.clientY)
    this.updateCursor(ev.ctrlKey)
  }

  private handleClick(ev: MouseEvent): void {
    if (!ev.ctrlKey) return
    const anchor = (ev.target as Element).closest('a[href]')
    if (!anchor) return
    ev.preventDefault()
    ev.stopPropagation()
    const href = anchor.getAttribute('href')
    if (href) window.open(href, '_blank', 'noopener,noreferrer')
  }

  private handleKeydown(ev: KeyboardEvent): void {
    if (ev.key === 'Control' && this.overLink) this.updateCursor(true)
  }

  private handleKeyup(ev: KeyboardEvent): void {
    if (ev.key === 'Control') this.updateCursor(false)
  }

  private updateCursor(ctrlActive: boolean): void {
    this.editorView.dom.classList.toggle(CSS_CTRL_HELD, ctrlActive && this.overLink)
  }

  private hide(): void {
    this.overLink = false
    this.editorView.dom.classList.remove(CSS_CTRL_HELD)
    this.dom.hidden = true
  }

  private position(clientX: number, clientY: number): void {
    let x = clientX + OFFSET_X
    let y = clientY + OFFSET_Y

    // getBoundingClientRect() forces a synchronous layout so dimensions are
    // accurate immediately after hidden is set to false.
    const {width, height} = this.dom.getBoundingClientRect()

    if (x + width > window.innerWidth - SCREEN_MARGIN) x = clientX - width - OFFSET_X
    if (y + height > window.innerHeight - SCREEN_MARGIN) y = clientY - height - 4

    this.dom.style.left = `${Math.max(SCREEN_MARGIN, x)}px`
    this.dom.style.top = `${Math.max(SCREEN_MARGIN, y)}px`
  }

  update(): void {
  }

  destroy(): void {
    this.editorView.dom.removeEventListener('mousemove', this.onMousemove)
    this.editorView.dom.removeEventListener('mouseleave', this.onMouseleave)
    this.editorView.dom.removeEventListener('click', this.onClick)
    document.removeEventListener('keydown', this.onKeydown)
    document.removeEventListener('keyup', this.onKeyup)
    this.editorView.dom.classList.remove(CSS_CTRL_HELD)
    this.dom.remove()
  }
}

// -- Extension -----------------------------------------------------------------

// noinspection DuplicatedCode: TipTap plugin wiring; each extension wraps a different view class via the same ProseMirror factory pattern
export const LinkTooltipOverlay = Extension.create({
  name: 'linkTooltipOverlay',

  addProseMirrorPlugins() {
    return [
      new Plugin({
        key: KEY,
        view(editorView) {
          return new LinkTooltipView(editorView)
        },
      }),
    ]
  },
})
