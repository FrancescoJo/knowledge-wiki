--liquibase formatted sql

--changeset Francesco Jo:2026-05-30-003-create-note-audits-table
CREATE TABLE note_audits
(
    id         UUID        NOT NULL,
    note_id    UUID        NOT NULL,
    version    INT         NOT NULL,
    action     VARCHAR(20) NOT NULL,
    actor_id   UUID        NOT NULL,
    remote_ip  INET        NOT NULL,
    summary    TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_note_audits PRIMARY KEY (id),
    CONSTRAINT fk_note_audits_note FOREIGN KEY (note_id) REFERENCES notes (id),
    CONSTRAINT fk_note_audits_actor FOREIGN KEY (actor_id) REFERENCES users (id)
);

CREATE INDEX idx_note_audits_note_id ON note_audits (note_id);
