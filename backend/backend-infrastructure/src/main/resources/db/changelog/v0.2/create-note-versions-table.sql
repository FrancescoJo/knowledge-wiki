--liquibase formatted sql

--changeset Francesco Jo:2026-05-30-002-create-note-versions-table
CREATE TABLE note_versions
(
    note_id     UUID        NOT NULL,
    version     INT         NOT NULL,
    content     TEXT        NOT NULL,
    is_snapshot BOOLEAN     NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_note_versions PRIMARY KEY (note_id, version),
    CONSTRAINT fk_note_versions_note FOREIGN KEY (note_id) REFERENCES notes (id)
);
