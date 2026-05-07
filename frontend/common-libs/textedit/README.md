# textedit

Rich text editor module for Knowledge Wiki, built on [TipTap core](https://tiptap.dev).

- No UI framework dependency (no React, no Vue)
- Content serialised as JSON
- Bundled as a single ES module (`dist/textedit.js`)

---

## File Structure

```
src/
  types.ts        — TextEditContent, HeadingLevel, TextEditOptions, TextEditHandle
  extensions.ts   — TipTap extension set (StarterKit, Table, TaskList, Link, Placeholder)
  TextEdit.ts     — Public API class
  index.ts        — Module entry point
dev/
  index.html      — Development page (mirrors the wiki edit page structure)
  main.ts         — Development bootstrap (toolbar and dev panel wiring)
```

---

## Development

```bash
npm install
npm run dev     # serves dev/index.html at http://localhost:5173
npm run build   # outputs dist/textedit.js
npm run lint    # tsc --noEmit
```

`vite.config.ts` switches behaviour by command:

| Command | Root | Output |
|---|---|---|
| `vite` (dev) | `dev/` | served in browser with HMR |
| `vite build` | `.` | `dist/textedit.js` (ES module bundle) |

---

## Usage

```typescript
import { TextEdit } from './textedit.js'
import type { TextEditContent } from './textedit.js'

const editor = new TextEdit({
  element: document.getElementById('editor')!,
  content: savedJson,
  onChange: (content: TextEditContent) => {
    // e.g. update a hidden form input for HTMX submission
  },
  onSelectionChange: (handle) => {
    // e.g. update toolbar button active states
    boldBtn.classList.toggle('is-active', handle.isActive('bold'))
  },
})
```

### API

#### Content

| Method | Description |
|---|---|
| `getContent()` | Returns current document as JSON |
| `setContent(content)` | Replaces entire document |

#### State

| Method | Description |
|---|---|
| `setReadOnly(boolean)` | Toggles editable / read-only mode |
| `isActive(name, attrs?)` | Returns true when the named mark or node is active at the current selection |
| `isFocused()` | Returns true when the editor has keyboard focus |

#### Focus

| Method | Description |
|---|---|
| `focus()` | Moves keyboard focus into the editor |

#### Text formatting

| Method | Description |
|---|---|
| `toggleBold()` | |
| `toggleItalic()` | |
| `toggleStrike()` | |
| `toggleCode()` | Inline code |

#### Block formatting

| Method | Description |
|---|---|
| `setHeading(level)` | `level` is `1`–`6` |
| `setParagraph()` | |
| `toggleBulletList()` | |
| `toggleOrderedList()` | |
| `toggleTaskList()` | |
| `toggleBlockquote()` | |
| `toggleCodeBlock()` | |

#### Table

| Method | Description |
|---|---|
| `insertTable()` | Inserts a 3×3 table with a header row at the cursor position |

#### Lifecycle

| Method | Description |
|---|---|
| `destroy()` | Destroys the editor and removes it from the DOM. Call when the host element is unmounted. |

---

## Development Page

`dev/index.html` is structured to match the wiki's edit page layout:

```
┌─────────────────────────────────────────────────┐
│ breadcrumb                                       │
├────────────────────────────────┬────────────────┤
│ [ Document title input       ] │                │
│ ─────────────────────────────  │  Dev panel     │
│ B  I  S  ‹›  H1 H2 H3 ¶  …   │                │
│ ─────────────────────────────  │  Live JSON     │
│                                │  output        │
│  editor area                   │                │
│                                │                │
│ ─────────────────────────────  │                │
│ [ Save ]  [ Cancel ]           │                │
└────────────────────────────────┴────────────────┘
```

The dev panel provides:

- **Live output** — JSON updated on every keystroke
- **Snapshot** — captures current JSON on demand
- **Make Read-only** — toggles editable state
- **Reload Sample** — restores the built-in sample document

The sample document covers all supported content types: headings, paragraphs, task list, code block, table, and blockquote.
