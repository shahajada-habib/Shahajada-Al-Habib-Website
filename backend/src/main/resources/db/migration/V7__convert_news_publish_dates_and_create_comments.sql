UPDATE news
SET scheduled_at = NULL
WHERE scheduled_at IS NOT NULL
  AND TRIM(scheduled_at) = '';

UPDATE news
SET publish_date = NULL
WHERE publish_date IS NOT NULL
  AND TRIM(publish_date) = '';

UPDATE news
SET scheduled_at = TRIM(REPLACE(scheduled_at, 'T', ' '))
WHERE scheduled_at IS NOT NULL;

UPDATE news
SET publish_date = TRIM(REPLACE(publish_date, 'T', ' '))
WHERE publish_date IS NOT NULL;

-- MySQL DATETIME(6) accepts microseconds only. Legacy Java strings may contain
-- nanoseconds, for example 2026-05-04 23:22:51.783875300. Truncate the
-- fractional component before STR_TO_DATE so existing published rows are kept.
UPDATE news
SET scheduled_at = CONCAT(
    SUBSTRING_INDEX(scheduled_at, '.', 1),
    '.',
    LEFT(SUBSTRING_INDEX(scheduled_at, '.', -1), 6)
)
WHERE scheduled_at IS NOT NULL
  AND scheduled_at LIKE '%.%'
  AND CHAR_LENGTH(SUBSTRING_INDEX(scheduled_at, '.', -1)) > 6;

UPDATE news
SET publish_date = CONCAT(
    SUBSTRING_INDEX(publish_date, '.', 1),
    '.',
    LEFT(SUBSTRING_INDEX(publish_date, '.', -1), 6)
)
WHERE publish_date IS NOT NULL
  AND publish_date LIKE '%.%'
  AND CHAR_LENGTH(SUBSTRING_INDEX(publish_date, '.', -1)) > 6;

-- Legacy demo data may contain non-ISO strings. Invalid values become NULL
-- before DATETIME conversion so Flyway can complete conservatively.
UPDATE news
SET scheduled_at = COALESCE(
    STR_TO_DATE(scheduled_at, '%Y-%m-%d %H:%i:%s.%f'),
    STR_TO_DATE(scheduled_at, '%Y-%m-%d %H:%i:%s'),
    STR_TO_DATE(scheduled_at, '%Y-%m-%d %H:%i'),
    STR_TO_DATE(scheduled_at, '%Y-%m-%d')
)
WHERE scheduled_at IS NOT NULL;

UPDATE news
SET publish_date = COALESCE(
    STR_TO_DATE(publish_date, '%Y-%m-%d %H:%i:%s.%f'),
    STR_TO_DATE(publish_date, '%Y-%m-%d %H:%i:%s'),
    STR_TO_DATE(publish_date, '%Y-%m-%d %H:%i'),
    STR_TO_DATE(publish_date, '%Y-%m-%d')
)
WHERE publish_date IS NOT NULL;

ALTER TABLE news
    MODIFY scheduled_at DATETIME(6) NULL,
    MODIFY publish_date DATETIME(6) NULL;

CREATE INDEX idx_news_public_publish
    ON news (status, publish_date, scheduled_at, created_at, id);

CREATE TABLE IF NOT EXISTS comments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    news_id BIGINT NOT NULL,
    author VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'pending',
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    INDEX idx_comments_news_status_created (news_id, status, created_at),
    INDEX idx_comments_status_created (status, created_at),
    CONSTRAINT fk_comments_news FOREIGN KEY (news_id) REFERENCES news(id) ON DELETE CASCADE
);
