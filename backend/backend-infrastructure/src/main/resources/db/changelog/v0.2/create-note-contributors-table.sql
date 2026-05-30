--liquibase formatted sql

--changeset Francesco Jo:2026-05-30-006-create-note-contributors-table
CREATE TABLE note_contributors
(
    note_id UUID NOT NULL,
    user_id UUID NOT NULL,
    CONSTRAINT pk_note_contributors PRIMARY KEY (note_id, user_id),
    CONSTRAINT fk_note_contributors_note FOREIGN KEY (note_id) REFERENCES notes (id) ON DELETE CASCADE,
    CONSTRAINT fk_note_contributors_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
