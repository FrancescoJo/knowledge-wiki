--liquibase formatted sql

--changeset Francesco Jo:2026-05-25-001-create-refresh-tokens-table
CREATE TABLE refresh_tokens
(
    token      VARCHAR(36) NOT NULL,
    user_id    UUID        NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (token),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
