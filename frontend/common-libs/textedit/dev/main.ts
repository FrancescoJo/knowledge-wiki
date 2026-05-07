/**
 * main.ts
 *
 * $Since: 2026-05-07
 */

import { TextEdit } from '../src/index'
import type { TextEditContent, HeadingLevel } from '../src/index'

const SAMPLE_CONTENT: TextEditContent = {
  type: 'doc',
  content: [
    {
      type: 'heading',
      attrs: { level: 1 },
      content: [{ type: 'text', text: 'Project Architecture Overview' }],
    },
    {
      type: 'paragraph',
      content: [
        { type: 'text', text: 'This document describes the high-level architecture of the ' },
        { type: 'text', marks: [{ type: 'bold' }], text: 'Knowledge Wiki' },
        { type: 'text', text: ' project, including technology choices and module boundaries.' },
      ],
    },
    {
      type: 'heading',
      attrs: { level: 2 },
      content: [{ type: 'text', text: 'Technology Stack' }],
    },
    {
      type: 'bulletList',
      content: [
        {
          type: 'listItem',
          content: [{ type: 'paragraph', content: [
            { type: 'text', marks: [{ type: 'bold' }], text: 'Backend: ' },
            { type: 'text', text: 'Kotlin / JVM, Spring Boot, Spring Web MVC' },
          ]}],
        },
        {
          type: 'listItem',
          content: [{ type: 'paragraph', content: [
            { type: 'text', marks: [{ type: 'bold' }], text: 'Database: ' },
            { type: 'text', text: 'PostgreSQL with pgvector extension' },
          ]}],
        },
        {
          type: 'listItem',
          content: [{ type: 'paragraph', content: [
            { type: 'text', marks: [{ type: 'bold' }], text: 'Frontend: ' },
            { type: 'text', text: 'HTMX, TypeScript, TipTap core' },
          ]}],
        },
      ],
    },
    {
      type: 'heading',
      attrs: { level: 2 },
      content: [{ type: 'text', text: 'Development Milestones' }],
    },
    {
      type: 'taskList',
      content: [
        {
          type: 'taskItem',
          attrs: { checked: true },
          content: [{ type: 'paragraph', content: [{ type: 'text', text: 'Define product roadmap' }] }],
        },
        {
          type: 'taskItem',
          attrs: { checked: true },
          content: [{ type: 'paragraph', content: [{ type: 'text', text: 'Finalise technology stack' }] }],
        },
        {
          type: 'taskItem',
          attrs: { checked: true },
          content: [{ type: 'paragraph', content: [{ type: 'text', text: 'Initialise textedit module' }] }],
        },
        {
          type: 'taskItem',
          attrs: { checked: false },
          content: [{ type: 'paragraph', content: [{ type: 'text', text: 'Implement v0.1 backend' }] }],
        },
        {
          type: 'taskItem',
          attrs: { checked: false },
          content: [{ type: 'paragraph', content: [{ type: 'text', text: 'Integrate textedit into wiki frontend' }] }],
        },
      ],
    },
    {
      type: 'heading',
      attrs: { level: 2 },
      content: [{ type: 'text', text: 'Module Structure' }],
    },
    {
      type: 'codeBlock',
      attrs: { language: 'text' },
      content: [{ type: 'text', text: 'frontend/\n  common-libs/\n    textedit/   ← this module\n\nbackend/\n  wiki/\n    app/\n    core/\n    infrastructure/' }],
    },
    {
      type: 'heading',
      attrs: { level: 2 },
      content: [{ type: 'text', text: 'Decision Log' }],
    },
    {
      type: 'table',
      content: [
        {
          type: 'tableRow',
          content: [
            { type: 'tableHeader', content: [{ type: 'paragraph', content: [{ type: 'text', text: 'Decision' }] }] },
            { type: 'tableHeader', content: [{ type: 'paragraph', content: [{ type: 'text', text: 'Choice' }] }] },
            { type: 'tableHeader', content: [{ type: 'paragraph', content: [{ type: 'text', text: 'Rationale' }] }] },
          ],
        },
        {
          type: 'tableRow',
          content: [
            { type: 'tableCell', content: [{ type: 'paragraph', content: [{ type: 'text', text: 'Backend framework' }] }] },
            { type: 'tableCell', content: [{ type: 'paragraph', content: [{ type: 'text', text: 'Spring Boot' }] }] },
            { type: 'tableCell', content: [{ type: 'paragraph', content: [{ type: 'text', text: 'Deep existing expertise' }] }] },
          ],
        },
        {
          type: 'tableRow',
          content: [
            { type: 'tableCell', content: [{ type: 'paragraph', content: [{ type: 'text', text: 'Database' }] }] },
            { type: 'tableCell', content: [{ type: 'paragraph', content: [{ type: 'text', text: 'PostgreSQL + pgvector' }] }] },
            { type: 'tableCell', content: [{ type: 'paragraph', content: [{ type: 'text', text: 'Vector search support without separate DB' }] }] },
          ],
        },
        {
          type: 'tableRow',
          content: [
            { type: 'tableCell', content: [{ type: 'paragraph', content: [{ type: 'text', text: 'Editor library' }] }] },
            { type: 'tableCell', content: [{ type: 'paragraph', content: [{ type: 'text', text: 'TipTap core' }] }] },
            { type: 'tableCell', content: [{ type: 'paragraph', content: [{ type: 'text', text: 'No React dependency, mature ecosystem' }] }] },
          ],
        },
      ],
    },
    {
      type: 'blockquote',
      content: [{
        type: 'paragraph',
        content: [{ type: 'text', text: 'Simplicity is the ultimate sophistication.' }],
      }],
    },
  ],
}

function initialiseToolbar(editor: TextEdit): void {
  const toolbar = document.getElementById('toolbar')
  if (!toolbar) return

  toolbar.addEventListener('click', (e) => {
    const target = (e.target as HTMLElement).closest<HTMLButtonElement>('button[data-cmd]')
    if (!target) return

    const cmd = target.dataset['cmd']
    const arg = target.dataset['arg']

    switch (cmd) {
      case 'bold':           editor.toggleBold(); break
      case 'italic':         editor.toggleItalic(); break
      case 'strike':         editor.toggleStrike(); break
      case 'code':           editor.toggleCode(); break
      case 'heading':        editor.setHeading(Number(arg) as HeadingLevel); break
      case 'paragraph':      editor.setParagraph(); break
      case 'bulletList':     editor.toggleBulletList(); break
      case 'orderedList':    editor.toggleOrderedList(); break
      case 'taskList':       editor.toggleTaskList(); break
      case 'blockquote':     editor.toggleBlockquote(); break
      case 'codeBlock':      editor.toggleCodeBlock(); break
      case 'insertTable':    editor.insertTable(); break
    }
  })
}

function updateToolbarState(editor: TextEdit): void {
  const buttons = document.querySelectorAll<HTMLButtonElement>('#toolbar button[data-cmd]')
  buttons.forEach((btn) => {
    const cmd = btn.dataset['cmd']
    const arg = btn.dataset['arg']
    const attrs = arg ? { level: Number(arg) } : undefined
    const name = cmd === 'paragraph' ? 'paragraph' : (cmd ?? '')
    btn.classList.toggle('is-active', editor.isActive(name, attrs))
  })
}

function initialiseDevPanel(editor: TextEdit): void {
  const jsonOutput = document.getElementById('json-output')
  const btnGetJson = document.getElementById('btn-get-json')
  const btnToggleReadonly = document.getElementById('btn-toggle-readonly')
  const btnLoadSample = document.getElementById('btn-load-sample')

  let isReadOnly = false

  btnGetJson?.addEventListener('click', () => {
    if (jsonOutput) {
      jsonOutput.textContent = JSON.stringify(editor.getContent(), null, 2)
    }
  })

  btnToggleReadonly?.addEventListener('click', () => {
    isReadOnly = !isReadOnly
    editor.setReadOnly(isReadOnly)
    if (btnToggleReadonly) {
      btnToggleReadonly.textContent = isReadOnly ? 'Make Editable' : 'Make Read-only'
    }
  })

  btnLoadSample?.addEventListener('click', () => {
    editor.setContent(SAMPLE_CONTENT)
    editor.focus()
  })
}

document.addEventListener('DOMContentLoaded', () => {
  const editorEl = document.getElementById('editor')
  if (!editorEl) throw new Error('Editor mount element #editor not found')

  const editor = new TextEdit({
    element: editorEl,
    content: SAMPLE_CONTENT,
    onChange: (content) => {
      const liveOutput = document.getElementById('json-output-live')
      if (liveOutput) {
        liveOutput.textContent = JSON.stringify(content, null, 2)
      }
    },
    onSelectionChange: (handle) => {
      updateToolbarState(handle as TextEdit)
    },
  })

  initialiseToolbar(editor)
  initialiseDevPanel(editor)
})
