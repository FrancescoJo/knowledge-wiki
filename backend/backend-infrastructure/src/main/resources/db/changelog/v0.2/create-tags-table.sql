--liquibase formatted sql

--changeset Francesco Jo:2026-05-30-004-create-tags-table
CREATE TABLE tags
(
    id         UUID         NOT NULL,
    name       VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_tags PRIMARY KEY (id),
    CONSTRAINT uq_tags_name UNIQUE (name)
);
