# Knowledge Wiki — Product Roadmap

## v0.1 — MVP (Personal Use)

### Login / Register

- Anonymous users can read documents
- Logged-in users can edit documents
- Permission/grants model is planned but deferred (personal use only at this stage)
- User creation is done via CLI script to avoid exposing a registration endpoint

### Version Management

Preserves full document history and a snapshot of the latest version for fast access.

### Document Hierarchy

Tree-like structure for organizing documents.

### Document Editor

- Powered by the `textedit` module (see Tech Stack)
- `textedit` is developed as a separate project under `frontend/common-libs/textedit`

### Document Search

- Title: similarity-based search

---

## v0.2 — Content Management

### LLM Summarization

- Summarize document content via LLM
- LLM API key management system

> **Note:** The LLM API infrastructure built here (API key management, call abstraction) will be reused in v0.3 for embedding generation.

---

## v0.3 — Enhanced Search

### Vector DB Search

- Document body: context-aware search using vector embeddings (PostgreSQL + pgvector)

---

## v0.5 — Multilingual Support

---

## v1.0 — Administration

- Admin interface
- Permission / Grants model
