CREATE TABLE tags (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL UNIQUE,
  slug VARCHAR(120) NOT NULL UNIQUE,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE TABLE news_tags (
  news_id BIGINT NOT NULL,
  tag_id BIGINT NOT NULL,
  PRIMARY KEY (news_id, tag_id),
  CONSTRAINT fk_newstags_news FOREIGN KEY (news_id) REFERENCES news(id) ON DELETE CASCADE,
  CONSTRAINT fk_newstags_tag FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
);

INSERT INTO tags (name, slug)
WITH RECURSIVE split_tags AS (
  SELECT
    id AS news_id,
    TRIM(SUBSTRING_INDEX(tags, ',', 1)) AS tag_name,
    CASE
      WHEN tags LIKE '%,%' THEN SUBSTRING(tags, LOCATE(',', tags) + 1)
      ELSE ''
    END AS rest
  FROM news
  WHERE tags IS NOT NULL AND TRIM(tags) <> ''

  UNION ALL

  SELECT
    news_id,
    TRIM(SUBSTRING_INDEX(rest, ',', 1)) AS tag_name,
    CASE
      WHEN rest LIKE '%,%' THEN SUBSTRING(rest, LOCATE(',', rest) + 1)
      ELSE ''
    END AS rest
  FROM split_tags
  WHERE rest <> ''
)
SELECT DISTINCT
  tag_name,
  COALESCE(
    NULLIF(REGEXP_REPLACE(LOWER(REPLACE(tag_name, ' ', '-')), '[^a-z0-9-]', ''), ''),
    CONCAT('tag-', LEFT(MD5(tag_name), 12))
  ) AS slug
FROM split_tags
WHERE tag_name <> ''
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT IGNORE INTO news_tags (news_id, tag_id)
WITH RECURSIVE split_tags AS (
  SELECT
    id AS news_id,
    TRIM(SUBSTRING_INDEX(tags, ',', 1)) AS tag_name,
    CASE
      WHEN tags LIKE '%,%' THEN SUBSTRING(tags, LOCATE(',', tags) + 1)
      ELSE ''
    END AS rest
  FROM news
  WHERE tags IS NOT NULL AND TRIM(tags) <> ''

  UNION ALL

  SELECT
    news_id,
    TRIM(SUBSTRING_INDEX(rest, ',', 1)) AS tag_name,
    CASE
      WHEN rest LIKE '%,%' THEN SUBSTRING(rest, LOCATE(',', rest) + 1)
      ELSE ''
    END AS rest
  FROM split_tags
  WHERE rest <> ''
)
SELECT split_tags.news_id, tags.id
FROM split_tags
JOIN tags ON tags.name = split_tags.tag_name
WHERE split_tags.tag_name <> '';
