CREATE TABLE topic_views (
    topic_id BIGINT NOT NULL REFERENCES topics (id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    viewed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (topic_id, user_id)
);

ALTER TABLE moderation_actions
    DROP CONSTRAINT chk_moderation_target_type,
    DROP CONSTRAINT chk_moderation_action,
    ADD CONSTRAINT chk_moderation_target_type
        CHECK (target_type IN ('TOPIC', 'REPLY', 'USER', 'SYSTEM')),
    ADD CONSTRAINT chk_moderation_action
        CHECK (action IN ('DELETE', 'PURGE', 'ANONYMIZE', 'ROLE_CHANGE', 'RESTORE_ENABLE', 'RESTORE_DISABLE'));
