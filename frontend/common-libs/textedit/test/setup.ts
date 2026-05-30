/**
 * setup.ts
 *
 * Global test setup. Provides a minimal canvas stub so that jsdom does not
 * emit "not-implemented" warnings when ColourPickerPopup creates a canvas.
 *
 * $Since: 2026-05-18
 */

const ctx2d = {
  fillRect: () => { /* stub */
  },
  createLinearGradient: () => ({
    addColorStop: () => { /* stub */
    }
  }),
  set fillStyle(_: string) { /* stub */
  },
}

Object.defineProperty(HTMLCanvasElement.prototype, 'getContext', {
  value: (type: string) => type === '2d' ? ctx2d : null,
})
