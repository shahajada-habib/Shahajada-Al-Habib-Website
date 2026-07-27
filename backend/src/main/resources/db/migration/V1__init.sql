CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username)
);

CREATE TABLE IF NOT EXISTS categories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_categories_slug (slug)
);

CREATE TABLE IF NOT EXISTS news (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    subtitle VARCHAR(255),
    content TEXT,
    image_url VARCHAR(255),
    status VARCHAR(255) NOT NULL,
    category VARCHAR(255),
    category_id BIGINT,
    reporter_name VARCHAR(255),
    source VARCHAR(255),
    tags TEXT,
    seo_title VARCHAR(255),
    seo_description TEXT,
    slug VARCHAR(255) NOT NULL,
    is_breaking BIT NOT NULL DEFAULT 0,
    is_featured BIT NOT NULL DEFAULT 0,
    scheduled_at VARCHAR(255),
    publish_date VARCHAR(255),
    view_count BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_news_slug (slug)
);

DELIMITER //

CREATE PROCEDURE add_column_if_missing(
    IN target_table VARCHAR(64),
    IN target_column VARCHAR(64),
    IN column_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = target_table
          AND column_name = target_column
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `', target_table, '` ADD COLUMN `', target_column, '` ', column_definition);
        PREPARE statement FROM @sql;
        EXECUTE statement;
        DEALLOCATE PREPARE statement;
    END IF;
END//

CREATE PROCEDURE add_index_if_missing(
    IN target_table VARCHAR(64),
    IN target_index VARCHAR(64),
    IN index_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = target_table
          AND index_name = target_index
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `', target_table, '` ADD ', index_definition);
        PREPARE statement FROM @sql;
        EXECUTE statement;
        DEALLOCATE PREPARE statement;
    END IF;
END//

CREATE PROCEDURE add_fk_if_missing(
    IN target_table VARCHAR(64),
    IN target_constraint VARCHAR(64),
    IN fk_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE table_schema = DATABASE()
          AND table_name = target_table
          AND constraint_name = target_constraint
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `', target_table, '` ADD ', fk_definition);
        PREPARE statement FROM @sql;
        EXECUTE statement;
        DEALLOCATE PREPARE statement;
    END IF;
END//

DELIMITER ;

CALL add_column_if_missing('news', 'category', 'VARCHAR(255) NULL');
CALL add_column_if_missing('news', 'category_id', 'BIGINT NULL');

UPDATE news n
JOIN categories c
  ON LOWER(c.slug) = LOWER(TRIM(n.category))
  OR LOWER(c.name) = LOWER(TRIM(n.category))
SET n.category_id = c.id
WHERE n.category_id IS NULL
  AND n.category IS NOT NULL
  AND TRIM(n.category) <> '';

CALL add_index_if_missing('news', 'uk_news_slug', 'UNIQUE INDEX uk_news_slug (slug)');
CALL add_index_if_missing('news', 'idx_news_status', 'INDEX idx_news_status (status)');
CALL add_index_if_missing('news', 'idx_news_category_id', 'INDEX idx_news_category_id (category_id)');

CALL add_fk_if_missing(
    'news',
    'fk_news_category',
    'CONSTRAINT fk_news_category FOREIGN KEY (category_id) REFERENCES categories(id)'
);

DROP PROCEDURE add_fk_if_missing;
DROP PROCEDURE add_index_if_missing;
DROP PROCEDURE add_column_if_missing;
