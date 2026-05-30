# OmniMemo Frontend Coding Guide

OmniMemo project–specific rules for the TypeScript frontend.
Read `00-coding_guide.md` and `01-coding_guide_frontend.md` first; the rules there take precedence where they overlap.

**Utility & helper index** — consult this before writing any new helper code, and keep it up to date whenever a utility is added, changed, or removed:

- [`docs/02-coding_guide_utilities_and_helpers_frontend.md`](02-coding_guide_utilities_and_helpers_frontend.md)


## Server Response Wrapping

All REST API responses from the OmniMemo backend are wrapped in a `ResponseEnvelope`:

```json
{
  "type": "OK" | "ERR",
  "body": <payload or error detail>,
  "timestamp": "2026-05-30T12:00:00Z"
}
```

Rules for client-side code that consumes these responses:

- **Always unwrap `body` before processing.** Never pass the envelope object to business logic or UI rendering — extract `body` first.
- **Check `type` before assuming success.** An HTTP 2xx status alone is insufficient; `type` must be `"OK"` to treat the response as successful. An HTTP 2xx with `type: "ERR"` is still an error.
- **Do not access nested data through the envelope.** Extract `body` at the boundary (e.g., inside the fetch wrapper or API client function) so that callers receive plain payload types, not envelopes.

```typescript
// Correct — unwrap at the boundary
async function fetchUser(id: string): Promise<UserResponse> {
  const envelope = await api.get<UserResponse>(`/v1/users/${id}`)
  if (envelope.type !== 'OK' || envelope.body == null) {
    throw new ApiError(envelope)
  }
  return envelope.body
}

// Incorrect — leaking the envelope to the caller
async function fetchUser(id: string): Promise<ResponseEnvelope<UserResponse>> {
  return api.get(`/v1/users/${id}`)
}
```


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
