/**
 * TextEdit.test.ts
 *
 * $Since: 2026-05-07
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { TextSelection, NodeSelection } from '@tiptap/pm/state'
import { GapCursor } from '@tiptap/pm/gapcursor'
import { TableMap } from '@tiptap/pm/tables'
import { TextEdit } from '@src/TextEdit'
import { NodeType, type TextEditContent } from '@src/types'
import {
  mountElement, pmView, pmState, setPmSelection, dispatchKeydown, setCellSelection, setCursorInCell, COLSPAN_TABLE_DOC,
  docNodes, isEditable, rowCells, tableRows, proseMirrorEl, PARAGRAPH_DOC, CODEBLOCK_DOC, BLOCKQUOTE_DOC, HEADING_2_DOC,
  EMPTY_DOC
} from './test-utils'

// 3×3 table: row 0 = header row (A, B, C); rows 1–2 = body rows (1,2,3 / 4,5,6)
const TABLE_DOC: TextEditContent = {
  type: NodeType.Doc,
  content: [{
    type: NodeType.Table,
    content: [
      {
        type: NodeType.TableRow,
        content: [
          { type: NodeType.TableHeader, content: [{ type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: 'A' }] }] },
          { type: NodeType.TableHeader, content: [{ type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: 'B' }] }] },
          { type: NodeType.TableHeader, content: [{ type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: 'C' }] }] },
        ],
      },
      {
        type: NodeType.TableRow,
        content: [
          { type: NodeType.TableCell, content: [{ type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: '1' }] }] },
          { type: NodeType.TableCell, content: [{ type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: '2' }] }] },
          { type: NodeType.TableCell, content: [{ type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: '3' }] }] },
        ],
      },
      {
        type: NodeType.TableRow,
        content: [
          { type: NodeType.TableCell, content: [{ type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: '4' }] }] },
          { type: NodeType.TableCell, content: [{ type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: '5' }] }] },
          { type: NodeType.TableCell, content: [{ type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: '6' }] }] },
        ],
      },
    ],
  }],
}

// All-body-cell table (no header row) — rowIsHeader returns false → firstDataRow=0
// in doSortColumn (fires the `:0` branch at line 632).
const HEADERLESS_TABLE_DOC: TextEditContent = {
  type: NodeType.Doc,
  content: [{
    type: NodeType.Table,
    content: [
      {
        type: NodeType.TableRow,
        content: [
          { type: NodeType.TableCell, content: [{ type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: 'cherry' }] }] },
          { type: NodeType.TableCell, content: [{ type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: '3' }] }] },
        ],
      },
      {
        type: NodeType.TableRow,
        content: [
          { type: NodeType.TableCell, content: [{ type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: 'apple' }] }] },
          { type: NodeType.TableCell, content: [{ type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: '1' }] }] },
        ],
      },
      {
        type: NodeType.TableRow,
        content: [
          { type: NodeType.TableCell, content: [{ type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: 'banana' }] }] },
          { type: NodeType.TableCell, content: [{ type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: '2' }] }] },
        ],
      },
    ],
  }],
}

// 1 header + 1 body row — map.height - firstDataRow = 1, which is not > 1,
// so doSortColumn returns early (line 634 guard).
const ONE_ROW_TABLE_DOC: TextEditContent = {
  type: NodeType.Doc,
  content: [{
    type: NodeType.Table,
    content: [
      {
        type: NodeType.TableRow,
        content: [
          { type: NodeType.TableHeader, content: [{ type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: 'Name' }] }] },
        ],
      },
      {
        type: NodeType.TableRow,
        content: [
          { type: NodeType.TableCell, content: [{ type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: 'only row' }] }] },
        ],
      },
    ],
  }],
}


// -- Tests ---------------------------------------------------------------------

describe('TextEdit:', () => {
  let element: HTMLElement
  let editor: TextEdit | undefined

  beforeEach(() => {
    element = mountElement()
  })

  // noinspection DuplicatedCode: teardown is local to each suite to capture its own editor and element bindings
  afterEach(() => {
    try { editor?.destroy() } catch { /* already destroyed */ }
    editor = undefined
    element.remove()
  })

  // -- constructor -------------------------------------------------------------

  describe('constructor:', () => {
    describe('when called with a DOM element and no content:', () => {
      it('should attach the editor to the host element', () => {
        editor = new TextEdit({ element })
        expect(proseMirrorEl(element)).not.toBeNull()
      })

      it('should be editable by default', () => {
        editor = new TextEdit({ element })
        expect(isEditable(element)).toBe(true)
      })

      it('should produce a document node as initial content', () => {
        editor = new TextEdit({ element })
        expect(editor.getContent().type).toBe('doc')
      })
    })

    describe('when called with initial content:', () => {
      it('should load the provided content into the document', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        expect(editor.getContent()).toMatchObject(PARAGRAPH_DOC)
      })
    })

    describe('when called with readOnly set to true:', () => {
      it('should initialise the editor as non-editable', () => {
        editor = new TextEdit({ element, readOnly: true })
        expect(isEditable(element)).toBe(false)
      })
    })
  })

  // -- getContent --------------------------------------------------------------

  describe('getContent:', () => {
    describe('when content was provided at construction:', () => {
      it('should return a JSON object matching the initial content', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        expect(editor.getContent()).toMatchObject(PARAGRAPH_DOC)
      })
    })

    describe('when no content was provided at construction:', () => {
      it('should return a document containing an empty paragraph node', () => {
        editor = new TextEdit({ element })
        expect(docNodes(editor)[0]).toMatchObject({ type: NodeType.Paragraph })
      })
    })
  })

  // -- setContent --------------------------------------------------------------

  describe('setContent:', () => {
    describe('when called with a new document:', () => {
      it('should replace the entire document with the given content', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.setContent(HEADING_2_DOC)
        expect(editor.getContent()).toMatchObject(HEADING_2_DOC)
      })
    })
  })

  // -- setReadOnly -------------------------------------------------------------

  describe('setReadOnly:', () => {
    describe('when passed true:', () => {
      it('should mark the editor as non-editable', () => {
        editor = new TextEdit({ element })
        editor.setReadOnly(true)
        expect(isEditable(element)).toBe(false)
      })
    })

    describe('when passed false after being made read-only:', () => {
      it('should restore editability', () => {
        editor = new TextEdit({ element, readOnly: true })
        editor.setReadOnly(false)
        expect(isEditable(element)).toBe(true)
      })
    })
  })

  // -- isActive ----------------------------------------------------------------

  describe('isActive:', () => {
    describe('when the current block is a heading after toggleHeading is called:', () => {
      it('should return true for heading with the matching level', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.toggleHeading(2)
        expect(editor.isActive('heading', { level: 2 })).toBe(true)
      })

      it('should return false for heading with a different level', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.toggleHeading(2)
        expect(editor.isActive('heading', { level: 1 })).toBe(false)
      })
    })

    describe('when the current block is a plain paragraph:', () => {
      it('should return false for heading', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        expect(editor.isActive('heading')).toBe(false)
      })
    })

    describe('when bold is toggled on without a selection:', () => {
      it('should return true for bold', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.toggleBold()
        expect(editor.isActive('bold')).toBe(true)
      })
    })
  })

  // -- isFocused ---------------------------------------------------------------

  describe('isFocused:', () => {
    describe('when the editor has not received focus:', () => {
      it('should return false', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        expect(editor.isFocused()).toBe(false)
      })
    })
  })

  // -- focus -------------------------------------------------------------------

  describe('focus:', () => {
    describe('when called:', () => {
      it('should report the editor as focused', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.focus()
        expect(editor.isFocused()).toBe(true)
      })
    })
  })

  // -- toggleBold --------------------------------------------------------------

  describe('toggleBold:', () => {
    describe('when called once without an active selection:', () => {
      it('should activate the bold stored mark', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.toggleBold()
        expect(editor.isActive('bold')).toBe(true)
      })
    })

    describe('when called twice without an active selection:', () => {
      it('should deactivate the bold stored mark', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.toggleBold()
        editor.toggleBold()
        expect(editor.isActive('bold')).toBe(false)
      })
    })
  })

  // -- toggleItalic ------------------------------------------------------------

  describe('toggleItalic:', () => {
    describe('when called once without an active selection:', () => {
      it('should activate the italic stored mark', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.toggleItalic()
        expect(editor.isActive('italic')).toBe(true)
      })
    })

    describe('when called twice without an active selection:', () => {
      it('should deactivate the italic stored mark', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.toggleItalic()
        editor.toggleItalic()
        expect(editor.isActive('italic')).toBe(false)
      })
    })
  })

  // -- toggleStrike ------------------------------------------------------------

  describe('toggleStrike:', () => {
    describe('when called once without an active selection:', () => {
      it('should activate the strike stored mark', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.toggleStrike()
        expect(editor.isActive('strike')).toBe(true)
      })
    })

    describe('when called twice without an active selection:', () => {
      it('should deactivate the strike stored mark', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.toggleStrike()
        editor.toggleStrike()
        expect(editor.isActive('strike')).toBe(false)
      })
    })
  })

  // -- toggleCode --------------------------------------------------------------

  describe('toggleCode:', () => {
    describe('when called once without an active selection:', () => {
      it('should activate the inline code stored mark', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.toggleCode()
        expect(editor.isActive('code')).toBe(true)
      })
    })

    describe('when called twice without an active selection:', () => {
      it('should deactivate the inline code stored mark', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.toggleCode()
        editor.toggleCode()
        expect(editor.isActive('code')).toBe(false)
      })
    })
  })

  // -- setLink / unsetLink ----------------------------------------------------

  describe('setLink:', () => {
    describe('when called with a URL and text is selected:', () => {
      it('should apply the link mark to the selected text', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        const view = pmView(editor)
        // "Hello, world." is 13 chars; pos 1 = start of text, pos 14 = end
        view.dispatch(view.state.tr.setSelection(TextSelection.create(view.state.doc, 1, 14)))
        editor.setLink('https://example.com')
        expect(editor.isActive('link')).toBe(true)
      })
    })
  })

  describe('unsetLink:', () => {
    describe('when called after setLink:', () => {
      it('should remove the link mark', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        const view = pmView(editor)
        view.dispatch(view.state.tr.setSelection(TextSelection.create(view.state.doc, 1, 14)))
        editor.setLink('https://example.com')
        editor.unsetLink()
        expect(editor.isActive('link')).toBe(false)
      })
    })
  })

  // -- toggleHeading -----------------------------------------------------------

  describe('toggleHeading:', () => {
    describe('when called with level 1 on a paragraph:', () => {
      it('should convert the current block to a level-1 heading', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.toggleHeading(1)
        expect(editor.isActive('heading', { level: 1 })).toBe(true)
      })
    })

    describe('when called with level 2 on a paragraph:', () => {
      it('should convert the current block to a level-2 heading', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.toggleHeading(2)
        expect(editor.isActive('heading', { level: 2 })).toBe(true)
      })
    })

    describe('when called with level 3 on a paragraph:', () => {
      it('should convert the current block to a level-3 heading', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.toggleHeading(3)
        expect(editor.isActive('heading', { level: 3 })).toBe(true)
      })
    })

    describe('when called twice with the same level:', () => {
      it('should revert the block back to a paragraph', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.toggleHeading(2)
        editor.toggleHeading(2)
        expect(editor.isActive('heading')).toBe(false)
      })
    })

    describe('when called with a different level on an existing heading:', () => {
      it('should change the heading to the new level', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.toggleHeading(2)
        editor.toggleHeading(4)
        expect(editor.isActive('heading', { level: 4 })).toBe(true)
      })
    })
  })

  // -- toggleUnderline ---------------------------------------------------------

  describe('toggleUnderline:', () => {
    describe('when called once without an active selection:', () => {
      it('should activate the underline stored mark', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.toggleUnderline()
        expect(editor.isActive('underline')).toBe(true)
      })
    })

    describe('when called twice without an active selection:', () => {
      it('should deactivate the underline stored mark', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.toggleUnderline()
        editor.toggleUnderline()
        expect(editor.isActive('underline')).toBe(false)
      })
    })
  })

  // -- setTextColour ------------------------------------------------------------

  describe('setTextColour:', () => {
    describe('when called with a colour value:', () => {
      it('should apply the textStyle mark with the given colour', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.setTextColour('#ff0000')
        expect(editor.isActive('textStyle', { color: '#ff0000' })).toBe(true)
      })
    })
  })

  // -- unsetTextColour ----------------------------------------------------------

  describe('unsetTextColour:', () => {
    describe('when called after setTextColour:', () => {
      it('should remove the textStyle colour mark', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.setTextColour('#ff0000')
        editor.unsetTextColour()
        expect(editor.isActive('textStyle', { color: '#ff0000' })).toBe(false)
      })
    })
  })

  // -- setHighlightColour -------------------------------------------------------

  describe('setHighlightColour:', () => {
    describe('when called with a colour value:', () => {
      it('should apply the highlight mark with the given colour', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.setHighlightColour('#ffff00')
        expect(editor.isActive('highlight', { color: '#ffff00' })).toBe(true)
      })
    })
  })

  // -- unsetHighlightColour -----------------------------------------------------

  describe('unsetHighlightColour:', () => {
    describe('when called after setHighlightColour:', () => {
      it('should remove the highlight mark', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.setHighlightColour('#ffff00')
        editor.unsetHighlightColour()
        expect(editor.isActive('highlight', { color: '#ffff00' })).toBe(false)
      })
    })
  })

  // -- toggleSuperscript --------------------------------------------------------

  describe('toggleSuperscript:', () => {
    describe('when called once without an active selection:', () => {
      it('should activate the superscript mark', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.toggleSuperscript()
        expect(editor.isActive('superscript')).toBe(true)
      })
    })

    describe('when called twice without an active selection:', () => {
      it('should deactivate the superscript mark', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.toggleSuperscript()
        editor.toggleSuperscript()
        expect(editor.isActive('superscript')).toBe(false)
      })
    })
  })

  // -- toggleSubscript ----------------------------------------------------------

  describe('toggleSubscript:', () => {
    describe('when called once without an active selection:', () => {
      it('should activate the subscript mark', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.toggleSubscript()
        expect(editor.isActive('subscript')).toBe(true)
      })
    })

    describe('when called twice without an active selection:', () => {
      it('should deactivate the subscript mark', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.toggleSubscript()
        editor.toggleSubscript()
        expect(editor.isActive('subscript')).toBe(false)
      })
    })
  })

  // -- setTextAlign -------------------------------------------------------------

  describe('setTextAlign:', () => {
    describe('when called with "centre":', () => {
      it('should apply centre alignment to the current block', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.setTextAlign('centre')
        expect(docNodes(editor)[0]).toMatchObject({ attrs: { textAlign: 'center' } })
      })
    })

    describe('when called with "right":', () => {
      it('should apply right alignment to the current block', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.setTextAlign('right')
        expect(docNodes(editor)[0]).toMatchObject({ attrs: { textAlign: 'right' } })
      })
    })

    describe('when called with "left" after another alignment:', () => {
      it('should apply left alignment to the current block', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.setTextAlign('right')
        editor.setTextAlign('left')
        expect(docNodes(editor)[0]).toMatchObject({ attrs: { textAlign: 'left' } })
      })
    })
  })

  // -- toggleBulletList --------------------------------------------------------

  describe('toggleBulletList:', () => {
    describe('when called on a plain paragraph:', () => {
      it('should wrap the block in a bullet list', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.toggleBulletList()
        expect(editor.isActive('bulletList')).toBe(true)
      })
    })

    describe('when called on an existing bullet list item:', () => {
      it('should unwrap the bullet list', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.toggleBulletList()
        editor.toggleBulletList()
        expect(editor.isActive('bulletList')).toBe(false)
      })
    })
  })

  // -- toggleOrderedList -------------------------------------------------------

  describe('toggleOrderedList:', () => {
    describe('when called on a plain paragraph:', () => {
      it('should wrap the block in an ordered list', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.toggleOrderedList()
        expect(editor.isActive('orderedList')).toBe(true)
      })
    })

    describe('when called on an existing ordered list item:', () => {
      it('should unwrap the ordered list', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.toggleOrderedList()
        editor.toggleOrderedList()
        expect(editor.isActive('orderedList')).toBe(false)
      })
    })
  })

  // -- toggleTaskList ----------------------------------------------------------

  describe('toggleTaskList:', () => {
    describe('when called on a plain paragraph:', () => {
      it('should wrap the block in a task list', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.toggleTaskList()
        expect(editor.isActive('taskList')).toBe(true)
      })
    })

    describe('when called on an existing task list item:', () => {
      it('should unwrap the task list', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.toggleTaskList()
        editor.toggleTaskList()
        expect(editor.isActive('taskList')).toBe(false)
      })
    })
  })

  // -- toggleBlockquote --------------------------------------------------------

  describe('toggleBlockquote:', () => {
    describe('when called on a plain paragraph:', () => {
      it('should wrap the block in a blockquote', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.toggleBlockquote()
        expect(editor.isActive('blockquote')).toBe(true)
      })
    })

    describe('when called on an existing blockquote:', () => {
      it('should unwrap the blockquote', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.toggleBlockquote()
        editor.toggleBlockquote()
        expect(editor.isActive('blockquote')).toBe(false)
      })
    })
  })

  // -- toggleCodeBlock ---------------------------------------------------------

  describe('toggleCodeBlock:', () => {
    describe('when called on a plain paragraph:', () => {
      it('should convert the block to a code block', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.toggleCodeBlock()
        expect(editor.isActive('codeBlock')).toBe(true)
      })
    })

    describe('when called on an existing code block:', () => {
      it('should convert the block back to a paragraph', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.toggleCodeBlock()
        editor.toggleCodeBlock()
        expect(editor.isActive('codeBlock')).toBe(false)
      })
    })
  })

  // -- insertTable -------------------------------------------------------------

  describe('insertTable:', () => {
    describe('when called with no arguments:', () => {
      it('should insert a table node into the document', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.insertTable()
        const hasTable = docNodes(editor).some(node => node['type'] === 'table')
        expect(hasTable).toBe(true)
      })

      it('should insert a table with 2 rows by default', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.insertTable()
        const table = docNodes(editor).find(node => node['type'] === 'table') as Record<string, unknown>
        const rows = table['content'] as unknown[]
        expect(rows).toHaveLength(2)
      })
    })

    describe('when called with rows: 3 and cols: 4:', () => {
      it('should insert a table with the specified dimensions', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.insertTable({ rows: 3, cols: 4, withHeaderRow: false })
        const table = docNodes(editor).find(node => node['type'] === 'table') as Record<string, unknown>
        const rows = table['content'] as unknown[]
        expect(rows).toHaveLength(3)
        const firstRow = rows[0] as Record<string, unknown>
        const cells = firstRow['content'] as unknown[]
        expect(cells).toHaveLength(4)
      })
    })

    describe('when called with withHeaderRow: true:', () => {
      it('should make the first row a header row', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.insertTable({ rows: 2, cols: 2, withHeaderRow: true })
        const table = docNodes(editor).find(node => node['type'] === 'table') as Record<string, unknown>
        const rows = table['content'] as unknown[]
        const firstRow = rows[0] as Record<string, unknown>
        const firstCells = firstRow['content'] as Record<string, unknown>[]
        expect(firstCells[0]['type']).toBe('tableHeader')
      })
    })

    describe('when called with withHeaderRow: false:', () => {
      it('should make all rows body rows', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        editor.insertTable({ rows: 2, cols: 2, withHeaderRow: false })
        const table = docNodes(editor).find(node => node['type'] === 'table') as Record<string, unknown>
        const rows = table['content'] as unknown[]
        const firstRow = rows[0] as Record<string, unknown>
        const firstCells = firstRow['content'] as Record<string, unknown>[]
        expect(firstCells[0]['type']).toBe('tableCell')
      })
    })
  })

  // -- isAtObjectBoundary ------------------------------------------------------

  describe('isAtObjectBoundary:', () => {
    describe('when cursor is inside a plain paragraph:', () => {
      it('should return false', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        expect(editor.isAtObjectBoundary()).toBe(false)
      })
    })

    describe('when text cursor is inside a codeBlock (not at a gap):', () => {
      it('should return false', () => {
        editor = new TextEdit({ element, content: CODEBLOCK_DOC })
        const { state } = pmView(editor)
        const codeBlock = state.doc.firstChild!
        const insidePos = 1 + Math.floor(codeBlock.content.size / 2)
        setPmSelection(editor, TextSelection.create(state.doc, insidePos))
        expect(editor.isAtObjectBoundary()).toBe(false)
      })
    })

    describe('when a GapCursor is positioned after a codeBlock:', () => {
      it('should return true', () => {
        editor = new TextEdit({ element, content: CODEBLOCK_DOC })
        const { state } = pmView(editor)
        const gapPos = state.doc.firstChild!.nodeSize
        setPmSelection(editor, new GapCursor(state.doc.resolve(gapPos)))
        expect(editor.isAtObjectBoundary()).toBe(true)
      })
    })

    describe('when a GapCursor is positioned after a blockquote:', () => {
      it('should return true', () => {
        editor = new TextEdit({ element, content: BLOCKQUOTE_DOC })
        const { state } = pmView(editor)
        const gapPos = state.doc.firstChild!.nodeSize
        setPmSelection(editor, new GapCursor(state.doc.resolve(gapPos)))
        expect(editor.isAtObjectBoundary()).toBe(true)
      })
    })
  })

  // -- ObjectExitCursor — keyboard navigation ----------------------------------

  describe('ObjectExitCursor keyboard navigation:', () => {
    describe('when ArrowRight is pressed with the cursor at the end of a codeBlock:', () => {
      it('should place a GapCursor in the gap after the codeBlock', () => {
        editor = new TextEdit({ element, content: CODEBLOCK_DOC })
        const state = pmState(editor)
        const codeBlock = state.doc.firstChild!
        const endOfCodeBlock = 1 + codeBlock.content.size
        setPmSelection(editor, TextSelection.create(state.doc, endOfCodeBlock))

        dispatchKeydown(editor, 'ArrowRight')

        expect(pmState(editor).selection).toBeInstanceOf(GapCursor)
      })
    })

    describe('when ArrowRight is pressed mid-codeBlock (not at end):', () => {
      it('should not create a GapCursor', () => {
        editor = new TextEdit({ element, content: CODEBLOCK_DOC })
        const state = pmState(editor)
        setPmSelection(editor, TextSelection.create(state.doc, 2))

        dispatchKeydown(editor, 'ArrowRight')

        expect(pmState(editor).selection).not.toBeInstanceOf(GapCursor)
      })
    })

    describe('when ArrowLeft is pressed from a GapCursor after a codeBlock:', () => {
      it('should move the text cursor back inside the codeBlock', () => {
        editor = new TextEdit({ element, content: CODEBLOCK_DOC })
        const state = pmState(editor)
        const codeBlock = state.doc.firstChild!
        const gapPos = codeBlock.nodeSize
        setPmSelection(editor, new GapCursor(state.doc.resolve(gapPos)))

        dispatchKeydown(editor, 'ArrowLeft')

        const { selection } = pmState(editor)
        expect(selection).toBeInstanceOf(TextSelection)
        expect((selection as TextSelection).$head.parent.type.name).toBe('codeBlock')
      })
    })

    describe('when Shift+ArrowLeft is pressed from a GapCursor after a codeBlock:', () => {
      it('should select the entire codeBlock as a NodeSelection', () => {
        editor = new TextEdit({ element, content: CODEBLOCK_DOC })
        const state = pmState(editor)
        const codeBlock = state.doc.firstChild!
        const gapPos = codeBlock.nodeSize
        setPmSelection(editor, new GapCursor(state.doc.resolve(gapPos)))

        dispatchKeydown(editor, 'ArrowLeft', true)

        const { selection } = pmState(editor)
        expect(selection).toBeInstanceOf(NodeSelection)
        expect((selection as NodeSelection).node.type.name).toBe('codeBlock')
      })
    })

    describe('when ArrowLeft is pressed from a GapCursor after a blockquote:', () => {
      it('should move the text cursor back inside the blockquote', () => {
        editor = new TextEdit({ element, content: BLOCKQUOTE_DOC })
        const state = pmState(editor)
        const blockquote = state.doc.firstChild!
        const gapPos = blockquote.nodeSize
        setPmSelection(editor, new GapCursor(state.doc.resolve(gapPos)))

        dispatchKeydown(editor, 'ArrowLeft')

        const { selection } = pmState(editor)
        expect(selection).toBeInstanceOf(TextSelection)
        const parent = (selection as TextSelection).$head.node(1)
        expect(parent.type.name).toBe('blockquote')
      })
    })

    describe('when Shift+ArrowLeft is pressed from a GapCursor after a blockquote:', () => {
      it('should select the entire blockquote as a NodeSelection', () => {
        editor = new TextEdit({ element, content: BLOCKQUOTE_DOC })
        const state = pmState(editor)
        const blockquote = state.doc.firstChild!
        const gapPos = blockquote.nodeSize
        setPmSelection(editor, new GapCursor(state.doc.resolve(gapPos)))

        dispatchKeydown(editor, 'ArrowLeft', true)

        const { selection } = pmState(editor)
        expect(selection).toBeInstanceOf(NodeSelection)
        expect((selection as NodeSelection).node.type.name).toBe('blockquote')
      })
    })
  })

  // -- isInTable ---------------------------------------------------------------

  describe('isInTable:', () => {
    describe('when the cursor is inside a table cell:', () => {
      it('should return true', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 0, 0)
        expect(editor.isInTable()).toBe(true)
      })
    })

    describe('when the cursor is in a plain paragraph:', () => {
      it('should return false', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        expect(editor.isInTable()).toBe(false)
      })
    })
  })

  // -- canMergeCells ------------------------------------------------------------

  describe('canMergeCells:', () => {
    describe('when a CellSelection spans multiple cells:', () => {
      it('should return true', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCellSelection(editor, 1, 0, 1, 1)
        expect(editor.canMergeCells()).toBe(true)
      })
    })

    describe('when the selection is a regular text cursor:', () => {
      it('should return false', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 0, 0)
        expect(editor.canMergeCells()).toBe(false)
      })
    })

    describe('when a CellSelection spans multiple rows in a single column:', () => {
      it('should return true via the row-span operand of the || check', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        // rows 0–1, col 0 only: rect.right-rect.left=1 (not >1), so the second
        // operand rect.bottom-rect.top>1 is evaluated and fires the true branch.
        setCellSelection(editor, 0, 0, 1, 0)
        expect(editor.canMergeCells()).toBe(true)
      })
    })
  })

  // -- canSplitCell -------------------------------------------------------------

  describe('canSplitCell:', () => {
    describe('when the cursor is in a normal (non-merged) cell:', () => {
      it('should return false', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 0, 0)
        expect(editor.canSplitCell()).toBe(false)
      })
    })

    describe('when the cursor is in a merged cell:', () => {
      it('should return true', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCellSelection(editor, 1, 0, 1, 1)
        editor.mergeCells()
        expect(editor.canSplitCell()).toBe(true)
      })
    })

    describe('when the cursor is outside a table:', () => {
      it('should return false', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        expect(editor.canSplitCell()).toBe(false)
      })
    })
  })

  // -- isTableFixedColumnWidths --------------------------------------------------

  describe('isTableFixedColumnWidths:', () => {
    describe('when the table uses default column widths:', () => {
      it('should return false', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 0, 0)
        expect(editor.isTableFixedColumnWidths()).toBe(false)
      })
    })

    describe('after setTableFixedColumnWidths(true):', () => {
      it('should return true', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 0, 0)
        editor.setTableFixedColumnWidths(true)
        expect(editor.isTableFixedColumnWidths()).toBe(true)
      })
    })
  })

  // -- isTableHeaderRow ---------------------------------------------------------

  describe('isTableHeaderRow:', () => {
    describe('when the first row consists of header cells:', () => {
      it('should return true', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 0, 0)
        expect(editor.isTableHeaderRow()).toBe(true)
      })
    })

    describe('after toggleHeaderRow converts the header row to body cells:', () => {
      it('should return false', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 0, 0)
        editor.toggleHeaderRow()
        expect(editor.isTableHeaderRow()).toBe(false)
      })
    })

    describe('when the cursor is outside a table:', () => {
      it('should return false', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        expect(editor.isTableHeaderRow()).toBe(false)
      })
    })
  })

  // -- isTableHeaderColumn -------------------------------------------------------

  describe('isTableHeaderColumn:', () => {
    describe('when the first column of body rows contains body cells:', () => {
      it('should return false', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 0, 0)
        expect(editor.isTableHeaderColumn()).toBe(false)
      })
    })

    describe('after toggleHeaderColumn converts the first column to header cells:', () => {
      it('should return true', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 0, 0)
        editor.toggleHeaderColumn()
        expect(editor.isTableHeaderColumn()).toBe(true)
      })
    })

    describe('when the cursor is outside a table:', () => {
      it('should return false', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        expect(editor.isTableHeaderColumn()).toBe(false)
      })
    })
  })

  // -- setTableFixedColumnWidths -------------------------------------------------

  describe('setTableFixedColumnWidths:', () => {
    describe('when called with true:', () => {
      it('should enable fixed column widths on the table', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 0, 0)
        editor.setTableFixedColumnWidths(true)
        expect(editor.isTableFixedColumnWidths()).toBe(true)
      })
    })

    describe('when called with false after being enabled:', () => {
      it('should disable fixed column widths on the table', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 0, 0)
        editor.setTableFixedColumnWidths(true)
        editor.setTableFixedColumnWidths(false)
        expect(editor.isTableFixedColumnWidths()).toBe(false)
      })
    })
  })

  // -- addColumnAfter ------------------------------------------------------------

  describe('addColumnAfter:', () => {
    describe('when the cursor is inside a table:', () => {
      it('should insert a column to the right of the current column', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 0, 0)
        editor.addColumnAfter()
        expect(rowCells(editor, 0)).toHaveLength(4)
      })
    })
  })

  // -- addColumnBefore -----------------------------------------------------------

  describe('addColumnBefore:', () => {
    describe('when the cursor is inside a table:', () => {
      it('should insert a column to the left of the current column', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 0, 1)
        editor.addColumnBefore()
        expect(rowCells(editor, 0)).toHaveLength(4)
      })
    })
  })

  // -- deleteColumn --------------------------------------------------------------

  describe('deleteColumn:', () => {
    describe('when the cursor is inside a table:', () => {
      it('should remove the column containing the cursor', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 0, 0)
        editor.deleteColumn()
        expect(rowCells(editor, 0)).toHaveLength(2)
      })
    })
  })

  // -- addRowAfter ---------------------------------------------------------------

  describe('addRowAfter:', () => {
    describe('when the cursor is inside a table:', () => {
      it('should insert a row below the current row', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 0, 0)
        editor.addRowAfter()
        expect(tableRows(editor)).toHaveLength(4)
      })
    })
  })

  // -- addRowBefore --------------------------------------------------------------

  describe('addRowBefore:', () => {
    describe('when the cursor is in a body row:', () => {
      it('should insert a row above the current row', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 1, 0)
        editor.addRowBefore()
        expect(tableRows(editor)).toHaveLength(4)
      })
    })
  })

  // -- deleteRow -----------------------------------------------------------------

  describe('deleteRow:', () => {
    describe('when the cursor is inside a table:', () => {
      it('should remove the row containing the cursor', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 1, 0)
        editor.deleteRow()
        expect(tableRows(editor)).toHaveLength(2)
      })
    })
  })

  // -- mergeCells ----------------------------------------------------------------

  describe('mergeCells:', () => {
    describe('when a CellSelection spans multiple cells:', () => {
      it('should merge the selected cells into one cell with the combined colspan', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCellSelection(editor, 1, 0, 1, 1)
        editor.mergeCells()
        const cell = rowCells(editor, 1)[0] as Record<string, unknown>
        expect((cell['attrs'] as Record<string, unknown>)['colspan']).toBe(2)
      })
    })
  })

  // -- splitCell -----------------------------------------------------------------

  describe('splitCell:', () => {
    describe('when the cursor is in a merged cell:', () => {
      it('should split the cell back into individual cells', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCellSelection(editor, 1, 0, 1, 1)
        editor.mergeCells()
        editor.splitCell()
        expect(rowCells(editor, 1)).toHaveLength(3)
      })
    })
  })

  // -- toggleHeaderRow -----------------------------------------------------------

  describe('toggleHeaderRow:', () => {
    describe('when the table has a header row:', () => {
      it('should convert the first row to body cells', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 0, 0)
        editor.toggleHeaderRow()
        expect(rowCells(editor, 0)[0]['type']).toBe('tableCell')
      })
    })
  })

  // -- toggleHeaderColumn --------------------------------------------------------

  describe('toggleHeaderColumn:', () => {
    describe('when the table has no header column:', () => {
      it('should convert the first column of body rows to header cells', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 0, 0)
        editor.toggleHeaderColumn()
        expect(rowCells(editor, 1)[0]['type']).toBe('tableHeader')
      })
    })
  })

  // -- setCellBackground ---------------------------------------------------------

  describe('setCellBackground:', () => {
    describe('when called with a colour value:', () => {
      it('should apply the background colour attribute to the current cell', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 1, 0)
        editor.setCellBackground('#ffeecc')
        const cell = rowCells(editor, 1)[0] as Record<string, unknown>
        expect((cell['attrs'] as Record<string, unknown>)['background']).toBe('#ffeecc')
      })
    })
  })

  // -- clearCellBackground -------------------------------------------------------

  describe('clearCellBackground:', () => {
    describe('when called after setCellBackground:', () => {
      it('should remove the background colour from the current cell', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 1, 0)
        editor.setCellBackground('#ffeecc')
        editor.clearCellBackground()
        const cell = rowCells(editor, 1)[0] as Record<string, unknown>
        expect((cell['attrs'] as Record<string, unknown>)['background']).not.toBe('#ffeecc')
      })
    })
  })

  // -- distributeColumns ---------------------------------------------------------

  describe('distributeColumns:', () => {
    describe('when called with the cursor inside a table:', () => {
      it('should complete without error', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        const ed = editor
        setCursorInCell(ed, 0, 0)
        expect(() => ed.distributeColumns()).not.toThrow()
      })
    })

    describe('when the table has an explicit tableWidth attribute:', () => {
      it('should set colwidth on every cell based on the table width divided by column count', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 0, 0)
        // Set tableWidth so distributeColumns uses the explicit-width path (lines 510-511).
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        ;(editor as any).editor.chain().focus().updateAttributes('table', { tableWidth: 600 }).run()
        editor.distributeColumns()
        // 3 columns in TABLE_DOC, 600 / 3 = 200 per column.
        const view = pmView(editor)
        const tableNode = view.state.doc.firstChild!
        const map = TableMap.get(tableNode)
        const cellOffset = map.positionAt(0, 0, tableNode)
        const cell = tableNode.nodeAt(cellOffset)!
        expect(cell.attrs['colwidth']).toEqual([200])
      })
    })

    describe('when the cursor is outside a table:', () => {
      it('should return without modifying the document', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        expect(() => editor!.distributeColumns()).not.toThrow()
      })
    })

    describe('when nodeDOM returns a non-HTMLElement (no explicit tableWidth):', () => {
      it('should return early without setting colwidths', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 0, 0)
        // nodeDOM returns a TextNode → tableDom = null → renderedWidth = 0 → return
        vi.spyOn(pmView(editor), 'nodeDOM').mockReturnValueOnce(document.createTextNode('x'))
        expect(() => editor!.distributeColumns()).not.toThrow()
        vi.restoreAllMocks()
      })
    })

    describe('when the table has a colspan=2 cell and an explicit tableWidth:', () => {
      it('should skip the duplicate map offset via the visited set', () => {
        editor = new TextEdit({ element, content: COLSPAN_TABLE_DOC })
        setCursorInCell(editor, 0, 0)
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        ;(editor as any).editor.chain().focus().updateAttributes('table', { tableWidth: 400 }).run()
        editor.distributeColumns()
        // colspan=2, 400/2=200 → colwidth=[200,200] on the header cell
        const view = pmView(editor)
        const tableNode = view.state.doc.firstChild!
        const headerCell = tableNode.firstChild!.firstChild!
        expect(headerCell.attrs['colwidth']).toEqual([200, 200])
      })
    })
  })

  // -- clearCells ----------------------------------------------------------------

  describe('clearCells:', () => {
    describe('when called with the cursor inside a cell:', () => {
      it('should replace the cell content with an empty paragraph', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 1, 0)
        editor.clearCells()
        const cell = rowCells(editor, 1)[0] as Record<string, unknown>
        const content = cell['content'] as Record<string, unknown>[]
        expect(content).toHaveLength(1)
        expect(content[0]['type']).toBe('paragraph')
        expect(content[0]['content']).toBeUndefined()
      })
    })

    describe('when called with a single-cell CellSelection:', () => {
      it('should clear the content of the selected cell', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        // CellSelection with anchor === head selects exactly one cell.
        setCellSelection(editor, 1, 1, 1, 1)
        editor.clearCells()
        const cell = rowCells(editor, 1)[1] as Record<string, unknown>
        const content = cell['content'] as Record<string, unknown>[]
        expect(content).toHaveLength(1)
        expect(content[0]['type']).toBe('paragraph')
        expect(content[0]['content']).toBeUndefined()
      })
    })

    describe('when called outside a table:', () => {
      it('should return without modifying the document', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        expect(() => editor!.clearCells()).not.toThrow()
      })
    })
  })

  // -- moveColumnLeft ------------------------------------------------------------

  describe('moveColumnLeft:', () => {
    describe('when the cursor is in a column that is not the first:', () => {
      it('should shift the column one position to the left', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 0, 1) // col 1 = "B"
        editor.moveColumnLeft()
        // "B" is now at index 0
        expect(rowCells(editor, 0)[0]).toMatchObject({
          content: [{ type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: 'B' }] }],
        })
      })
    })

    describe('when the cursor is in the first column:', () => {
      it('should leave the table unchanged', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 0, 0) // col 0 = "A"
        editor.moveColumnLeft()
        expect(rowCells(editor, 0)[0]).toMatchObject({
          content: [{ type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: 'A' }] }],
        })
      })
    })

    describe('when called outside a table:', () => {
      it('should return without modifying the document', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        expect(() => editor!.moveColumnLeft()).not.toThrow()
      })
    })
  })

  // -- moveColumnRight -----------------------------------------------------------

  describe('moveColumnRight:', () => {
    describe('when the cursor is in a column that is not the last:', () => {
      it('should shift the column one position to the right', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 0, 1) // col 1 = "B"
        editor.moveColumnRight()
        // "B" is now at index 2
        expect(rowCells(editor, 0)[2]).toMatchObject({
          content: [{ type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: 'B' }] }],
        })
      })
    })

    describe('when the cursor is in the last column:', () => {
      it('should leave the table unchanged', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 0, 2) // col 2 = "C"
        editor.moveColumnRight()
        expect(rowCells(editor, 0)[2]).toMatchObject({
          content: [{ type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: 'C' }] }],
        })
      })
    })

    describe('when called outside a table:', () => {
      it('should return without modifying the document', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        expect(() => editor!.moveColumnRight()).not.toThrow()
      })
    })
  })

  // -- moveRowUp -----------------------------------------------------------------

  describe('moveRowUp:', () => {
    describe('when the cursor is in a row that is not the first:', () => {
      it('should shift the row one position up', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 1, 0) // row 1 = "1, 2, 3"
        editor.moveRowUp()
        // "1, 2, 3" is now at index 0
        expect(rowCells(editor, 0)[0]).toMatchObject({
          content: [{ type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: '1' }] }],
        })
      })
    })

    describe('when the cursor is in the first row:', () => {
      it('should leave the table unchanged', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 0, 0) // row 0 = "A, B, C"
        editor.moveRowUp()
        expect(rowCells(editor, 0)[0]).toMatchObject({
          content: [{ type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: 'A' }] }],
        })
      })
    })

    describe('when called outside a table:', () => {
      it('should return without modifying the document', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        expect(() => editor!.moveRowUp()).not.toThrow()
      })
    })
  })

  // -- moveRowDown ---------------------------------------------------------------

  describe('moveRowDown:', () => {
    describe('when the cursor is in a row that is not the last:', () => {
      it('should shift the row one position down', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 1, 0) // row 1 = "1, 2, 3"
        editor.moveRowDown()
        // "1, 2, 3" is now at index 2
        expect(rowCells(editor, 2)[0]).toMatchObject({
          content: [{ type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: '1' }] }],
        })
      })
    })

    describe('when the cursor is in the last row:', () => {
      it('should leave the table unchanged', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 2, 0) // row 2 = "4, 5, 6"
        editor.moveRowDown()
        expect(rowCells(editor, 2)[0]).toMatchObject({
          content: [{ type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: '4' }] }],
        })
      })
    })

    describe('when called outside a table:', () => {
      it('should return without modifying the document', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        expect(() => editor!.moveRowDown()).not.toThrow()
      })
    })
  })

  // -- sortColumnAscending -------------------------------------------------------

  describe('sortColumnAscending:', () => {
    describe('when data rows are in descending order:', () => {
      it('should reorder data rows so column text is ascending', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 1, 0)
        editor.sortColumnDescending() // row 1 = "4,…", row 2 = "1,…"
        setCursorInCell(editor, 1, 0)
        editor.sortColumnAscending() // restores: row 1 = "1,…"
        expect(rowCells(editor, 1)[0]).toMatchObject({
          content: [{ type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: '1' }] }],
        })
      })
    })

    describe('when called outside a table:', () => {
      it('should return without modifying the document', () => {
        editor = new TextEdit({ element, content: PARAGRAPH_DOC })
        expect(() => editor!.sortColumnAscending()).not.toThrow()
      })
    })

    describe('when the table has no header row:', () => {
      it('should treat all rows as data rows and sort from row 0 (firstDataRow=0)', () => {
        editor = new TextEdit({ element, content: HEADERLESS_TABLE_DOC })
        setCursorInCell(editor, 0, 0)
        editor.sortColumnAscending()
        // 'apple' < 'banana' < 'cherry' → row 0 is now 'apple'
        expect(rowCells(editor, 0)[0]).toMatchObject({
          content: [{ type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: 'apple' }] }],
        })
      })
    })

    describe('when the table has only one data row:', () => {
      it('should return without reordering (only 1 row to sort)', () => {
        editor = new TextEdit({ element, content: ONE_ROW_TABLE_DOC })
        setCursorInCell(editor, 1, 0)
        const before = editor.getContent()
        editor.sortColumnAscending()
        expect(editor.getContent()).toEqual(before)
      })
    })

    describe('when the column data is already sorted in ascending order:', () => {
      it('should return without dispatching a transaction', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 1, 0)
        // TABLE_DOC body col 0 is "1" then "4" — already ascending
        const before = editor.getContent()
        editor.sortColumnAscending()
        expect(editor.getContent()).toEqual(before)
      })
    })
  })

  // -- sortColumnDescending ------------------------------------------------------

  describe('sortColumnDescending:', () => {
    describe('when data rows are in ascending order:', () => {
      it('should reorder data rows so column text is descending', () => {
        editor = new TextEdit({ element, content: TABLE_DOC })
        setCursorInCell(editor, 1, 0)
        editor.sortColumnDescending()
        // "4" is now at row 1 (was row 2)
        expect(rowCells(editor, 1)[0]).toMatchObject({
          content: [{ type: NodeType.Paragraph, content: [{ type: NodeType.Text, text: '4' }] }],
        })
      })
    })
  })

  // -- destroy -----------------------------------------------------------------

  describe('destroy:', () => {
    describe('when called:', () => {
      it('should remove the editor from the host element', () => {
        editor = new TextEdit({ element })
        editor.destroy()
        expect(proseMirrorEl(element)).toBeNull()
        editor = undefined
      })
    })
  })

  // -- onChange callback -------------------------------------------------------

  describe('onChange callback:', () => {
    describe('when the document is replaced via setContent:', () => {
      it('should call onChange with an object containing the updated content', () => {
        const onChange = vi.fn()
        editor = new TextEdit({ element, content: EMPTY_DOC, onChange })
        editor.setContent(PARAGRAPH_DOC)
        expect(onChange).toHaveBeenCalledWith(expect.objectContaining({ type: NodeType.Doc }))
      })
    })
  })

  // -- onSelectionChange callback ----------------------------------------------

  describe('onSelectionChange callback:', () => {
    describe('when a transaction is dispatched:', () => {
      it('should call onSelectionChange with a handle that exposes isActive', () => {
        const onSelectionChange = vi.fn()
        editor = new TextEdit({ element, content: PARAGRAPH_DOC, onSelectionChange })
        editor.setContent(HEADING_2_DOC)
        expect(onSelectionChange).toHaveBeenCalled()
        const handle = onSelectionChange.mock.calls[0][0] as { isActive: unknown }
        expect(typeof handle.isActive).toBe('function')
      })
    })
  })
})
