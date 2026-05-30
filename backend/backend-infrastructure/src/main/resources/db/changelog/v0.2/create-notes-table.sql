--liquibase formatted sql

--changeset Francesco Jo:2026-05-30-001-create-notes-table
CREATE TABLE notes
(
    id              UUID         NOT NULL,
    language        VARCHAR(10)  NOT NULL,
    title           VARCHAR(500) NOT NULL,
    title_index     VARCHAR(10)  NOT NULL,
    access_level    VARCHAR(20)  NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    current_version INT          NOT NULL DEFAULT 0,
    author_id       UUID         NOT NULL,
    soft_deleted_by UUID,
    soft_deleted_at TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_notes PRIMARY KEY (id),
    CONSTRAINT uq_notes_title UNIQUE (title),
    CONSTRAINT fk_notes_author FOREIGN KEY (author_id) REFERENCES users (id),
    CONSTRAINT fk_notes_soft_deleted_by FOREIGN KEY (soft_deleted_by) REFERENCES users (id)
);

CREATE INDEX idx_notes_language_title_index ON notes (language, title_index);
