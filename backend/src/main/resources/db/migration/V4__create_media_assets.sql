CREATE TABLE IF NOT EXISTS media_assets (
    id BIGINT NOT NULL AUTO_INCREMENT,
    file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    size BIGINT NOT NULL,
    uploaded_by VARCHAR(255) NOT NULL,
    created_at DATETIME(6),
    PRIMARY KEY (id)
);

CREATE INDEX idx_media_assets_created_at ON media_assets (created_at);
CREATE INDEX idx_media_assets_uploaded_by ON media_assets (uploaded_by);
