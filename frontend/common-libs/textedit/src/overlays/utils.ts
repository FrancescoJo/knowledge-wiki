/**
 * utils.ts
 *
 * Shared helpers used across overlay extensions.
 *
 * $Since: 2026-05-18
 */

import type { Editor } from '@tiptap/core'
import type { Node as PmNode } from '@tiptap/pm/model'
import type { EditorView } from '@tiptap/pm/view'
import { TableMap } from '@tiptap/pm/tables'
import type { selectionCell } from '@tiptap/pm/tables'

export function tableDepthOf($cell: ReturnType<typeof selectionCell>): number {
  let d = $cell.depth
  while (d > 0 && $cell.node(d).type.spec['tableRole'] !== 'table') d--
  return d
}

export function tableNodeAt($cell: ReturnType<typeof selectionCell>): {
  tableNode: ReturnType<ReturnType<typeof selectionCell>['node']>
  tableStart: number
} {
  const depth = tableDepthOf($cell)
  return { tableNode: $cell.node(depth), tableStart: $cell.start(depth) }
}

export function mkItem(label: string, className: string, title?: string): HTMLButtonElement {
  const b = document.createElement('button')
  b.type = 'button'
  b.className = className
  b.textContent = label
  if (title) b.title = title
  return b
}

export function setDisabled(btn: HTMLButtonElement, disabled: boolean): void {
  btn.classList.toggle('is-disabled', disabled)
  btn.disabled = disabled
}

export function bindMenuCloseListeners(opts: {
  anchors: HTMLElement[]
  isOpen: () => boolean
  close: () => void
  identity: unknown
}): void {
  const { anchors, isOpen, close, identity } = opts
  document.addEventListener('mousedown', ev => {
    if (anchors.every(el => !el.contains(ev.target as Node))) close()
  })
  document.addEventListener('keydown', ev => {
    if (ev.key === 'Escape' && isOpen()) close()
  })
  document.addEventListener('te:context-menu-open', (ev: Event) => {
    if ((ev as CustomEvent).detail !== identity) close()
  })
}

// -- Drag helpers -------------------------------------------------------------

export const DRAG_THRESHOLD = 5

export function mkSep(cssClass: string): HTMLHRElement {
  const hr = document.createElement('hr')
  hr.className = cssClass
  return hr
}

export function createDragGhost(cssHandle: string, cssGhost: string): HTMLElement {
  const ghost = document.createElement('div')
  ghost.className = `${cssHandle} ${cssGhost}`
  ghost.style.cssText = 'position:fixed;pointer-events:none;opacity:0.5;z-index:9999'
  document.body.appendChild(ghost)
  return ghost
}

export function updateDragGhost(ghost: HTMLElement | null, x: number, y: number): void {
  if (!ghost) return
  const w = ghost.offsetWidth
  const h = ghost.offsetHeight
  ghost.style.left = `${x - w / 2}px`
  ghost.style.top  = `${y - h / 2}px`
}

export function removeDragGhost(ghost: HTMLElement | null): void {
  ghost?.remove()
}

// -- Table helpers -------------------------------------------------------------

export function clearCells(
  view: EditorView,
  editor: Editor,
  tableNode: PmNode,
  tableStart: number,
  getOffsets: (map: TableMap) => number[],
): void {
  const { state } = view
  const paragraphType = state.schema.nodes['paragraph']
  if (!paragraphType) return
  try {
    // Sort descending so replaceWith calls don't shift subsequent positions.
    const offsets = getOffsets(TableMap.get(tableNode)).sort((a, b) => b - a)
    const tr = state.tr
    let modified = false
    for (const offset of offsets) {
      const cellNode = tableNode.nodeAt(offset)
      if (!cellNode) continue
      const cellPos = tableStart + offset
      const start = cellPos + 1
      const end = cellPos + cellNode.nodeSize - 1
      if (end > start) {
        tr.replaceWith(start, end, paragraphType.create())
        modified = true
      }
    }
    if (modified) view.dispatch(tr)
    editor.commands.focus()
  } catch { /* no-op */ }
}

// -- Colour helpers ------------------------------------------------------------

function hsvToRgb(h: number, s: number, v: number): [number, number, number] {
  const c = v * s
  const x = c * (1 - Math.abs(((h / 60) % 2) - 1))
  const m = v - c
  let r = 0, g = 0, b = 0
  if      (h < 60)  { r = c; g = x; b = 0 }
  else if (h < 120) { r = x; g = c; b = 0 }
  else if (h < 180) { r = 0; g = c; b = x }
  else if (h < 240) { r = 0; g = x; b = c }
  else if (h < 300) { r = x; g = 0; b = c }
  else              { r = c; g = 0; b = x }
  return [Math.round((r + m) * 255), Math.round((g + m) * 255), Math.round((b + m) * 255)]
}

function rgbToHsv(r: number, g: number, b: number): [number, number, number] {
  const rn = r / 255, gn = g / 255, bn = b / 255
  const max = Math.max(rn, gn, bn)
  const min = Math.min(rn, gn, bn)
  const delta = max - min
  let h = 0
  if (delta > 0) {
    if      (max === rn) h = 60 * (((gn - bn) / delta) % 6)
    else if (max === gn) h = 60 * ((bn - rn) / delta + 2)
    else                 h = 60 * ((rn - gn) / delta + 4)
  }
  if (h < 0) h += 360
  return [h, max === 0 ? 0 : delta / max, max]
}

function rgbToHex(r: number, g: number, b: number): string {
  return '#' + [r, g, b].map(n => Math.max(0, Math.min(255, Math.round(n))).toString(16).padStart(2, '0')).join('')
}

function hexToRgb(hex: string): [number, number, number] | null {
  const m = /^#?([0-9a-f]{2})([0-9a-f]{2})([0-9a-f]{2})$/i.exec(hex)
  return m ? [parseInt(m[1], 16), parseInt(m[2], 16), parseInt(m[3], 16)] : null
}

function clamp(n: number, lo: number, hi: number): number {
  return Math.max(lo, Math.min(hi, n))
}

function mkNumInput(min: string, max: string): HTMLInputElement {
  const inp = document.createElement('input')
  inp.type = 'number'
  inp.className = 'te-colour-picker__rgb-input'
  inp.min = min; inp.max = max; inp.step = '1'; inp.value = '0'
  return inp
}

function mkRgbGroup(inp: HTMLInputElement, label: string): HTMLElement {
  const g = document.createElement('div')
  g.className = 'te-colour-picker__rgb-group'
  const lbl = document.createElement('label')
  lbl.className = 'te-colour-picker__rgb-label'
  lbl.textContent = label
  g.append(inp, lbl)
  return g
}

// -- ColourPickerPopup ---------------------------------------------------------

export class ColourPickerPopup {
  readonly dom: HTMLElement

  private readonly canvas:    HTMLCanvasElement
  private readonly cursor:    HTMLElement
  private readonly hueSlider: HTMLInputElement
  private readonly swatch:    HTMLElement
  private readonly inputR:    HTMLInputElement
  private readonly inputG:    HTMLInputElement
  private readonly inputB:    HTMLInputElement
  private readonly hexInput:  HTMLInputElement
  private readonly applyBtn:  HTMLButtonElement
  private readonly cancelBtn: HTMLButtonElement

  private hue = 0
  private sat = 1
  private val = 1

  private onApply: ((colour: string) => void) | null = null
  private skipNextClose = false

  constructor(extraClass = '') {
    this.dom = document.createElement('div')
    this.dom.className = `te-colour-picker${extraClass ? ` ${extraClass}` : ''}`
    this.dom.hidden = true
    this.dom.style.display = 'none'

    // -- SV canvas -----------------------------------------------------------
    const canvasWrap = document.createElement('div')
    canvasWrap.className = 'te-colour-picker__sv-wrap'

    this.canvas = document.createElement('canvas')
    this.canvas.className = 'te-colour-picker__sv'
    this.canvas.width = 220
    this.canvas.height = 150

    this.cursor = document.createElement('div')
    this.cursor.className = 'te-colour-picker__sv-cursor'

    canvasWrap.append(this.canvas, this.cursor)

    // -- Controls: swatch + hue slider ---------------------------------------
    const controlsRow = document.createElement('div')
    controlsRow.className = 'te-colour-picker__controls'

    this.swatch = document.createElement('div')
    this.swatch.className = 'te-colour-picker__swatch'

    this.hueSlider = document.createElement('input')
    this.hueSlider.type = 'range'
    this.hueSlider.className = 'te-colour-picker__hue'
    this.hueSlider.min = '0'; this.hueSlider.max = '360'
    this.hueSlider.step = '1'; this.hueSlider.value = '0'

    controlsRow.append(this.swatch)

    // -- Eyedropper (Chromium only) -------------------------------------------
    if ('EyeDropper' in window) {
      const EyeDropperCtor = (window as Record<string, unknown>)['EyeDropper'] as
        new () => { open(): Promise<{ sRGBHex: string }> }
      const eyedropperBtn = document.createElement('button')
      eyedropperBtn.type = 'button'
      eyedropperBtn.className = 'te-colour-picker__eyedropper'
      eyedropperBtn.title = 'Pick a colour'
      eyedropperBtn.textContent = '◎'
      controlsRow.append(eyedropperBtn)
      eyedropperBtn.addEventListener('click', () => {
        new EyeDropperCtor().open()
          .then(r => this.setColour(r.sRGBHex))
          .catch(() => { /* user cancelled */ })
      })
    }

    controlsRow.append(this.hueSlider)

    // -- Inputs: R G B + hex -------------------------------------------------
    const inputsRow = document.createElement('div')
    inputsRow.className = 'te-colour-picker__inputs'

    this.inputR = mkNumInput('0', '255')
    this.inputG = mkNumInput('0', '255')
    this.inputB = mkNumInput('0', '255')

    this.hexInput = document.createElement('input')
    this.hexInput.type = 'text'
    this.hexInput.className = 'te-colour-picker__hex'
    this.hexInput.maxLength = 7
    this.hexInput.placeholder = '#000000'
    this.hexInput.spellcheck = false

    inputsRow.append(
      mkRgbGroup(this.inputR, 'R'),
      mkRgbGroup(this.inputG, 'G'),
      mkRgbGroup(this.inputB, 'B'),
      this.hexInput,
    )

    // -- Actions -------------------------------------------------------------
    const actionsRow = document.createElement('div')
    actionsRow.className = 'te-colour-picker__actions'

    this.cancelBtn = document.createElement('button')
    this.cancelBtn.type = 'button'
    this.cancelBtn.className = 'te-colour-picker__cancel'
    this.cancelBtn.textContent = 'Cancel'

    this.applyBtn = document.createElement('button')
    this.applyBtn.type = 'button'
    this.applyBtn.className = 'te-colour-picker__apply'
    this.applyBtn.textContent = 'Apply'

    actionsRow.append(this.cancelBtn, this.applyBtn)

    this.dom.append(canvasWrap, controlsRow, inputsRow, actionsRow)

    // -- Canvas drag ---------------------------------------------------------
    this.canvas.addEventListener('mousedown', ev => {
      ev.preventDefault()
      this.pickFromCanvas(ev)
    })
    this.canvas.addEventListener('mousemove', ev => {
      if (ev.buttons & 1) this.pickFromCanvas(ev)
    })

    // -- Hue slider ----------------------------------------------------------
    this.hueSlider.addEventListener('input', () => {
      this.hue = Number(this.hueSlider.value)
      this.drawCanvas()
      this.syncFromHsv()
    })

    // -- RGB inputs ----------------------------------------------------------
    const onRgbChange = (): void => {
      const r = clamp(Number(this.inputR.value) || 0, 0, 255)
      const g = clamp(Number(this.inputG.value) || 0, 0, 255)
      const b = clamp(Number(this.inputB.value) || 0, 0, 255)
      ;[this.hue, this.sat, this.val] = rgbToHsv(r, g, b)
      this.hueSlider.value = String(Math.round(this.hue))
      this.drawCanvas()
      this.updateCursor()
      this.hexInput.value = rgbToHex(r, g, b)
      this.updateSwatch()
    }
    this.inputR.addEventListener('input', onRgbChange)
    this.inputG.addEventListener('input', onRgbChange)
    this.inputB.addEventListener('input', onRgbChange)

    // -- Hex input -----------------------------------------------------------
    this.hexInput.addEventListener('input', () => {
      const rgb = hexToRgb(this.hexInput.value)
      if (!rgb) return
      ;[this.hue, this.sat, this.val] = rgbToHsv(...rgb)
      this.hueSlider.value = String(Math.round(this.hue))
      this.drawCanvas()
      this.updateCursor()
      this.inputR.value = String(rgb[0])
      this.inputG.value = String(rgb[1])
      this.inputB.value = String(rgb[2])
      this.updateSwatch()
    })

    // -- Apply / Cancel ------------------------------------------------------
    this.applyBtn.addEventListener('click', () => {
      this.onApply?.(this.currentHex())
      this.close()
    })
    this.cancelBtn.addEventListener('click', () => {
      this.close()
    })

    document.addEventListener('mousedown', ev => {
      if (this.skipNextClose) { this.skipNextClose = false; return }
      if (!this.dom.hidden && !this.dom.contains(ev.target as Node)) this.close()
    })
    document.addEventListener('keydown', ev => {
      if (ev.key === 'Escape' && !this.dom.hidden) this.close()
    })
    document.addEventListener('te:context-menu-open', () => {
      if (!this.dom.hidden) this.close()
    })
  }

  get isOpen(): boolean { return !this.dom.hidden }

  open(nearRect: DOMRect, initialColour: string, onApply: (colour: string) => void): void {
    this.skipNextClose = true
    this.onApply = onApply
    this.setColour(initialColour)
    this.dom.hidden = false
    this.dom.style.display = ''
    this.position(nearRect)
  }

  close(): void {
    this.onApply = null
    this.dom.hidden = true
    this.dom.style.display = 'none'
  }

  private setColour(hex: string): void {
    const rgb = hexToRgb(hex)
    if (!rgb) return
    ;[this.hue, this.sat, this.val] = rgbToHsv(...rgb)
    this.hueSlider.value = String(Math.round(this.hue))
    this.drawCanvas()
    this.updateCursor()
    this.inputR.value = String(rgb[0])
    this.inputG.value = String(rgb[1])
    this.inputB.value = String(rgb[2])
    this.hexInput.value = hex.startsWith('#') ? hex : `#${hex}`
    this.updateSwatch()
  }

  private currentHex(): string {
    return rgbToHex(...hsvToRgb(this.hue, this.sat, this.val))
  }

  private pickFromCanvas(ev: MouseEvent): void {
    const rect = this.canvas.getBoundingClientRect()
    const w = rect.width  || this.canvas.width
    const h = rect.height || this.canvas.height
    this.sat = clamp((ev.clientX - rect.left) / w, 0, 1)
    this.val = clamp(1 - (ev.clientY - rect.top) / h, 0, 1)
    this.updateCursor()
    this.syncFromHsv()
  }

  private syncFromHsv(): void {
    const [r, g, b] = hsvToRgb(this.hue, this.sat, this.val)
    this.inputR.value = String(r)
    this.inputG.value = String(g)
    this.inputB.value = String(b)
    this.hexInput.value = rgbToHex(r, g, b)
    this.updateSwatch()
  }

  private drawCanvas(): void {
    const ctx = this.canvas.getContext('2d')
    if (!ctx) return
    const { width: w, height: h } = this.canvas

    ctx.fillStyle = `hsl(${this.hue}, 100%, 50%)`
    ctx.fillRect(0, 0, w, h)

    const gradS = ctx.createLinearGradient(0, 0, w, 0)
    gradS.addColorStop(0, 'rgba(255,255,255,1)')
    gradS.addColorStop(1, 'rgba(255,255,255,0)')
    ctx.fillStyle = gradS
    ctx.fillRect(0, 0, w, h)

    const gradV = ctx.createLinearGradient(0, 0, 0, h)
    gradV.addColorStop(0, 'rgba(0,0,0,0)')
    gradV.addColorStop(1, 'rgba(0,0,0,1)')
    ctx.fillStyle = gradV
    ctx.fillRect(0, 0, w, h)
  }

  private updateCursor(): void {
    const w = this.canvas.getBoundingClientRect().width  || this.canvas.width
    const h = this.canvas.getBoundingClientRect().height || this.canvas.height
    this.cursor.style.left = `${this.sat * w}px`
    this.cursor.style.top  = `${(1 - this.val) * h}px`
  }

  private updateSwatch(): void {
    this.swatch.style.backgroundColor = this.currentHex()
  }

  private position(nearRect: DOMRect): void {
    const r = this.dom.getBoundingClientRect()
    const pickerW = r.width  || 244
    const pickerH = r.height || 320

    let top  = nearRect.bottom + 4
    let left = nearRect.left

    if (left + pickerW > window.innerWidth)  left = window.innerWidth - pickerW - 8
    left = Math.max(8, left)
    if (top  + pickerH > window.innerHeight) top  = nearRect.top - pickerH - 4
    top  = Math.max(8, top)

    this.dom.style.top  = `${top}px`
    this.dom.style.left = `${left}px`
  }
}
