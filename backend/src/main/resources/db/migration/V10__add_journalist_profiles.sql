ALTER TABLE users
  ADD COLUMN display_name VARCHAR(150) NULL
    AFTER username,
  ADD COLUMN designation VARCHAR(100) NULL
    AFTER display_name,
  ADD COLUMN bio TEXT NULL
    AFTER designation,
  ADD COLUMN profile_image_url VARCHAR(500) NULL
    AFTER bio,
  ADD COLUMN facebook_url VARCHAR(300) NULL
    AFTER profile_image_url,
  ADD COLUMN twitter_url VARCHAR(300) NULL
    AFTER facebook_url,
  ADD COLUMN email_public VARCHAR(200) NULL
    AFTER twitter_url,
  ADD COLUMN is_public BOOLEAN NOT NULL DEFAULT TRUE
    AFTER email_public;

ALTER TABLE news
  ADD COLUMN author_id BIGINT NULL AFTER reporter_name,
  ADD CONSTRAINT fk_news_author
    FOREIGN KEY (author_id)
    REFERENCES users(id) ON DELETE SET NULL;

UPDATE news n
  JOIN users u ON LOWER(u.username) = LOWER(n.reporter_name)
  SET n.author_id = u.id
  WHERE n.author_id IS NULL
    AND n.reporter_name IS NOT NULL
    AND n.reporter_name != '';
