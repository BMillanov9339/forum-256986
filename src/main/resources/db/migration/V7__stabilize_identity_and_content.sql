UPDATE users
SET password_hash = '!disabled-legacy-account!'
WHERE password_hash IS NULL;

UPDATE users
SET username = lower(btrim(username)),
    email = CASE WHEN email IS NULL OR btrim(email) = '' THEN NULL ELSE lower(btrim(email)) END;

UPDATE users
SET password_hash = '!bootstrap-secret-required!'
WHERE username = 'admin'
  AND password_hash = '$2a$10$k/5TFC9kQu4R4QAtp0GXkeFQQ7G1Xjk2ZrXKHFGzsOy7GLsiZQGVm';

INSERT INTO users (username, email, role, created_at, password_hash)
SELECT '__bootstrap_admin_v7__', NULL, 'ADMIN', NOW(), '!bootstrap-secret-required!'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE role = 'ADMIN');

ALTER TABLE users
    ADD COLUMN auth_version INTEGER NOT NULL DEFAULT 0,
    ALTER COLUMN password_hash SET NOT NULL,
    ADD CONSTRAINT chk_users_role CHECK (role IN ('ADMIN', 'MODERATOR', 'USER')),
    ADD CONSTRAINT chk_users_username_not_blank CHECK (btrim(username) <> ''),
    ADD CONSTRAINT chk_users_email_not_blank CHECK (email IS NULL OR btrim(email) <> '');

CREATE UNIQUE INDEX uq_users_username_ci ON users (lower(username));
CREATE UNIQUE INDEX uq_users_email_ci ON users (lower(email)) WHERE email IS NOT NULL;

ALTER TABLE posts
    ADD COLUMN author_id BIGINT REFERENCES users (id) ON DELETE RESTRICT;

UPDATE posts
SET author_id = (SELECT id FROM users WHERE role = 'ADMIN' ORDER BY id LIMIT 1)
WHERE author_id IS NULL;

ALTER TABLE posts
    ALTER COLUMN author_id SET NOT NULL,
    ADD CONSTRAINT chk_posts_title_not_blank CHECK (btrim(title) <> ''),
    ADD CONSTRAINT chk_posts_content_not_blank CHECK (btrim(content) <> '');

CREATE INDEX idx_posts_created_at_id ON posts (created_at DESC, id DESC);
CREATE INDEX idx_posts_author_id ON posts (author_id);

ALTER TABLE replies
    ADD COLUMN author_id BIGINT REFERENCES users (id) ON DELETE RESTRICT;

UPDATE replies
SET author_id = (SELECT id FROM users WHERE role = 'ADMIN' ORDER BY id LIMIT 1)
WHERE author_id IS NULL;

ALTER TABLE replies
    ALTER COLUMN author_id SET NOT NULL,
    ADD CONSTRAINT chk_replies_content_not_blank CHECK (btrim(content) <> '');

CREATE INDEX idx_replies_post_created_id ON replies (post_id, created_at ASC, id ASC);
CREATE INDEX idx_replies_author_id ON replies (author_id);
