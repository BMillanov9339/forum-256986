ALTER TABLE posts RENAME TO topics;
ALTER TABLE replies RENAME COLUMN post_id TO topic_id;

ALTER INDEX idx_posts_created_at_id RENAME TO idx_topics_created_at_id;
ALTER INDEX idx_posts_author_id RENAME TO idx_topics_author_id;
ALTER INDEX idx_replies_post_id RENAME TO idx_replies_topic_id;
ALTER INDEX idx_replies_post_created_id RENAME TO idx_replies_topic_created_id;

ALTER TABLE topics
    ADD COLUMN updated_at TIMESTAMPTZ,
    ADD COLUMN view_count BIGINT NOT NULL DEFAULT 0;

UPDATE topics SET updated_at = created_at WHERE updated_at IS NULL;

ALTER TABLE topics
    ALTER COLUMN updated_at SET NOT NULL,
    ADD CONSTRAINT chk_topics_view_count_nonnegative CHECK (view_count >= 0);

CREATE UNIQUE INDEX uq_topics_title_ci ON topics (lower(btrim(title)));

ALTER TABLE replies ADD COLUMN updated_at TIMESTAMPTZ;
UPDATE replies SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE replies ALTER COLUMN updated_at SET NOT NULL;
