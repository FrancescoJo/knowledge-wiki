--liquibase formatted sql

--changeset Francesco Jo:2026-05-30-007-create-note-title-history-table
CREATE TABLE note_title_history
(
    old_title  VARCHAR(500) NOT NULL,
    note_id    UUID         NOT NULL,
    changed_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_note_title_history PRIMARY KEY (old_title),
    CONSTRAINT fk_note_title_history_note FOREIGN KEY (note_id) REFERENCES notes (id)
);
