/**
 * main.ts
 *
 * $Since: 2026-05-07
 */

import {MarkType, NodeType, TextEdit} from '@src/index'
import type {HeadingLevel, InsertTableOptions, TextAlignment, TextEditContent, TextEditHandle} from '@src/types'
import {ColourPickerPopup} from '@src/overlays/utils'

const SAMPLE_CONTENT: TextEditContent = {
  type: NodeType.Doc,
  content: [

    // -- Heading levels ------------------------------------------------------

    {
      type: NodeType.Heading,
      attrs: {level: 1},
      content: [{type: NodeType.Text, text: 'Formatting Reference'}],
    },
    {
      type: NodeType.Heading,
      attrs: {level: 2},
      content: [{type: NodeType.Text, text: 'Heading 2 — Lorem ipsum dolor sit amet'}],
    },
    {
      type: NodeType.Heading,
      attrs: {level: 3},
      content: [{type: NodeType.Text, text: 'Heading 3 — consectetur adipiscing elit'}],
    },
    {
      type: NodeType.Heading,
      attrs: {level: 4},
      content: [{type: NodeType.Text, text: 'Heading 4 — sed do eiusmod tempor'}],
    },
    {
      type: NodeType.Heading,
      attrs: {level: 5},
      content: [{type: NodeType.Text, text: 'Heading 5 — incididunt ut labore'}],
    },
    {
      type: NodeType.Heading,
      attrs: {level: 6},
      content: [{type: NodeType.Text, text: 'Heading 6 — et dolore magna aliqua'}],
    },

    // -- Inline marks --------------------------------------------------------

    {
      type: NodeType.Heading,
      attrs: {level: 2},
      content: [{type: NodeType.Text, text: 'Inline Marks'}],
    },
    {
      type: NodeType.Paragraph,
      content: [
        {type: NodeType.Text, marks: [{type: MarkType.Bold}], text: 'Bold'},
        {type: NodeType.Text, text: ',  '},
        {type: NodeType.Text, marks: [{type: MarkType.Italic}], text: 'italic'},
        {type: NodeType.Text, text: ',  '},
        {type: NodeType.Text, marks: [{type: MarkType.Strike}], text: 'strikethrough'},
        {type: NodeType.Text, text: ',  '},
        {type: NodeType.Text, marks: [{type: MarkType.Underline}], text: 'underline'},
        {type: NodeType.Text, text: ',  '},
        {type: NodeType.Text, marks: [{type: MarkType.Code}], text: 'inline code'},
        {type: NodeType.Text, text: ',  '},
        {type: NodeType.Text, marks: [{type: MarkType.TextStyle, attrs: {color: '#e03e3e'}}], text: 'red text'},
        {type: NodeType.Text, text: ',  '},
        {type: NodeType.Text, marks: [{type: MarkType.TextStyle, attrs: {color: '#2563eb'}}], text: 'blue text'},
        {type: NodeType.Text, text: ',  '},
        {type: NodeType.Text, marks: [{type: MarkType.Highlight, attrs: {color: '#fef08a'}}], text: 'yellow highlight'},
        {type: NodeType.Text, text: ',  '},
        {type: NodeType.Text, marks: [{type: MarkType.Highlight, attrs: {color: '#bbf7d0'}}], text: 'green highlight'},
        {type: NodeType.Text, text: '.'},
      ],
    },
    {
      type: NodeType.Paragraph,
      content: [
        {type: NodeType.Text, text: 'Superscript: E = mc'},
        {type: NodeType.Text, marks: [{type: MarkType.Superscript}], text: '2'},
        {type: NodeType.Text, text: '.  Subscript: H'},
        {type: NodeType.Text, marks: [{type: MarkType.Subscript}], text: '2'},
        {type: NodeType.Text, text: 'O.  Combined: '},
        {
          type: NodeType.Text,
          marks: [{type: MarkType.Bold}, {type: MarkType.Italic}, {type: MarkType.Underline}],
          text: 'bold italic underline'
        },
        {type: NodeType.Text, text: '.'},
      ],
    },
    {
      type: NodeType.Paragraph,
      content: [
        {type: NodeType.Text, text: 'Hyperlink: '},
        {
          type: NodeType.Text,
          marks: [{type: MarkType.Link, attrs: {href: 'https://tiptap.dev'}}],
          text: 'TipTap documentation'
        },
        {type: NodeType.Text, text: ',  '},
        {
          type: NodeType.Text,
          marks: [{type: MarkType.Bold}, {type: MarkType.Link, attrs: {href: 'https://tiptap.dev/extensions'}}],
          text: 'bold link'
        },
        {type: NodeType.Text, text: '.'},
      ],
    },

    // -- Lists ----------------------------------------------------------------

    {
      type: NodeType.Heading,
      attrs: {level: 2},
      content: [{type: NodeType.Text, text: 'Lists'}],
    },
    {
      type: NodeType.BulletList,
      content: [
        {
          type: NodeType.ListItem,
          content: [{
            type: NodeType.Paragraph,
            content: [{type: NodeType.Text, text: 'Bullet item — Lorem ipsum dolor sit amet'}]
          }]
        },
        {
          type: NodeType.ListItem,
          content: [{
            type: NodeType.Paragraph,
            content: [{type: NodeType.Text, text: 'Bullet item — consectetur adipiscing elit'}]
          }]
        },
        {
          type: NodeType.ListItem,
          content: [{
            type: NodeType.Paragraph,
            content: [{type: NodeType.Text, text: 'Bullet item — sed do eiusmod tempor incididunt'}]
          }]
        },
      ],
    },
    {
      type: NodeType.OrderedList,
      attrs: {start: 1},
      content: [
        {
          type: NodeType.ListItem,
          content: [{
            type: NodeType.Paragraph,
            content: [{type: NodeType.Text, text: 'First step — initialise the project'}]
          }]
        },
        {
          type: NodeType.ListItem,
          content: [{
            type: NodeType.Paragraph,
            content: [{type: NodeType.Text, text: 'Second step — configure extensions'}]
          }]
        },
        {
          type: NodeType.ListItem,
          content: [{
            type: NodeType.Paragraph,
            content: [{type: NodeType.Text, text: 'Third step — deploy to production'}]
          }]
        },
      ],
    },
    {
      type: NodeType.TaskList,
      content: [
        {
          type: NodeType.TaskItem,
          attrs: {checked: true},
          content: [{type: NodeType.Paragraph, content: [{type: NodeType.Text, text: 'Design the API surface'}]}]
        },
        {
          type: NodeType.TaskItem,
          attrs: {checked: true},
          content: [{type: NodeType.Paragraph, content: [{type: NodeType.Text, text: 'Write unit tests'}]}]
        },
        {
          type: NodeType.TaskItem,
          attrs: {checked: false},
          content: [{
            type: NodeType.Paragraph,
            content: [{type: NodeType.Text, text: 'Integrate with the wiki frontend'}]
          }]
        },
        {
          type: NodeType.TaskItem,
          attrs: {checked: false},
          content: [{type: NodeType.Paragraph, content: [{type: NodeType.Text, text: 'Publish release notes'}]}]
        },
      ],
    },

    // -- Alignment -----------------------------------------------------------

    {
      type: NodeType.Heading,
      attrs: {level: 2},
      content: [{type: NodeType.Text, text: 'Alignment'}],
    },
    {
      type: NodeType.Paragraph,
      attrs: {textAlign: 'left'},
      content: [{
        type: NodeType.Text,
        text: 'Left-aligned — Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.'
      }],
    },
    {
      type: NodeType.Paragraph,
      attrs: {textAlign: 'center'},
      content: [{
        type: NodeType.Text,
        text: 'Centre-aligned — Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.'
      }],
    },
    {
      type: NodeType.Paragraph,
      attrs: {textAlign: 'right'},
      content: [{
        type: NodeType.Text,
        text: 'Right-aligned — Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.'
      }],
    },

    // -- Block elements -------------------------------------------------------

    {
      type: NodeType.Heading,
      attrs: {level: 2},
      content: [{type: NodeType.Text, text: 'Block Elements'}],
    },
    {
      type: NodeType.Blockquote,
      content: [{
        type: NodeType.Paragraph,
        content: [{type: NodeType.Text, text: '"Simplicity is the ultimate sophistication." — Leonardo da Vinci'}]
      }],
    },
    {
      type: NodeType.CodeBlock,
      attrs: {language: 'typescript'},
      content: [{
        type: NodeType.Text,
        text: 'function greet(name: string): string {\n  return `Hello, ${name}!`\n}\n\nconsole.log(greet(\'World\'))'
      }],
    },
    {
      type: NodeType.Table,
      content: [
        {
          type: NodeType.TableRow,
          content: [
            {
              type: NodeType.TableHeader,
              content: [{type: NodeType.Paragraph, content: [{type: NodeType.Text, text: 'Feature'}]}]
            },
            {
              type: NodeType.TableHeader,
              content: [{type: NodeType.Paragraph, content: [{type: NodeType.Text, text: 'Status'}]}]
            },
            {
              type: NodeType.TableHeader,
              content: [{type: NodeType.Paragraph, content: [{type: NodeType.Text, text: 'Notes'}]}]
            },
          ],
        },
        {
          type: NodeType.TableRow,
          content: [
            {
              type: NodeType.TableCell,
              content: [{type: NodeType.Paragraph, content: [{type: NodeType.Text, text: 'Text formatting'}]}]
            },
            {
              type: NodeType.TableCell,
              content: [{type: NodeType.Paragraph, content: [{type: NodeType.Text, text: '✓ Complete'}]}]
            },
            {
              type: NodeType.TableCell,
              content: [{
                type: NodeType.Paragraph,
                content: [{type: NodeType.Text, text: 'Bold, italic, strike, underline, code'}]
              }]
            },
          ],
        },
        {
          type: NodeType.TableRow,
          content: [
            {
              type: NodeType.TableCell,
              content: [{type: NodeType.Paragraph, content: [{type: NodeType.Text, text: 'Colour'}]}]
            },
            {
              type: NodeType.TableCell,
              content: [{type: NodeType.Paragraph, content: [{type: NodeType.Text, text: '✓ Complete'}]}]
            },
            {
              type: NodeType.TableCell,
              content: [{type: NodeType.Paragraph, content: [{type: NodeType.Text, text: 'Foreground and highlight'}]}]
            },
          ],
        },
        {
          type: NodeType.TableRow,
          content: [
            {
              type: NodeType.TableCell,
              content: [{type: NodeType.Paragraph, content: [{type: NodeType.Text, text: 'Alignment'}]}]
            },
            {
              type: NodeType.TableCell,
              content: [{type: NodeType.Paragraph, content: [{type: NodeType.Text, text: '✓ Complete'}]}]
            },
            {
              type: NodeType.TableCell,
              content: [{type: NodeType.Paragraph, content: [{type: NodeType.Text, text: 'Left, centre, right'}]}]
            },
          ],
        },
      ],
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
      case 'heading':
        editor.toggleHeading(Number(arg) as HeadingLevel);
        break
      case 'bold':
        editor.toggleBold();
        break
      case 'italic':
        editor.toggleItalic();
        break
      case 'strike':
        editor.toggleStrike();
        break
      case 'underline':
        editor.toggleUnderline();
        break
      case 'code':
        editor.toggleCode();
        break
      case 'superscript':
        editor.toggleSuperscript();
        break
      case 'subscript':
        editor.toggleSubscript();
        break
      case 'bulletList':
        editor.toggleBulletList();
        break
      case 'orderedList':
        editor.toggleOrderedList();
        break
      case 'taskList':
        editor.toggleTaskList();
        break
      case 'align':
        editor.setTextAlign(arg as TextAlignment);
        break
      case 'blockquote':
        editor.toggleBlockquote();
        break
      case 'codeBlock':
        editor.toggleCodeBlock();
        break
      case 'link': {
        if (editor.isActive('link')) {
          editor.unsetLink()
        } else {
          const url = window.prompt('URL')
          if (url) editor.setLink(url)
        }
        break
      }
      case 'insertTable':
        editor.insertTable();
        break
    }
  })

  const fgBtn = document.getElementById('colour-fg-btn')
  const bgBtn = document.getElementById('colour-bg-btn')
  const fgSwatch = document.getElementById('colour-fg-swatch') as HTMLElement | null
  const bgSwatch = document.getElementById('colour-bg-swatch') as HTMLElement | null

  const fgPicker = new ColourPickerPopup('te-colour-input--fg')
  const bgPicker = new ColourPickerPopup('te-colour-input--bg')
  document.body.append(fgPicker.dom, bgPicker.dom)

  let currentFg = '#000000'
  let currentBg = '#ffff00'

  fgBtn?.addEventListener('mousedown', ev => {
    ev.preventDefault()
    const rect = fgBtn.getBoundingClientRect()
    fgPicker.open(rect, currentFg, colour => {
      currentFg = colour
      if (fgSwatch) fgSwatch.style.background = colour
      editor.setTextColour(colour)
    })
  })

  bgBtn?.addEventListener('mousedown', ev => {
    ev.preventDefault()
    const rect = bgBtn.getBoundingClientRect()
    bgPicker.open(rect, currentBg, colour => {
      currentBg = colour
      if (bgSwatch) bgSwatch.style.background = colour
      editor.setHighlightColour(colour)
    })
  })
}

function updateToolbarState(editor: TextEditHandle): void {
  const buttons = document.querySelectorAll<HTMLButtonElement>('#toolbar button[data-cmd]')
  buttons.forEach((btn) => {
    const cmd = btn.dataset['cmd']
    const arg = btn.dataset['arg']

    let isActive = false

    if (cmd === 'heading' && arg) {
      isActive = editor.isActive('heading', {level: Number(arg)})
    } else if (cmd === 'align' && arg) {
      const tiptapAlign = arg === 'centre' ? 'center' : arg
      isActive = editor.isActive('paragraph', {textAlign: tiptapAlign}) ||
        editor.isActive('heading', {textAlign: tiptapAlign})
    } else if (cmd && cmd !== 'insertTable') {
      isActive = editor.isActive(cmd)
    }

    btn.classList.toggle('is-active', isActive)
  })
}

function initialiseTablePicker(editor: TextEdit): void {
  const toggle = document.getElementById('table-picker-toggle')
  const picker = document.getElementById('table-picker')
  const wrap = document.getElementById('table-picker-wrap')
  const gridEl = document.getElementById('table-picker-grid')
  const sizeLabel = document.getElementById('table-picker-size')
  const headerCb = document.getElementById('table-include-header') as HTMLInputElement | null

  if (!toggle || !picker || !wrap || !gridEl || !sizeLabel || !headerCb) return

  const GRID_COLS = 10
  const GRID_ROWS = 10

  // Build 10×10 grid
  for (let r = 1; r <= GRID_ROWS; r++) {
    for (let c = 1; c <= GRID_COLS; c++) {
      const cell = document.createElement('div')
      cell.className = 'table-picker-cell'
      cell.dataset['row'] = String(r)
      cell.dataset['col'] = String(c)
      gridEl.appendChild(cell)
    }
  }

  function highlight(rows: number, cols: number): void {
    gridEl!.querySelectorAll<HTMLElement>('.table-picker-cell').forEach((cell) => {
      cell.classList.toggle('is-active',
        Number(cell.dataset['row']) <= rows && Number(cell.dataset['col']) <= cols,
      )
    })
    sizeLabel!.textContent = `${cols} × ${rows}`
  }

  function openPicker(): void {
    picker!.hidden = false
    toggle!.setAttribute('aria-expanded', 'true')
    highlight(1, 1)
  }

  function closePicker(): void {
    picker!.hidden = true
    toggle!.setAttribute('aria-expanded', 'false')
    sizeLabel!.textContent = ''
  }

  toggle.addEventListener('click', (e) => {
    e.stopPropagation()
    picker.hidden ? openPicker() : closePicker()
  })

  gridEl.addEventListener('mouseover', (e) => {
    const cell = (e.target as HTMLElement).closest<HTMLElement>('.table-picker-cell')
    if (cell) highlight(Number(cell.dataset['row']), Number(cell.dataset['col']))
  })

  gridEl.addEventListener('click', (e) => {
    const cell = (e.target as HTMLElement).closest<HTMLElement>('.table-picker-cell')
    if (!cell) return
    const options: InsertTableOptions = {
      rows: Number(cell.dataset['row']),
      cols: Number(cell.dataset['col']),
      withHeaderRow: headerCb.checked,
    }
    editor.insertTable(options)
    closePicker()
    editor.focus()
  })

  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && !picker.hidden) closePicker()
  })

  document.addEventListener('click', (e) => {
    if (!picker.hidden && !wrap.contains(e.target as Node)) closePicker()
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

function registerHotkeys(editor: TextEdit): void {
  // H1–H6 (Ctrl+Alt+1–6) are handled natively by TipTap's HeadingExtension.
  document.addEventListener('keydown', (e) => {
    if (!e.ctrlKey || !e.altKey) return

    switch (e.code) {
      // Styling
      case 'KeyB':
        e.preventDefault();
        editor.toggleBold();
        break
      case 'KeyI':
        e.preventDefault();
        editor.toggleItalic();
        break
      case 'KeyS':
        e.preventDefault();
        editor.toggleStrike();
        break
      case 'KeyU':
        e.preventDefault();
        editor.toggleUnderline();
        break
      case 'KeyZ':
        e.preventDefault();
        editor.toggleCode();
        break
      case 'KeyC':
        e.preventDefault();
        document.getElementById('colour-fg-btn')?.click();
        break
      case 'KeyV':
        e.preventDefault();
        document.getElementById('colour-bg-btn')?.click();
        break

      // Script
      case 'BracketRight':
        e.preventDefault();
        editor.toggleSuperscript();
        break
      case 'BracketLeft':
        e.preventDefault();
        editor.toggleSubscript();
        break

      // Listing
      case 'Equal':
        e.preventDefault();
        editor.toggleOrderedList();
        break
      case 'Minus':
        e.preventDefault();
        editor.toggleBulletList();
        break
      case 'Semicolon':
        e.preventDefault();
        editor.toggleTaskList();
        break

      // Alignment
      case 'KeyJ':
        e.preventDefault();
        editor.setTextAlign('left');
        break
      case 'KeyK':
        e.preventDefault();
        editor.setTextAlign('centre');
        break
      case 'KeyL':
        e.preventDefault();
        editor.setTextAlign('right');
        break
    }
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
      updateToolbarState(handle)
    },
  })

  initialiseToolbar(editor)
  initialiseTablePicker(editor)
  initialiseDevPanel(editor)
  registerHotkeys(editor)
})
