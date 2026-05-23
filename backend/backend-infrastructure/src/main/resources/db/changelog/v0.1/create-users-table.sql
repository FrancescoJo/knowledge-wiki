--liquibase formatted sql

--changeset Francesco Jo:2026-05-23-001-create-users-table
CREATE TABLE users (
    id              UUID         NOT NULL,
    email_encrypted BYTEA        NOT NULL,
    email_hmac      BYTEA        NOT NULL,
    password_hash   VARCHAR(72)  NOT NULL,
    iv              BYTEA        NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email_hmac UNIQUE (email_hmac)
);
