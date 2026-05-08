/**
 * utils.test.ts
 *
 * Tests for utilities exported from utils.ts.
 *
 * $Since: 2026-05-19
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import {
  ColourPickerPopup,
  mkItem,
  setDisabled,
  bindMenuCloseListeners,
  tableNodeAt,
  tableDepthOf,
} from '@src/overlays/utils'
import type { selectionCell } from '@tiptap/pm/tables'

// -- Test doubles --------------------------------------------------------------

function fakeCellAt(tableDepth: number, totalDepth: number): ReturnType<typeof selectionCell> {
  return {
    depth: totalDepth,
    node: (d: number) => ({
      type: { spec: { tableRole: d === tableDepth ? 'table' : 'cell' } },
    }),
    start: (d: number) => d * 100,
  } as unknown as ReturnType<typeof selectionCell>
}

// -- mkItem --------------------------------------------------------------------

describe('mkItem:', () => {
  it('should create a button with the given label and class name', () => {
    const btn = mkItem('Save', 'my-btn')
    expect(btn.tagName).toBe('BUTTON')
    expect(btn.type).toBe('button')
    expect(btn.textContent).toBe('Save')
    expect(btn.className).toBe('my-btn')
  })

  it('should leave the title attribute empty when not provided', () => {
    const btn = mkItem('Save', 'my-btn')
    expect(btn.title).toBe('')
  })

  it('should set the title attribute when provided', () => {
    const btn = mkItem('Save', 'my-btn', 'Save the document')
    expect(btn.title).toBe('Save the document')
  })
})

// -- setDisabled ---------------------------------------------------------------

describe('setDisabled:', () => {
  it('should add the is-disabled class and set the disabled property when called with true', () => {
    const btn = document.createElement('button')
    setDisabled(btn, true)
    expect(btn.classList.contains('is-disabled')).toBe(true)
    expect(btn.disabled).toBe(true)
  })

  it('should remove the is-disabled class and clear the disabled property when called with false', () => {
    const btn = document.createElement('button')
    btn.classList.add('is-disabled')
    btn.disabled = true
    setDisabled(btn, false)
    expect(btn.classList.contains('is-disabled')).toBe(false)
    expect(btn.disabled).toBe(false)
  })
})

// -- tableDepthOf --------------------------------------------------------------

describe('tableDepthOf:', () => {
  it('should return the depth at which the table node sits', () => {
    const $cell = fakeCellAt(2, 3)
    expect(tableDepthOf($cell)).toBe(2)
  })

  it('should return 0 when the table is at the root depth', () => {
    const $cell = fakeCellAt(0, 1)
    expect(tableDepthOf($cell)).toBe(0)
  })
})

// -- tableNodeAt ---------------------------------------------------------------

describe('tableNodeAt:', () => {
  it('should return a tableNode whose tableRole is "table"', () => {
    const $cell = fakeCellAt(2, 3)
    const { tableNode } = tableNodeAt($cell)
    expect(tableNode.type.spec['tableRole']).toBe('table')
  })

  it('should return the tableStart derived from the table depth', () => {
    const $cell = fakeCellAt(2, 3)
    // fakeCellAt sets start(d) = d * 100, so depth 2 → 200
    const { tableStart } = tableNodeAt($cell)
    expect(tableStart).toBe(200)
  })
})

// -- bindMenuCloseListeners ----------------------------------------------------

describe('bindMenuCloseListeners:', () => {
  let anchor: HTMLElement
  let outside: HTMLElement

  beforeEach(() => {
    anchor  = document.createElement('div')
    outside = document.createElement('div')
    document.body.append(anchor, outside)
  })

  afterEach(() => {
    anchor.remove()
    outside.remove()
  })

  it('should call close when mousedown occurs outside all anchors', () => {
    let closed = false
    bindMenuCloseListeners({
      anchors: [anchor],
      isOpen: () => true,
      close: () => { closed = true },
      identity: {},
    })
    outside.dispatchEvent(new MouseEvent('mousedown', { bubbles: true }))
    expect(closed).toBe(true)
  })

  it('should not call close when mousedown occurs inside an anchor', () => {
    let closed = false
    bindMenuCloseListeners({
      anchors: [anchor],
      isOpen: () => true,
      close: () => { closed = true },
      identity: {},
    })
    anchor.dispatchEvent(new MouseEvent('mousedown', { bubbles: true }))
    expect(closed).toBe(false)
  })

  it('should call close when Escape is pressed and isOpen returns true', () => {
    let closed = false
    bindMenuCloseListeners({
      anchors: [anchor],
      isOpen: () => true,
      close: () => { closed = true },
      identity: {},
    })
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    expect(closed).toBe(true)
  })

  it('should not call close when Escape is pressed and isOpen returns false', () => {
    let closed = false
    bindMenuCloseListeners({
      anchors: [anchor],
      isOpen: () => false,
      close: () => { closed = true },
      identity: {},
    })
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    expect(closed).toBe(false)
  })

  it('should call close when te:context-menu-open fires with a different identity', () => {
    let closed = false
    const id = {}
    bindMenuCloseListeners({
      anchors: [anchor],
      isOpen: () => true,
      close: () => { closed = true },
      identity: id,
    })
    document.dispatchEvent(new CustomEvent('te:context-menu-open', { detail: {} }))
    expect(closed).toBe(true)
  })

  it('should not call close when te:context-menu-open fires with the same identity', () => {
    let closed = false
    const id = {}
    bindMenuCloseListeners({
      anchors: [anchor],
      isOpen: () => true,
      close: () => { closed = true },
      identity: id,
    })
    document.dispatchEvent(new CustomEvent('te:context-menu-open', { detail: id }))
    expect(closed).toBe(false)
  })
})

// -- ColourPickerPopup ---------------------------------------------------------

// -- Helpers -------------------------------------------------------------------

function fakeRect(x = 10, y = 10, w = 100, h = 30): DOMRect {
  return new DOMRect(x, y, w, h)
}

function canvas(popup: ColourPickerPopup): HTMLCanvasElement {
  return popup.dom.querySelector<HTMLCanvasElement>('.te-colour-picker__sv')!
}

function hueSlider(popup: ColourPickerPopup): HTMLInputElement {
  return popup.dom.querySelector<HTMLInputElement>('.te-colour-picker__hue')!
}

function hexInput(popup: ColourPickerPopup): HTMLInputElement {
  return popup.dom.querySelector<HTMLInputElement>('.te-colour-picker__hex')!
}

function rgbInputs(popup: ColourPickerPopup): [HTMLInputElement, HTMLInputElement, HTMLInputElement] {
  const all = popup.dom.querySelectorAll<HTMLInputElement>('.te-colour-picker__rgb-input')
  return [all[0], all[1], all[2]]
}

function applyBtn(popup: ColourPickerPopup): HTMLButtonElement {
  return popup.dom.querySelector<HTMLButtonElement>('.te-colour-picker__apply')!
}

function cancelBtn(popup: ColourPickerPopup): HTMLButtonElement {
  return popup.dom.querySelector<HTMLButtonElement>('.te-colour-picker__cancel')!
}

// -- Tests ---------------------------------------------------------------------

describe('ColourPickerPopup:', () => {
  let popup: ColourPickerPopup

  beforeEach(() => {
    popup = new ColourPickerPopup()
    document.body.append(popup.dom)
  })

  afterEach(() => {
    popup.dom.remove()
  })

  // -- Construction ---------------------------------------------------------

  describe('on construction:', () => {
    it('should start hidden', () => {
      expect(popup.isOpen).toBe(false)
      expect(popup.dom.hidden).toBe(true)
    })

    it('should have the default CSS class on the root element', () => {
      expect(popup.dom.classList.contains('te-colour-picker')).toBe(true)
    })
  })

  describe('when constructed with an extra CSS class:', () => {
    it('should append the extra class to the root element', () => {
      const p = new ColourPickerPopup(' te-my-extra')
      expect(p.dom.classList.contains('te-my-extra')).toBe(true)
    })
  })

  // -- open -----------------------------------------------------------------

  describe('open:', () => {
    it('should make the popup visible', () => {
      popup.open(fakeRect(), '#ff0000', () => {})
      expect(popup.isOpen).toBe(true)
      expect(popup.dom.hidden).toBe(false)
    })

    it('should populate the hex input with the initial colour', () => {
      popup.open(fakeRect(), '#00ff00', () => {})
      expect(hexInput(popup).value).toBe('#00ff00')
    })
  })

  // -- close -----------------------------------------------------------------

  describe('close:', () => {
    it('should hide the popup', () => {
      popup.open(fakeRect(), '#ff0000', () => {})
      popup.close()
      expect(popup.isOpen).toBe(false)
      expect(popup.dom.hidden).toBe(true)
    })
  })

  // -- Apply button ----------------------------------------------------------

  describe('when the apply button is clicked:', () => {
    it('should call onApply with a #rrggbb string', () => {
      let result: string | undefined
      popup.open(fakeRect(), '#ff0000', c => { result = c })
      applyBtn(popup).dispatchEvent(new MouseEvent('click', { bubbles: true }))
      expect(result).toMatch(/^#[0-9a-f]{6}$/i)
    })

    it('should close the popup', () => {
      popup.open(fakeRect(), '#ff0000', () => {})
      applyBtn(popup).dispatchEvent(new MouseEvent('click', { bubbles: true }))
      expect(popup.isOpen).toBe(false)
    })
  })

  // -- Cancel button ---------------------------------------------------------

  describe('when the cancel button is clicked:', () => {
    it('should close the popup without calling onApply', () => {
      let called = false
      popup.open(fakeRect(), '#ff0000', () => { called = true })
      cancelBtn(popup).dispatchEvent(new MouseEvent('click', { bubbles: true }))
      expect(popup.isOpen).toBe(false)
      expect(called).toBe(false)
    })
  })

  // -- Escape key ------------------------------------------------------------

  describe('when Escape is pressed while open:', () => {
    it('should close the popup', () => {
      popup.open(fakeRect(), '#ff0000', () => {})
      document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }))
      expect(popup.isOpen).toBe(false)
    })
  })

  describe('when Escape is pressed while closed:', () => {
    it('should have no effect', () => {
      document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }))
      expect(popup.isOpen).toBe(false)
    })
  })

  // -- Outside click ---------------------------------------------------------

  describe('when mousedown outside the popup fires after it opened:', () => {
    it('should close the popup on the second external mousedown', () => {
      popup.open(fakeRect(), '#ff0000', () => {})
      // First mousedown clears the skipNextClose guard set by open().
      document.dispatchEvent(new MouseEvent('mousedown', { bubbles: false }))
      expect(popup.isOpen).toBe(true)
      // Second mousedown (target=document, not inside popup.dom) triggers close.
      document.dispatchEvent(new MouseEvent('mousedown', { bubbles: false }))
      expect(popup.isOpen).toBe(false)
    })
  })

  // -- te:context-menu-open --------------------------------------------------

  describe('when te:context-menu-open fires while the popup is open:', () => {
    it('should close the popup', () => {
      popup.open(fakeRect(), '#ff0000', () => {})
      document.dispatchEvent(new CustomEvent('te:context-menu-open'))
      expect(popup.isOpen).toBe(false)
    })
  })

  describe('when te:context-menu-open fires while the popup is closed:', () => {
    it('should have no effect', () => {
      document.dispatchEvent(new CustomEvent('te:context-menu-open'))
      expect(popup.isOpen).toBe(false)
    })
  })

  // -- Canvas mousedown → pickFromCanvas + syncFromHsv -----------------------

  describe('when the canvas receives a mousedown event:', () => {
    it('should update the hex input to reflect the picked colour', () => {
      popup.open(fakeRect(), '#ff0000', () => {})
      canvas(popup).dispatchEvent(
        new MouseEvent('mousedown', { bubbles: true, cancelable: true, clientX: 100, clientY: 50 }),
      )
      expect(hexInput(popup).value).toMatch(/^#[0-9a-f]{6}$/i)
    })
  })

  describe('when the mouse is dragged across the canvas (buttons=1):', () => {
    it('should update the hex input at the new pointer position', () => {
      popup.open(fakeRect(), '#ff0000', () => {})
      canvas(popup).dispatchEvent(
        new MouseEvent('mousemove', { bubbles: true, cancelable: true, buttons: 1, clientX: 50, clientY: 75 }),
      )
      expect(hexInput(popup).value).toMatch(/^#[0-9a-f]{6}$/i)
    })
  })

  describe('when the mouse moves over the canvas without a button held (buttons=0):', () => {
    it('should not change the hex input', () => {
      popup.open(fakeRect(), '#ff0000', () => {})
      const before = hexInput(popup).value
      canvas(popup).dispatchEvent(
        new MouseEvent('mousemove', { bubbles: true, cancelable: true, buttons: 0, clientX: 50, clientY: 75 }),
      )
      expect(hexInput(popup).value).toBe(before)
    })
  })

  // -- Hue slider → syncFromHsv ----------------------------------------------

  describe('when the hue slider is moved:', () => {
    it('should update the RGB and hex inputs', () => {
      popup.open(fakeRect(), '#ff0000', () => {})
      const slider = hueSlider(popup)
      slider.value = '120'
      slider.dispatchEvent(new Event('input'))
      expect(hexInput(popup).value).toMatch(/^#[0-9a-f]{6}$/i)
    })

    it('should reflect different hue values correctly', () => {
      popup.open(fakeRect(), '#ff0000', () => {})
      const slider = hueSlider(popup)
      // Exercise each of the 6 hsvToRgb hue branches:
      for (const hue of [30, 90, 150, 210, 270, 330]) {
        slider.value = String(hue)
        slider.dispatchEvent(new Event('input'))
        expect(hexInput(popup).value).toMatch(/^#[0-9a-f]{6}$/i)
      }
    })
  })

  // -- RGB inputs ------------------------------------------------------------

  describe('when RGB inputs are changed:', () => {
    it('should update the hex input to match the new RGB values', () => {
      popup.open(fakeRect(), '#ff0000', () => {})
      const [r, g, b] = rgbInputs(popup)
      r.value = '0';   r.dispatchEvent(new Event('input'))
      g.value = '128'; g.dispatchEvent(new Event('input'))
      b.value = '255'; b.dispatchEvent(new Event('input'))
      expect(hexInput(popup).value).toMatch(/^#[0-9a-f]{6}$/i)
    })
  })

  // -- Hex input -------------------------------------------------------------

  describe('when the hex input receives a valid 6-digit hex:', () => {
    it('should update the RGB inputs to the corresponding values', () => {
      popup.open(fakeRect(), '#000000', () => {})
      const hex = hexInput(popup)
      hex.value = '#0080ff'
      hex.dispatchEvent(new Event('input'))
      const [r, g, b] = rgbInputs(popup)
      expect(Number(r.value)).toBe(0)
      expect(Number(g.value)).toBe(128)
      expect(Number(b.value)).toBe(255)
    })
  })

  describe('when the hex input receives an invalid value:', () => {
    it('should not crash and leave RGB inputs unchanged', () => {
      popup.open(fakeRect(), '#ff0000', () => {})
      const [r] = rgbInputs(popup)
      const rBefore = r.value
      hexInput(popup).value = 'not-a-colour'
      hexInput(popup).dispatchEvent(new Event('input'))
      expect(r.value).toBe(rBefore)
    })
  })
})

// -- EyeDropper integration ----------------------------------------------------
// window.EyeDropper is absent in jsdom, so we inject a mock before constructing
// ColourPickerPopup. The constructor's `if ('EyeDropper' in window)` guard then
// fires and registers the eyedropper button and its click handler.

describe('ColourPickerPopup — EyeDropper (resolve path):', () => {
  let popup: ColourPickerPopup

  afterEach(() => {
    popup.dom.remove()
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    delete (window as any).EyeDropper
  })

  async function expectHexInput(initialColour: string, expected: string): Promise<void> {
      popup.open(fakeRect(), initialColour, () => {})
      const btn = popup.dom.querySelector<HTMLButtonElement>('.te-colour-picker__eyedropper')!
      btn.dispatchEvent(new MouseEvent('click', { bubbles: true }))
      await new Promise(r => setTimeout(r, 0))
      expect(hexInput(popup).value).toBe(expected)
  }

  describe('when sRGBHex includes a leading #:', () => {
    beforeEach(() => {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      ;(window as any).EyeDropper = class {
        open(): Promise<{ sRGBHex: string }> {
          return Promise.resolve({ sRGBHex: '#ff0000' })
        }
      }
      popup = new ColourPickerPopup()
      document.body.append(popup.dom)
    })

    it('should create an eyedropper button', () => {
      expect(popup.dom.querySelector('.te-colour-picker__eyedropper')).not.toBeNull()
    })

    it('should apply the picked colour when the eyedropper resolves', async () => {
      await expectHexInput('#ffffff', '#ff0000')
    })
  })

  // -- setColour: hex without leading '#' (line 298 ternary false branch) ------
  // The real EyeDropper spec always returns '#rrggbb', but the defensive ternary
  // also handles bare 'rrggbb'. Mock sRGBHex without '#' to cover that branch.
  describe('when sRGBHex has no leading #:', () => {
    beforeEach(() => {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      ;(window as any).EyeDropper = class {
        open(): Promise<{ sRGBHex: string }> {
          return Promise.resolve({ sRGBHex: 'ff0000' })  // no leading '#'
        }
      }
      popup = new ColourPickerPopup()
      document.body.append(popup.dom)
    })

    it('should prepend # when sRGBHex has no leading hash', async () => {
      await expectHexInput('#ffffff', '#ff0000')
    })
  })
})

describe('ColourPickerPopup — EyeDropper (cancel path):', () => {
  let popup: ColourPickerPopup

  beforeEach(() => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    ;(window as any).EyeDropper = class {
      open(): Promise<{ sRGBHex: string }> {
        return Promise.reject(new Error('AbortError'))
      }
    }
    popup = new ColourPickerPopup()
    document.body.append(popup.dom)
  })

  afterEach(() => {
    popup.dom.remove()
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    delete (window as any).EyeDropper
  })

  it('should silently absorb the rejection and leave the colour unchanged', async () => {
    popup.open(fakeRect(), '#ffffff', () => {})
    const btn = popup.dom.querySelector<HTMLButtonElement>('.te-colour-picker__eyedropper')!
    btn.dispatchEvent(new MouseEvent('click', { bubbles: true }))
    await new Promise(r => setTimeout(r, 0))
    expect(hexInput(popup).value).toBe('#ffffff')
  })
})

// -- position(): overflow-adjustment branches (lines 365/367 TRUE branches) ----
// Shrink the viewport to 100×100 so the popup (244×320 default) always
// overflows, triggering the clamping/flipping branches.

describe('ColourPickerPopup — position (viewport overflow):', () => {
  let popup: ColourPickerPopup

  beforeEach(() => {
    vi.stubGlobal('innerWidth',  100)
    vi.stubGlobal('innerHeight', 100)
    popup = new ColourPickerPopup()
    document.body.append(popup.dom)
  })

  afterEach(() => {
    popup.dom.remove()
    vi.unstubAllGlobals()
  })

  it('should clamp left when the popup overflows the right edge of the viewport', () => {
    // left=10, pickerW=244 → 10+244=254 > 100 → clamp fires (line 365 TRUE)
    // left = 100-244-8 = -152 → max(8,−152) = 8
    popup.open(fakeRect(10, 10, 100, 30), '#ff0000', () => {})
    expect(popup.dom.style.left).toBe('8px')
  })

  it('should flip above the anchor when the popup overflows the bottom of the viewport', () => {
    // top=44, pickerH=320 → 44+320=364 > 100 → flip fires (line 367 TRUE)
    // top = nearRect.top − pickerH − 4 = 10−320−4 = −314 → max(8,−314) = 8
    popup.open(fakeRect(10, 10, 100, 30), '#ff0000', () => {})
    expect(popup.dom.style.top).toBe('8px')
  })
})

// -- rgbToHsv: h < 0 guard (line 52 TRUE branch) ------------------------------
// When R is max and G < B, the 60*(…%6) formula yields a negative h.
// Colour #ff0010 (R=255, G=0, B=16) triggers this: h≈−3.8 → +360.

describe('ColourPickerPopup — rgbToHsv negative-hue wrap-around:', () => {
  let popup: ColourPickerPopup

  beforeEach(() => {
    popup = new ColourPickerPopup()
    document.body.append(popup.dom)
  })

  afterEach(() => { popup.dom.remove() })

  it('should normalise a negative hue value by adding 360', () => {
    // #ff0010: R>B, G=0 → (G−B)/delta < 0 → h < 0 before wrap
    popup.open(fakeRect(), '#ff0010', () => {})
    expect(hexInput(popup).value).toMatch(/^#[0-9a-f]{6}$/i)
  })
})

// -- setColour: invalid hex (line 290 TRUE branch) -----------------------------
// When hexToRgb returns null the method returns early without updating any
// field. The popup still opens (setColour is called before dom.hidden=false).

describe('ColourPickerPopup — setColour with invalid hex:', () => {
  let popup: ColourPickerPopup

  beforeEach(() => {
    popup = new ColourPickerPopup()
    document.body.append(popup.dom)
  })

  afterEach(() => { popup.dom.remove() })

  it('should open without crashing when the initial colour is invalid', () => {
    popup.open(fakeRect(), 'not-a-colour', () => {})
    expect(popup.isOpen).toBe(true)
  })
})

// -- drawCanvas(): null context guard (line 327 TRUE branch) -------------------
// jsdom's canvas always returns a context, so mock getContext to return null
// and confirm drawCanvas silently exits rather than throwing.

describe('ColourPickerPopup — drawCanvas with null context:', () => {
  let popup: ColourPickerPopup

  beforeEach(() => {
    popup = new ColourPickerPopup()
    document.body.append(popup.dom)
  })

  afterEach(() => {
    popup.dom.remove()
    vi.restoreAllMocks()
  })

  it('should silently return when the 2D canvas context is unavailable', () => {
    popup.open(fakeRect(), '#ff0000', () => {})
    vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue(null)
    const slider = hueSlider(popup)
    slider.value = '120'
    slider.dispatchEvent(new Event('input'))
    // drawCanvas returned early without throwing; popup remains open.
    expect(popup.isOpen).toBe(true)
  })
})
