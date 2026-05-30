# textedit

Rich text editor module for Knowledge Wiki, built on [TipTap core](https://tiptap.dev).

- No UI framework dependency (no React, no Vue)
- Content serialised as JSON
- Bundled as a single ES module (`dist/textedit.js`)
- Heavy dependencies (@tiptap/\*, lowlight, highlight.js) are `peerDependencies` — the consuming app supplies them

---

## File Structure

```
src/
  TextEdit.ts            — Public API class
  types.ts               — TypeScript interfaces and type aliases
  utils.ts               — Shared utilities
  index.ts               — Module entry point
  extensions/            — TipTap extension set and custom extensions
  overlays/              — DOM overlay widgets (handles, menus, tooltips)
dev/
  index.html             — Development page (mirrors the wiki edit page structure)
  main.ts                — Development bootstrap (toolbar and dev panel wiring)
```

---

## Development

```bash
npm install
npm run dev       # serves dev/index.html at http://localhost:5173
npm run build     # outputs dist/textedit.js
npm run lint      # tsc --noEmit
```

`vite.config.ts` switches behaviour by command:

| Command      | Root   | Output                                             |
|--------------|--------|----------------------------------------------------|
| `vite` (dev) | `dev/` | served in browser with HMR                         |
| `vite build` | `.`    | `dist/textedit.js` (ES module, peer deps excluded) |

---

## Testing

All tests are **Small** tests (jsdom environment, no network, no filesystem).
Test files live under `test/` and must not be mixed into `src/`.

```bash
npm test              # run once — default reporter, per-file summary
npm run test:ci       # run once — dot reporter, less output (recommended for CI / AI)
npm run test:watch    # watch mode for active development
npm run coverage      # branch coverage via v8 (excludes types.ts and index.ts)
```

### Reducing output in AI-assisted workflows

`npm run test:ci` uses Vitest's `dot` reporter, which suppresses individual test names
for passing files and prints full error details only for failures.
It produces roughly half the terminal lines of the default reporter.

If you need even less output — for example, only the pass/fail summary —
pipe through PowerShell's `Select-String`:

```powershell
npm run test:ci 2>&1 | Select-String -NotMatch "^\s+✓"
```

Or on Unix:

```bash
npm run test:ci 2>&1 | grep -v "^\s*✓"
```

This leaves only failures, the summary counts, and timing.

### Coverage

```bash
npm run coverage
```

Generates a v8 branch-coverage report in the terminal.
Two files are excluded from measurement because they contain no executable code:

| File           | Reason                     |
|----------------|----------------------------|
| `src/types.ts` | TypeScript interfaces only |
| `src/index.ts` | Re-export barrel           |

---

## Usage

### TypeScript (recommended)

Install the package and its peer dependencies in the consuming app, then import:

```typescript
import { TextEdit } from '@wiki/textedit'
import type { TextEditContent, TextEditHandle } from '@wiki/textedit'

const editor = new TextEdit({
  element: document.getElementById('editor')!,
  content: savedJson,           // TextEditContent | null
  onChange: (content: TextEditContent) => {
    hiddenInput.value = JSON.stringify(content)
  },
  onSelectionChange: (handle: TextEditHandle) => {
    boldBtn.classList.toggle('is-active', handle.isActive('bold'))
  },
})
```

### Plain JavaScript

The same API without type annotations:

```javascript
import { TextEdit } from '@wiki/textedit'

const editor = new TextEdit({
  element: document.getElementById('editor'),
  content: null,
  onChange: (content) => {
    document.getElementById('content-input').value = JSON.stringify(content)
  },
})
```

### HTML — standard form submission

Wire the editor to a hidden `<input>` so the JSON value is posted with the form:

```html
<form method="post" action="/pages/1/edit">
  <div id="editor"></div>
  <input type="hidden" name="content" id="content-input">
  <button type="submit">Save</button>
</form>

<script type="module" src="/assets/wiki.js"></script>
```

```javascript
// wiki.js (after your bundler merges textedit + peer deps)
import { TextEdit } from '@wiki/textedit'

const editor = new TextEdit({
  element: document.getElementById('editor'),
  content: JSON.parse(document.getElementById('content-input').value || 'null'),
  onChange: (content) => {
    document.getElementById('content-input').value = JSON.stringify(content)
  },
})
```

The hidden input is pre-populated with the saved JSON by the server on page load;
the editor reads it as its initial content and keeps it in sync on every change.

### HTML — HTMX

```html
<form id="edit-form"
      hx-post="/pages/1/content"
      hx-target="#page-content"
      hx-swap="outerHTML">
  <div id="editor"></div>
  <input type="hidden" name="content" id="content-input">
  <button type="submit">Save</button>
</form>

<script type="module" src="/assets/wiki.js"></script>
```

```javascript
import { TextEdit } from '@wiki/textedit'

const editor = new TextEdit({
  element: document.getElementById('editor'),
  content: window.INITIAL_CONTENT ?? null,   // injected by the server template
  onChange: (content) => {
    document.getElementById('content-input').value = JSON.stringify(content)
  },
})
```

HTMX serialises the hidden input as part of the form POST.
No custom `htmx:configRequest` hook is needed.

---

## API

### Content

| Method                | Description                      |
|-----------------------|----------------------------------|
| `getContent()`        | Returns current document as JSON |
| `setContent(content)` | Replaces entire document         |

### State

| Method                   | Description                                                                                                 |
|--------------------------|-------------------------------------------------------------------------------------------------------------|
| `setReadOnly(boolean)`   | Toggles editable / read-only mode                                                                           |
| `isActive(name, attrs?)` | Returns true when the named mark or node is active at the current selection                                 |
| `isFocused()`            | Returns true when the editor has keyboard focus                                                             |
| `isDirty()`              | Returns true when the document has been modified since the editor was created                               |
| `isAtObjectBoundary()`   | Returns true when a GapCursor is positioned immediately after a block object (codeBlock, table, blockquote) |

### Focus

| Method    | Description                          |
|-----------|--------------------------------------|
| `focus()` | Moves keyboard focus into the editor |

### Heading

| Method                 | Description                                                                       |
|------------------------|-----------------------------------------------------------------------------------|
| `toggleHeading(level)` | `level` is `1`–`6`; if the block is already at that level, reverts to a paragraph |

### Text formatting

| Method              | Description |
|---------------------|-------------|
| `toggleBold()`      |             |
| `toggleItalic()`    |             |
| `toggleStrike()`    |             |
| `toggleUnderline()` |             |
| `toggleCode()`      | Inline code |

### Link

| Method          | Description                                      |
|-----------------|--------------------------------------------------|
| `setLink(href)` | Applies a hyperlink to the current selection     |
| `unsetLink()`   | Removes the hyperlink from the current selection |

### Colour

| Method                       | Description                                                    |
|------------------------------|----------------------------------------------------------------|
| `setTextColour(colour)`      | Applies foreground colour; CSS colour value (e.g. `'#ff0000'`) |
| `unsetTextColour()`          | Removes foreground colour                                      |
| `setHighlightColour(colour)` | Applies background highlight colour                            |
| `unsetHighlightColour()`     | Removes background highlight                                   |

### Script

| Method                | Description |
|-----------------------|-------------|
| `toggleSuperscript()` |             |
| `toggleSubscript()`   |             |

### Listing

| Method                | Description |
|-----------------------|-------------|
| `toggleBulletList()`  |             |
| `toggleOrderedList()` |             |
| `toggleTaskList()`    |             |

### Alignment

| Method                    | Description                                       |
|---------------------------|---------------------------------------------------|
| `setTextAlign(alignment)` | `alignment` is `'left'`, `'centre'`, or `'right'` |

### Objects

| Method                  | Description                                                                                                                    |
|-------------------------|--------------------------------------------------------------------------------------------------------------------------------|
| `toggleBlockquote()`    |                                                                                                                                |
| `toggleCodeBlock()`     |                                                                                                                                |
| `insertTable(options?)` | Inserts a table at the cursor position. Default: 2 rows × 2 columns with header row. Options: `rows`, `cols`, `withHeaderRow`. |

### Table state

| Method                       | Description                                                                |
|------------------------------|----------------------------------------------------------------------------|
| `isInTable()`                | Returns true when the cursor is inside a table cell                        |
| `canMergeCells()`            | Returns true when the selection spans multiple cells that can be merged    |
| `canSplitCell()`             | Returns true when the cursor is in a merged cell that can be split         |
| `isTableFixedColumnWidths()` | Returns true when the current table has fixed column widths enabled        |
| `isTableHeaderRow()`         | Returns true when the first row of the current table is a header row       |
| `isTableHeaderColumn()`      | Returns true when the first column of the current table is a header column |

### Table attributes

| Method                             | Description                                                  |
|------------------------------------|--------------------------------------------------------------|
| `setTableFixedColumnWidths(fixed)` | Enables or disables fixed column widths on the current table |

### Table structure

| Method                 | Description                                                |
|------------------------|------------------------------------------------------------|
| `addColumnBefore()`    | Inserts a column to the left of the cursor column          |
| `addColumnAfter()`     | Inserts a column to the right of the cursor column         |
| `deleteColumn()`       | Deletes the cursor column                                  |
| `addRowBefore()`       | Inserts a row above the cursor row                         |
| `addRowAfter()`        | Inserts a row below the cursor row                         |
| `deleteRow()`          | Deletes the cursor row                                     |
| `mergeCells()`         | Merges the selected cells into one                         |
| `splitCell()`          | Splits the cursor cell into its original constituent cells |
| `toggleHeaderRow()`    | Toggles the first row between header and body cells        |
| `toggleHeaderColumn()` | Toggles the first column between header and body cells     |

### Cell background

| Method                      | Description                                                           |
|-----------------------------|-----------------------------------------------------------------------|
| `setCellBackground(colour)` | Applies a background colour to the current cell or selected cells     |
| `clearCellBackground()`     | Removes the background colour from the current cell or selected cells |

### Table layout

| Method                | Description                                                  |
|-----------------------|--------------------------------------------------------------|
| `distributeColumns()` | Distributes all column widths equally across the table width |
| `clearCells()`        | Clears the content of the current cell or all selected cells |

### Column / row movement

| Method              | Description                                       |
|---------------------|---------------------------------------------------|
| `moveColumnLeft()`  | Moves the cursor column one position to the left  |
| `moveColumnRight()` | Moves the cursor column one position to the right |
| `moveRowUp()`       | Moves the cursor row one position up              |
| `moveRowDown()`     | Moves the cursor row one position down            |

### Column sort

| Method                   | Description                                              |
|--------------------------|----------------------------------------------------------|
| `sortColumnAscending()`  | Sorts data rows by the cursor column in ascending order  |
| `sortColumnDescending()` | Sorts data rows by the cursor column in descending order |

### Lifecycle

| Method      | Description                                                                               |
|-------------|-------------------------------------------------------------------------------------------|
| `destroy()` | Destroys the editor and removes it from the DOM. Call when the host element is unmounted. |

---

## Development Page

`dev/index.html` is structured to match the wiki's edit page layout:

```
┌────────────────────────────────────────────────────────────────────────┐
│ breadcrumb                                                             │
├───────────────────────────────────────────────────────┬────────────────┤
│ [ Document title input       ]                        │                │
│ ───────────────────────────────────────────────────── │  Dev panel     │
│ H1 … H6 │ B I S U ‹› │ x² x₂ │ • 1 ☑ │ ← ↔ → │ " {} ⊞ │                │
│ ───────────────────────────────────────────────────── │  Live JSON     │
│                                                       │  output        │
│  editor area                                          │                │
│                                                       │                │
│ ───────────────────────────────────────────────────── │                │
│ [ Save ]  [ Cancel ]                                  │                │
└───────────────────────────────────────────────────────┴────────────────┘
```

The dev panel provides:

- **Live output** — JSON updated on every keystroke
- **Snapshot** — captures current JSON on demand
- **Make Read-only** — toggles editable state
- **Reload Sample** — restores the built-in sample document

The sample document covers all supported content types: headings, paragraphs, task list, code block, table, and
blockquote.
