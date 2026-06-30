ALTER TABLE topics
    ADD COLUMN deleted_at TIMESTAMPTZ,
    ADD COLUMN deleted_by BIGINT REFERENCES users (id) ON DELETE SET NULL;

ALTER TABLE replies
    ADD COLUMN deleted_at TIMESTAMPTZ,
    ADD COLUMN deleted_by BIGINT REFERENCES users (id) ON DELETE SET NULL;

ALTER TABLE users ADD COLUMN anonymized_at TIMESTAMPTZ;

CREATE TABLE moderation_actions (
    id BIGSERIAL PRIMARY KEY,
    actor_id BIGINT REFERENCES users (id) ON DELETE SET NULL,
    target_type VARCHAR(32) NOT NULL,
    target_id BIGINT NOT NULL,
    action VARCHAR(32) NOT NULL,
    reason_code VARCHAR(64) NOT NULL,
    note VARCHAR(1000),
    user_notified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_moderation_target_type CHECK (target_type IN ('TOPIC', 'REPLY', 'USER')),
    CONSTRAINT chk_moderation_action CHECK (action IN ('DELETE', 'PURGE', 'ANONYMIZE', 'ROLE_CHANGE'))
);

CREATE INDEX idx_moderation_actions_created_at ON moderation_actions (created_at DESC, id DESC);
CREATE INDEX idx_topics_deleted_at ON topics (deleted_at);
CREATE INDEX idx_replies_deleted_at ON replies (deleted_at);

DROP INDEX uq_topics_title_ci;
CREATE UNIQUE INDEX uq_topics_title_ci ON topics (lower(btrim(title))) WHERE deleted_at IS NULL;
