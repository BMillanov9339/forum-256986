ALTER TABLE topics
    ADD CONSTRAINT chk_topics_title_length
        CHECK (char_length(title) <= 128) NOT VALID,
    ADD CONSTRAINT chk_topics_content_length
        CHECK (char_length(content) <= 2000) NOT VALID;

ALTER TABLE replies
    ADD CONSTRAINT chk_replies_content_length
        CHECK (char_length(content) <= 2000) NOT VALID;
