-- Links to the author's work published in newspapers and magazines.
-- These point at someone else's site, so they are deliberately kept out of the
-- news table: they never get their own article page, sitemap entry or RSS item.
CREATE TABLE IF NOT EXISTS press_clippings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(300) NOT NULL,
    publication VARCHAR(160) NOT NULL,
    url VARCHAR(1000) NOT NULL,
    summary TEXT,
    image_url VARCHAR(1000),
    published_on DATE,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    INDEX idx_press_clippings_status_date (status, published_on)
);
