# Frontend Utilities & Helpers Index

Index of all shared utility and helper code in the frontend modules.
Keep this file up to date whenever a helper is added, changed, or removed.

Entries follow the format:
```
(Call path)  :  (one-line description)  [source file]
```

---

## `frontend/common-libs/textedit`

> Source root: `frontend/common-libs/textedit/`

### Production Utilities

#### `src/utils.ts`

| Call path | Description |
|---|---|
| `hashJson(value: unknown): string` | djb2 + FNV-1a 32-bit hash of a JSON-serialisable value; ~64-bit collision resistance |

#### `src/overlays/utils.ts`

Shared helpers for overlay extensions (table, menu, drag-and-drop, colour).

**DOM helpers**

| Call path | Description |
|---|---|
| `mkItem(label, className, title?): HTMLButtonElement` | Creates a labelled toolbar button element |
| `mkSep(cssClass): HTMLHRElement` | Creates a separator `<hr>` element |
| `setDisabled(btn, disabled)` | Toggles the `disabled` attribute on a button |
| `bindMenuCloseListeners(opts)` | Registers pointer and keyboard listeners to close a floating menu |

**Drag-and-drop helpers**

| Call path | Description |
|---|---|
| `DRAG_THRESHOLD` | Minimum pixel distance (5) before a drag gesture is recognised |
| `createDragGhost(cssHandle, cssGhost): HTMLElement` | Creates and appends a drag ghost element to `document.body` |
| `updateDragGhost(ghost, x, y)` | Repositions the ghost to the given coordinates |
| `removeDragGhost(ghost)` | Removes the ghost element from the DOM |

**ProseMirror / table helpers**

| Call path | Description |
|---|---|
| `tableDepthOf($cell): number` | Returns the nesting depth of the table containing a resolved cell position |
| `tableNodeAt($cell)` | Returns the table node and its document start position for a resolved cell position |
| `clearCells(view, editor, tableNode, tableStart, getOffsets)` | Clears content from a set of table cells |

**Colour utilities**

| Call path | Description |
|---|---|
| `hsvToRgb(h, s, v)` | Converts HSV to RGB |
| `rgbToHsv(r, g, b)` | Converts RGB to HSV |
| `rgbToHex(r, g, b)` | Converts RGB to hex string |
| `hexToRgb(hex)` | Converts hex string to `{r, g, b}` |
| `clamp(n, lo, hi)` | Clamps a number to the range `[lo, hi]` |

**Input element factories**

| Call path | Description |
|---|---|
| `mkNumInput(min, max): HTMLInputElement` | Creates a number input restricted to `[min, max]` |
| `mkRgbGroup(inp, label): HTMLElement` | Creates a labelled RGB channel group element |

**Classes**

| Class | Description |
|---|---|
| `ColourPickerPopup` | Full colour picker popup (HSV canvas, RGB/Hex inputs, eyedropper) |

---

### Test Utilities

#### `test/test-utils.ts`

Shared data fixtures and DOM helpers for Vitest specs. Not included in the build output.

**Document fixtures** (`TextEditContent` objects)

| Constant | Description |
|---|---|
| `EMPTY_DOC` | Empty document |
| `PARAGRAPH_DOC` | Single paragraph |
| `HEADING_2_DOC` | Level-2 heading |
| `CODEBLOCK_DOC` | Code block |
| `BLOCKQUOTE_DOC` | Block quote |
| `TABLE_DOC` | Single table |
| `PARA_DOC` | Paragraph (alias) |
| `TABLE_AND_PARA_DOC` | Table followed by a paragraph |
| `TWO_TABLE_DOC` | Two consecutive tables |
| `COLSPAN_TABLE_DOC` | Table with a colspan cell |

**DOM / editor helpers**

| Call path | Description |
|---|---|
| `mountElement(): HTMLElement` | Creates and appends a fresh `<div>` to `document.body` for test mounting |
| `proseMirrorEl(host): Element \| null` | Queries `.ProseMirror` within the host element |
| `isEditable(host): boolean` | Returns whether the host element has `contenteditable="true"` |
| `pmView(editor)` | Accesses the internal `EditorView` of a `TextEdit` instance |
| `pmState(editor)` | Accesses the `EditorState` of a `TextEdit` instance |
| `getDoc(editor)` | Returns the current document content of a `TextEdit` instance |
| `docNodes(editor): Record<string, unknown>[]` | Returns the top-level node array of the document |
| `tableRows(editor): Record<string, unknown>[]` | Returns the row array of the first table in the document |
| `rowCells(editor, rowIndex): Record<string, unknown>[]` | Returns the cell array of the specified table row |
| `setPmSelection(editor, sel)` | Sets the ProseMirror selection programmatically |
| `setCellSelection(editor, r0, c0, r1, c1)` | Sets a table cell range selection |
| `setCursorInCell(editor, row, col)` | Places the cursor inside the specified table cell |
| `setTextSelectedAtCell0_0(editor): number` | Selects text in cell (0, 0) and returns the selection anchor |
| `setFixedColumnWidths(editor, fixed)` | Toggles fixed column widths on the first table |
| `dispatchKeydown(editor, key, shiftKey?)` | Dispatches a `keydown` event on the editor |
| `rect(x, y, w, h): DOMRect` | Constructs a `DOMRect` with the given dimensions |
