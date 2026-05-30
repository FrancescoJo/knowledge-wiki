/**
 * note-viewer.test.ts
 *
 * $Since: 2026-05-31
 */

import {afterEach, beforeEach, describe, expect, it} from 'vitest'
import {initNoteViewer} from '@src/note-viewer'

describe('initNoteViewer:', () => {
  let root: HTMLDivElement

  beforeEach(() => {
    root = document.createElement('div')
    document.body.appendChild(root)
  })

  afterEach(() => {
    document.body.removeChild(root)
  })

  it('renders Markdown from textarea into note-body', () => {
    root.innerHTML = `
      <div id="note-body"></div>
      <textarea id="note-editor-content"># Hello World</textarea>
    `

    initNoteViewer()

    const noteBody = document.getElementById('note-body')!
    expect(noteBody.innerHTML).toBe('<h1>Hello World</h1>\n')
  })

  it('does nothing when note-body is absent', () => {
    root.innerHTML = `<textarea id="note-editor-content"># Hello</textarea>`

    expect(() => initNoteViewer()).not.toThrow()
  })

  it('does nothing when textarea is absent', () => {
    root.innerHTML = `<div id="note-body"></div>`

    initNoteViewer()

    const noteBody = document.getElementById('note-body')!
    expect(noteBody.innerHTML).toBe('')
  })
})
