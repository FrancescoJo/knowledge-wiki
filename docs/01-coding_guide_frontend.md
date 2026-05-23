# Frontend Coding Guide

A supplement to `00-coding_guide.md` covering TypeScript frontend–specific patterns.
Read `00-coding_guide.md` first; the rules there take precedence where they overlap.


## TypeScript Patterns

### `as const` over `enum`

Use `as const` objects instead of TypeScript `enum`. Enums generate a double-indirection runtime object and produce opaque numeric values by default; `as const` objects are plain records whose values appear directly in compiled output and are tree-shakeable.

```typescript
// Preferred
export const NodeType = {
  Doc:       'doc',
  Paragraph: 'paragraph',
  Heading:   'heading',
} as const

export type NodeType = typeof NodeType[keyof typeof NodeType]

// Avoid
enum NodeType { Doc = 'doc', Paragraph = 'paragraph' }
```

### `interface` vs `type` Alias

| Use | When |
|---|---|
| `interface` | Object shapes: can be extended, supports method overloading |
| `type` alias | Union types, opaque wrappers, primitive-ish values |

```typescript
// interface — public contract with method overloads
export interface TextEditHandle {
  isActive(name: string, attributes?: Record<string, unknown>): boolean
  isActive(attributes: Record<string, unknown>): boolean
  isDirty(): boolean
}

// type alias — opaque wrapper over a primitive shape
export type TextEditContent = Record<string, unknown>

// type alias — constrained union
export type HeadingLevel = 1 | 2 | 3 | 4 | 5 | 6
```

### `readonly` by Default

Declare all class fields `readonly` unless mutation is explicitly required. This applies especially to injected dependencies and event handler references stored for later removal.

```typescript
class LinkTooltipView {
  readonly dom: HTMLElement
  private readonly editorView: EditorView
  private readonly onMousemove: (ev: MouseEvent) => void  // stored for removeEventListener

  constructor(editorView: EditorView) {
    this.editorView = editorView
    this.onMousemove = (ev) => this.handleMousemove(ev)
    editorView.dom.addEventListener('mousemove', this.onMousemove)
  }
}
```


## Class Design

### Member Ordering

Group members by role, separate each group with a section comment `// -- Group Name`. Within a group, place public members before private.

```typescript
export class TextEdit implements TextEditHandle {
  // -- State
  private readonly editor: Editor
  private currentHash: string

  // -- Constructor
  constructor(options: TextEditOptions) { ... }

  // -- Content
  getContent(): TextEditContent { ... }
  setContent(content: TextEditContent): void { ... }

  // -- Formatting
  toggleBold(): void { ... }
  toggleItalic(): void { ... }

  // -- Lifecycle
  destroy(): void { ... }
}
```

### Lifecycle: Mandatory Cleanup

Any class that appends DOM nodes or registers event listeners must implement `destroy()`. Listeners must be removed by reference — store handler functions as `readonly` fields in the constructor.

```typescript
destroy(): void {
  this.editorView.dom.removeEventListener('mousemove', this.onMousemove)
  this.dom.remove()
}
```

Failing to remove listeners by reference (e.g., using inline lambdas) causes memory leaks.


## Naming

### CSS Classes

Use kebab-case with a short module prefix. The prefix prevents class name collisions when the module is embedded in a host application.

```typescript
// textedit module — prefix 'te-'
this.dom.className = 'te-link-tooltip'
editorView.dom.classList.add('te-ctrl-held')
```

Define class name strings as constants at file scope, not inline.

```typescript
const CSS_TOOLTIP = 'te-link-tooltip'
const CSS_ACTIVE  = 'te-link-tooltip--active'
```

### Constants

All magic literals — numbers, strings, limits — must be named constants in UPPER_SNAKE_CASE declared at file or module scope.

```typescript
const HREF_MAX_CHARS   = 62
const DJB2_SEED        = 5381
const TABLE_DEFAULT_ROWS = 3
```


## Module Public API

Each module exposes its public API through a single barrel file (`src/index.ts`). Keep this file strictly as re-exports — no logic.

Separate runtime exports from type-only exports so consumers using `isolatedModules` or `verbatimModuleSyntax` get correct output:

```typescript
// Runtime values
export { TextEdit } from './TextEdit'
export { NodeType, MarkType } from './types'
export { BLOCK_OBJECT_TYPES } from './extensions'

// Types only — erased at compile time
export type { TextEditContent, TextEditHandle, TextEditOptions } from './types'
export type { BlockObjectType } from './extensions'
```

Do not export implementation details. Helper functions, internal constants, and overlay classes stay unexported.


## TipTap / ProseMirror Patterns

### Extension Pattern

Extend built-in TipTap extensions using `.extend()`. Always call `this.parent?.()` to chain the parent's behaviour; omitting it silently discards the parent's plugins or attributes.

```typescript
export const CustomTable = Table.extend({
  addAttributes() {
    return {
      ...this.parent?.(),       // preserve existing attributes
      fixedColumnWidths: {
        default: false,
        parseHTML: el => el.getAttribute('data-fixed-column-widths') === 'true',
        renderHTML: attrs => attrs.fixedColumnWidths
          ? { 'data-fixed-column-widths': 'true' }
          : {},
      },
    }
  },
})
```

### Plugin View Lifecycle

ProseMirror plugin views must implement `update()` and `destroy()`. Floating UI elements (tooltips, overlays) attach to `document.body`, not to the editor container — this avoids clipping by `overflow: hidden` ancestors.

```typescript
class MyOverlayView {
  readonly dom: HTMLElement

  constructor(private readonly editorView: EditorView) {
    this.dom = document.createElement('div')
    document.body.appendChild(this.dom)   // attach to body, not editor
  }

  update(): void { /* react to editor state changes */ }

  destroy(): void {
    this.dom.remove()
  }
}
```


## Test Patterns

All frontend tests are **Small** (see `00-coding_guide.md` for the full definition): no network, no filesystem, no database. Run under jsdom via Vitest.

### Fixture Design

Define reusable document fixtures as named constants in `test-utils.ts`, not inline in individual test files. Express the fixture in terms of the public type vocabulary (`NodeType`, `MarkType`), not raw strings.

```typescript
export const EMPTY_DOC: TextEditContent = {
  type: NodeType.Doc,
  content: [{ type: NodeType.Paragraph }],
}
```

### Cleanup Pattern

Destroy the editor in `afterEach`. Use try/catch because a test that fails mid-setup may leave the editor in a state where `destroy()` itself throws.

```typescript
let editor: TextEdit | undefined

afterEach(() => {
  try { editor?.destroy() } catch { /* partially initialised */ }
  editor = undefined
  element.remove()
})
```

### Accessing Internals

Use `(obj as any).field` only when testing behaviour that cannot be observed through the public API — for example, verifying that a ProseMirror view reference was correctly stored. Keep these casts confined to `test-utils.ts` helpers; do not scatter them across test files.

```typescript
// test-utils.ts — centralised internal access
export function pmView(editor: TextEdit) {
  return (editor as any).editor.view as EditorView
}
```


## Library Build Optimisation

These rules apply to any frontend module published as a library (i.e. has a `build.lib` entry in its Vite config).

- **Declare peer dependencies correctly.** Packages the consuming application is expected to supply — UI frameworks, editor cores, syntax-highlighting engines — must be listed in `peerDependencies`, not `dependencies`. Also add them to `devDependencies` so the library's own dev environment installs them.
- **Externalise peer dependencies in the bundler.** List every peer package in `build.rollupOptions.external`, including subpath imports such as `highlight.js/lib/languages/*`. Bundling a peer dependency duplicates code in the consumer's final build and inflates the library's dist file.
- **Enable minification explicitly.** Set `build.minify: 'esbuild'` in `vite.config.ts`. Do not rely on the bundler default, which may change across Vite versions.
- **Verify output size after every build change.** Record the gzip size reported by `vite build` and confirm it has not regressed before committing.
